/**
 * @file threading.h
 * @brief Cross-platform threading primitives for ForensicLogEngine.
 *
 * Design Rationale:
 * -----------------
 * The MinGW.org GCC 6.3.0 distribution uses the win32 thread model,
 * which does NOT provide the C++ standard threading headers (<thread>,
 * <mutex>, <condition_variable>). Only MinGW-w64 with the posix thread
 * model provides these.
 *
 * To maintain compatibility, we implement lightweight wrappers around
 * the Windows API threading primitives (CRITICAL_SECTION, CONDITION_VARIABLE,
 * CreateThread) that mirror the std:: interfaces.
 *
 * These wrappers are:
 * - forensic::Mutex       (replaces std::mutex)
 * - forensic::LockGuard   (replaces std::lock_guard)
 * - forensic::UniqueLock  (replaces std::unique_lock)
 * - forensic::CondVar     (replaces std::condition_variable)
 * - forensic::Thread      (replaces std::thread)
 * - forensic::AtomicBool  (replaces std::atomic<bool> with Interlocked ops)
 * - forensic::AtomicSizeT (replaces std::atomic<size_t>)
 */

#ifndef FORENSIC_THREADING_H
#define FORENSIC_THREADING_H

// Force Windows Vista+ API level to enable CONDITION_VARIABLE.
// Old MinGW defaults to 0x0500 (Windows 2000) which lacks it.
#ifdef _WIN32_WINNT
#undef _WIN32_WINNT
#endif
#define _WIN32_WINNT 0x0600

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <process.h>
#include <functional>
#include <cstddef>

namespace forensic {

// ===================================================================
// Mutex — wraps CRITICAL_SECTION
// ===================================================================
// CRITICAL_SECTION is preferred over CreateMutex() because:
// 1. It's lighter weight (user-mode, no kernel transition for uncontended).
// 2. It's re-entrant by default (though we don't rely on that).
// 3. It's faster for intra-process synchronization.
class Mutex {
public:
    Mutex()  { InitializeCriticalSection(&cs_); }
    ~Mutex() { DeleteCriticalSection(&cs_); }

    Mutex(const Mutex&) = delete;
    Mutex& operator=(const Mutex&) = delete;

    void lock()   { EnterCriticalSection(&cs_); }
    void unlock() { LeaveCriticalSection(&cs_); }

    // Expose the underlying CS for condition variable usage.
    CRITICAL_SECTION* native_handle() { return &cs_; }

private:
    CRITICAL_SECTION cs_;
};

// ===================================================================
// LockGuard — RAII lock wrapper (mirrors std::lock_guard)
// ===================================================================
class LockGuard {
public:
    explicit LockGuard(Mutex& m) : mutex_(m) { mutex_.lock(); }
    ~LockGuard() { mutex_.unlock(); }

    LockGuard(const LockGuard&) = delete;
    LockGuard& operator=(const LockGuard&) = delete;

private:
    Mutex& mutex_;
};

// ===================================================================
// UniqueLock — movable RAII lock (mirrors std::unique_lock)
// ===================================================================
// Needed for condition variable wait(), which must unlock/relock.
class UniqueLock {
public:
    explicit UniqueLock(Mutex& m) : mutex_(&m), owns_(true) {
        mutex_->lock();
    }
    ~UniqueLock() {
        if (owns_) mutex_->unlock();
    }

    UniqueLock(const UniqueLock&) = delete;
    UniqueLock& operator=(const UniqueLock&) = delete;

    void lock()   { mutex_->lock(); owns_ = true; }
    void unlock() { mutex_->unlock(); owns_ = false; }

    Mutex* mutex() { return mutex_; }

private:
    Mutex* mutex_;
    bool   owns_;
};

// ===================================================================
// CondVar — wraps Windows CONDITION_VARIABLE
// ===================================================================
// Windows CONDITION_VARIABLE (Vista+) works with CRITICAL_SECTION
// and provides the same semantics as pthread_cond / std::condition_variable.
class CondVar {
public:
    CondVar() { InitializeConditionVariable(&cv_); }
    // Windows CONDITION_VARIABLEs don't need explicit destruction.

    CondVar(const CondVar&) = delete;
    CondVar& operator=(const CondVar&) = delete;

    void notify_one() { WakeConditionVariable(&cv_); }
    void notify_all() { WakeAllConditionVariable(&cv_); }

    // Wait with predicate — handles spurious wakeups.
    // The predicate is re-checked after each wakeup, just like
    // std::condition_variable::wait(lock, predicate).
    template<typename Pred>
    void wait(UniqueLock& lock, Pred predicate) {
        while (!predicate()) {
            // SleepConditionVariableCS atomically:
            // 1. Releases the critical section
            // 2. Puts the thread to sleep
            // 3. Re-acquires the critical section on wakeup
            // This is the same atomic unlock-sleep-relock that
            // std::condition_variable::wait() provides.
            SleepConditionVariableCS(&cv_, lock.mutex()->native_handle(), INFINITE);
        }
    }

private:
    CONDITION_VARIABLE cv_;
};

// ===================================================================
// AtomicBool — lock-free boolean using Interlocked operations
// ===================================================================
class AtomicBool {
public:
    AtomicBool(bool initial = false) : value_(initial ? 1 : 0) {}

    bool load() const {
        // InterlockedCompareExchange reads the value atomically.
        // We compare-and-swap with 0,0 to just read.
        return InterlockedCompareExchange(
            const_cast<volatile LONG*>(&value_), 0, 0) != 0;
    }

    void store(bool val) {
        InterlockedExchange(&value_, val ? 1 : 0);
    }

    // Atomically sets to desired if current value equals expected.
    // Returns true if the exchange was performed.
    bool compare_exchange(bool expected, bool desired) {
        LONG exp = expected ? 1 : 0;
        LONG des = desired ? 1 : 0;
        return InterlockedCompareExchange(&value_, des, exp) == exp;
    }

private:
    volatile LONG value_;
};

// ===================================================================
// AtomicSizeT — lock-free size_t counter using Interlocked
// ===================================================================
class AtomicSizeT {
public:
    AtomicSizeT(long initial = 0) : value_(initial) {}

    long load() const {
        return InterlockedCompareExchange(
            const_cast<volatile LONG*>(&value_), 0, 0);
    }

    void store(long val) {
        InterlockedExchange(&value_, val);
    }

    long fetch_add(long delta) {
        return InterlockedExchangeAdd(&value_, delta);
    }

private:
    volatile LONG value_;
};

// ===================================================================
// Thread — wraps Windows _beginthreadex
// ===================================================================
// We use _beginthreadex instead of CreateThread because _beginthreadex
// properly initializes the C runtime's per-thread data structures.
// CreateThread can cause memory leaks with CRT functions.
class Thread {
public:
    Thread() : handle_(NULL) {}

    // Constructor that accepts any callable — stores it in a heap-
    // allocated wrapper and passes it to _beginthreadex.
    template<typename Func>
    explicit Thread(Func f) : handle_(NULL) {
        // Allocate the callable on the heap so it outlives this constructor.
        // The thread function will delete it after invocation.
        auto* wrapper = new std::function<void()>(std::move(f));
        handle_ = reinterpret_cast<HANDLE>(
            _beginthreadex(
                nullptr,            // default security
                0,                  // default stack size
                thread_entry,       // thread function
                wrapper,            // argument (our callable)
                0,                  // run immediately
                nullptr             // don't need thread id
            )
        );
        if (handle_ == NULL) {
            delete wrapper;
        }
    }

    ~Thread() {
        // If the thread is joinable (not yet joined), this is a
        // programming error. Unlike std::thread which calls terminate(),
        // we just close the handle to avoid crashes.
        if (handle_ != NULL) {
            CloseHandle(handle_);
        }
    }

    // Move semantics — transfer thread ownership.
    Thread(Thread&& other) : handle_(other.handle_) {
        other.handle_ = NULL;
    }

    Thread& operator=(Thread&& other) {
        if (this != &other) {
            if (handle_ != NULL) CloseHandle(handle_);
            handle_ = other.handle_;
            other.handle_ = NULL;
        }
        return *this;
    }

    Thread(const Thread&) = delete;
    Thread& operator=(const Thread&) = delete;

    bool joinable() const { return handle_ != NULL; }

    void join() {
        if (handle_ != NULL) {
            WaitForSingleObject(handle_, INFINITE);
            CloseHandle(handle_);
            handle_ = NULL;
        }
    }

    // Returns the number of logical CPUs (equivalent to
    // std::thread::hardware_concurrency).
    static unsigned int hardware_concurrency() {
        SYSTEM_INFO si;
        GetSystemInfo(&si);
        return si.dwNumberOfProcessors;
    }

private:
    HANDLE handle_;

    // Static thread entry point for _beginthreadex.
    // Signature must be: unsigned __stdcall func(void*).
    static unsigned __stdcall thread_entry(void* arg) {
        auto* func = static_cast<std::function<void()>*>(arg);
        try {
            (*func)();
        } catch (...) {
            // Swallow exceptions — same as std::thread behavior.
        }
        delete func;
        return 0;
    }
};

} // namespace forensic

#endif // FORENSIC_THREADING_H

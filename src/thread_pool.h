/**
 * @file thread_pool.h
 * @brief Thread pool declaration for ForensicLogEngine.
 *
 * Design Rationale:
 * -----------------
 * We use a fixed-size thread pool to avoid the overhead of creating and
 * destroying threads for every log line. In forensic analysis, log files
 * can contain millions of entries, so amortizing thread creation cost is
 * critical. The pool uses a producer-consumer pattern with a shared task
 * queue protected by a mutex and signaled via a condition variable.
 *
 * Threading Model:
 * - Uses custom wrappers around Windows API primitives (CRITICAL_SECTION,
 *   CONDITION_VARIABLE, _beginthreadex) defined in threading.h.
 * - This ensures compatibility with MinGW.org GCC 6.3.0 which does NOT
 *   provide std::thread, std::mutex, or std::condition_variable.
 */

#ifndef FORENSIC_THREAD_POOL_H
#define FORENSIC_THREAD_POOL_H

#include "threading.h"
#include <vector>
#include <queue>
#include <functional>

namespace forensic {

class ThreadPool {
public:
    /**
     * @brief Construct a thread pool with a fixed number of workers.
     * @param num_threads Number of worker threads to spawn. Defaults to
     *        the hardware concurrency hint, falling back to 4 if unavailable.
     *
     * Each worker thread runs an infinite loop, waiting on the condition
     * variable for tasks. This avoids busy-waiting and keeps CPU usage low
     * when the queue is empty.
     */
    explicit ThreadPool(size_t num_threads = 0);

    /**
     * @brief Destructor - ensures clean shutdown of all worker threads.
     *
     * Calls shutdown() if not already stopped. This guarantees that no
     * threads are left dangling.
     */
    ~ThreadPool();

    // Non-copyable, non-movable - threads hold references to internal state.
    ThreadPool(const ThreadPool&) = delete;
    ThreadPool& operator=(const ThreadPool&) = delete;

    /**
     * @brief Submit a task (callable) to the thread pool's queue.
     * @param task A std::function<void()> to be executed by a worker.
     *
     * Thread Safety:
     * - The queue is locked with LockGuard before pushing. This is
     *   sufficient because we only need exclusive access for the push
     *   operation (no waiting involved on the producer side).
     * - After pushing, we notify_one() to wake exactly one sleeping worker.
     *   notify_one() is preferred over notify_all() to avoid thundering herd.
     */
    void submit(std::function<void()> task);

    /**
     * @brief Gracefully shut down the pool. All queued tasks will finish.
     *
     * Sets the stop flag, notifies all workers (so they check the flag),
     * and joins every thread. Safe to call multiple times.
     */
    void shutdown();

    /**
     * @brief Returns the number of worker threads in the pool.
     */
    size_t thread_count() const;

private:
    /**
     * @brief The loop each worker thread executes.
     *
     * Uses UniqueLock (not LockGuard) because the condition variable's
     * wait() needs to temporarily unlock the mutex while sleeping and
     * re-lock it upon wakeup. LockGuard does not support this pattern.
     */
    void worker_loop();

    std::vector<Thread>                 workers_;     ///< Worker thread handles
    std::queue<std::function<void()>>   task_queue_;  ///< Pending tasks
    Mutex                               queue_mutex_; ///< Protects task_queue_
    CondVar                             cv_;          ///< Signals new tasks / stop
    AtomicBool                          stop_;        ///< Shutdown flag
};

} // namespace forensic

#endif // FORENSIC_THREAD_POOL_H

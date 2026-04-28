/**
 * @file thread_pool.cpp
 * @brief Thread pool implementation for ForensicLogEngine.
 */

#include "thread_pool.h"
#include <iostream>

namespace forensic {

ThreadPool::ThreadPool(size_t num_threads) : stop_(false) {
    // If caller passes 0, use the hardware concurrency hint.
    // Thread::hardware_concurrency() may return 0 on some platforms,
    // so we fall back to 4 threads as a reasonable default.
    if (num_threads == 0) {
        num_threads = Thread::hardware_concurrency();
        if (num_threads == 0) {
            num_threads = 4;
        }
    }

    // Spawn worker threads. Each thread immediately enters worker_loop()
    // and blocks on the condition variable until work arrives.
    for (size_t i = 0; i < num_threads; ++i) {
        workers_.push_back(Thread([this]() { this->worker_loop(); }));
    }
}

ThreadPool::~ThreadPool() {
    // Ensure all threads are joined before destruction.
    // If shutdown() was already called, this is a no-op.
    shutdown();
}

void ThreadPool::submit(std::function<void()> task) {
    {
        // Lock the queue only for the duration of the push.
        // LockGuard is used here (not UniqueLock) because we don't
        // need to wait on a condition variable - we're just pushing.
        LockGuard lock(queue_mutex_);

        // Don't accept new tasks after shutdown has been initiated.
        if (stop_.load()) {
            return;
        }

        task_queue_.push(std::move(task));
    }
    // Notify one sleeping worker that a new task is available.
    // Using notify_one() instead of notify_all() prevents the "thundering
    // herd" problem where all workers wake up but only one gets work.
    cv_.notify_one();
}

void ThreadPool::shutdown() {
    // Set the stop flag. If already set, return early.
    if (!stop_.compare_exchange(false, true)) {
        return; // Already shutting down or shut down.
    }

    // Wake ALL workers so they can observe the stop flag and exit.
    // notify_all() is necessary here because every thread needs to
    // check stop_ and terminate - notify_one() could leave threads sleeping.
    cv_.notify_all();

    // Join all worker threads. join() blocks until the thread finishes.
    // This ensures all in-progress tasks complete before shutdown returns.
    for (auto& worker : workers_) {
        if (worker.joinable()) {
            worker.join();
        }
    }
}

size_t ThreadPool::thread_count() const {
    return workers_.size();
}

void ThreadPool::worker_loop() {
    while (true) {
        std::function<void()> task;

        {
            // UniqueLock is REQUIRED here (not LockGuard) because
            // cv_.wait() needs to atomically unlock the mutex while
            // sleeping and re-lock it when woken. LockGuard only
            // supports RAII lock/unlock and cannot be manually unlocked.
            UniqueLock lock(queue_mutex_);

            // Wait until either:
            // 1. A task is available in the queue, OR
            // 2. The stop flag is set (shutdown requested).
            //
            // The lambda predicate guards against spurious wakeups -
            // the condition variable may wake the thread without notify
            // being called. The predicate ensures we only proceed when
            // there's actual work or a shutdown signal.
            cv_.wait(lock, [this]() {
                return stop_.load() || !task_queue_.empty();
            });

            // If stop is requested AND no more tasks remain, exit the loop.
            // We drain remaining tasks even after stop is set, ensuring
            // all submitted work completes (graceful shutdown).
            if (stop_.load() && task_queue_.empty()) {
                return;
            }

            // Move the task out of the queue while still holding the lock.
            task = std::move(task_queue_.front());
            task_queue_.pop();
        }
        // Lock is released here (UniqueLock destructor).

        // Execute the task outside the lock - this is critical for
        // performance. Holding the lock during task execution would
        // serialize all work, defeating the purpose of a thread pool.
        try {
            task();
        } catch (const std::exception& e) {
            // Log but don't propagate - one bad task shouldn't kill a worker.
            std::cerr << "[ThreadPool] Task threw exception: " << e.what() << "\n";
        }
    }
}

} // namespace forensic

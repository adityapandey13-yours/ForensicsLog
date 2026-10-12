/**
 * @file main.cpp
 * @brief Entry point for ForensicLogEngine - a multi-threaded log ingestion
 *        and IOC pattern matching engine for forensic analysis.
 *
 * Architecture Overview:
 * ----------------------
 * 1. Parse the log file line by line (single-threaded file I/O).
 * 2. Dispatch each parsed entry as a task to the thread pool.
 * 3. Each worker thread independently matches the entry against IOC rules.
 * 4. Matches are written to SQLite via a thread-safe DatabaseWriter.
 * 5. After all tasks complete, print a summary to stdout.
 */

#include "thread_pool.h"
#include "log_parser.h"
#include "pattern_matcher.h"
#include "db_writer.h"
#include "threading.h"

#include <iostream>
#include <string>

// Simple timing using Windows API since <chrono> may have issues on old MinGW.
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>

int main(int argc, char* argv[]) {
    // ---------------------------------------------------------------
    // 1. Validate CLI arguments
    // ---------------------------------------------------------------
    if (argc < 2) {
        std::cerr << "Usage: forensic_log_engine <log_file_path>\n";
        std::cerr << "Example: forensic_log_engine.exe logs/sample.log\n";
        return 1;
    }

    const std::string log_file = argv[1];

    std::cout << "============================================\n";
    std::cout << "  ForensicLogEngine v1.0\n";
    std::cout << "  Multi-threaded IOC Detection Engine\n";
    std::cout << "============================================\n\n";

    // ---------------------------------------------------------------
    // 2. Initialize components
    // ---------------------------------------------------------------
    forensic::ThreadPool pool;
    std::cout << "[*] Thread pool initialized with "
              << pool.thread_count() << " worker threads\n";

    // The pattern matcher loads IOC rules at construction.
    // It is immutable after this point - safe for concurrent reads.
    forensic::PatternMatcher matcher;
    std::cout << "[*] Pattern matcher loaded with "
              << matcher.rule_count() << " IOC rules\n";

    // The database writer creates/opens alerts.db in the current directory.
    forensic::DatabaseWriter db_writer("alerts.db");
    std::cout << "[*] Database writer initialized (alerts.db)\n";

    // Atomic counter for alerts - updated from multiple worker threads.
    // AtomicSizeT uses Windows Interlocked operations for lock-free,
    // thread-safe increments without needing a separate mutex.
    forensic::AtomicSizeT alert_count(0);

    std::cout << "\n[*] Processing log file: " << log_file << "\n\n";

    DWORD start_time = GetTickCount();

    // ---------------------------------------------------------------
    // 3. Parse the log file and submit tasks to the thread pool
    // ---------------------------------------------------------------
    size_t total_lines = 0;

    try {
        total_lines = forensic::parse_log_file(log_file,
            [&pool, &matcher, &db_writer, &alert_count](const forensic::LogEntry& entry) {
                // Capture the entry by VALUE for the lambda submitted to the pool.
                // This is critical: the entry reference from parse_log_file is
                // only valid during this callback. The thread pool task may
                // execute later, so we must own our own copy.
                pool.submit([entry, &matcher, &db_writer, &alert_count]() {
                    // Match the entry against all IOC rules.
                    auto matches = matcher.match(entry);

                    // Write each match to the database and report it.
                    for (size_t i = 0; i < matches.size(); ++i) {
                        const auto& m = matches[i];
                        db_writer.write_alert(m);
                        alert_count.fetch_add(1);

                        // Print alert to stdout.
                        std::string display = m.matched_line;
                        if (display.size() > 80) {
                            display = display.substr(0, 80) + "...";
                        }
                        std::cout << "  [ALERT] " << m.rule_name
                                  << " @ line " << m.line_number
                                  << ": " << display << "\n";
                    }
                });
            });
    } catch (const std::exception& e) {
        std::cerr << "[ERROR] " << e.what() << "\n";
        pool.shutdown();
        return 1;
    }

    // ---------------------------------------------------------------
    // 4. Shutdown the thread pool and wait for all tasks to complete
    // ---------------------------------------------------------------
    pool.shutdown();

    DWORD end_time = GetTickCount();
    DWORD duration = end_time - start_time;

    // ---------------------------------------------------------------
    // 5. Print summary
    // ---------------------------------------------------------------
    long final_alerts = alert_count.load();
    size_t db_alerts  = db_writer.get_alert_count();

    std::cout << "\n============================================\n";
    std::cout << "  Analysis Summary\n";
    std::cout << "============================================\n";
    std::cout << "  Log file:         " << log_file << "\n";
    std::cout << "  Total lines:      " << total_lines << "\n";
    std::cout << "  Alerts triggered: " << final_alerts << "\n";
    std::cout << "  Alerts in DB:     " << db_alerts << "\n";
    std::cout << "  Processing time:  " << duration << " ms\n";
    std::cout << "  Worker threads:   " << pool.thread_count() << "\n";
    std::cout << "============================================\n";

    if (final_alerts > 0) {
        std::cout << "\n[!] IOC matches detected! Review alerts.db for details.\n";
        std::cout << "    Query: sqlite3 alerts.db \"SELECT * FROM alerts;\"\n";
    } else {
        std::cout << "\n[+] No IOC matches detected. Log file appears clean.\n";
    }

    return 0;
}

/**
 * @file db_writer.h
 * @brief SQLite database writer for ForensicLogEngine.
 *
 * Design Rationale:
 * -----------------
 * Matched IOC alerts are persisted to a local SQLite database for post-
 * analysis querying, evidence preservation, and report generation.
 * SQLite is chosen because:
 * - Zero configuration - no server needed, just a file.
 * - ACID compliant - guarantees data integrity even on crash.
 * - Portable - the .db file can be moved between machines.
 * - Embedded - compiled directly into the binary via the amalgamation.
 *
 * Thread Safety:
 * - The DatabaseWriter uses its own forensic::Mutex to serialize all
 *   write operations. SQLite in single-thread mode isn't safe for
 *   concurrent writes, so we enforce serialization at the app level.
 * - Read operations (like get_alert_count) also acquire the lock to
 *   avoid reading during a write transaction.
 */

#ifndef FORENSIC_DB_WRITER_H
#define FORENSIC_DB_WRITER_H

#include "pattern_matcher.h"
#include "threading.h"
#include <string>

// Forward declaration - avoids including the full SQLite header here.
struct sqlite3;

namespace forensic {

class DatabaseWriter {
public:
    /**
     * @brief Open (or create) the SQLite database at the given path.
     * @param db_path Path to the .db file (e.g., "alerts.db").
     *
     * Creates the alerts table if it doesn't exist. Uses WAL journal
     * mode for better concurrent read performance.
     *
     * @throws std::runtime_error if the database cannot be opened.
     */
    explicit DatabaseWriter(const std::string& db_path);

    /**
     * @brief Close the database connection cleanly.
     */
    ~DatabaseWriter();

    // Non-copyable - SQLite handles shouldn't be shared.
    DatabaseWriter(const DatabaseWriter&) = delete;
    DatabaseWriter& operator=(const DatabaseWriter&) = delete;

    /**
     * @brief Insert a matched IOC alert into the database.
     * @param result The match result containing rule name, timestamp, etc.
     *
     * Thread Safety: This method acquires an internal mutex before
     * executing the INSERT statement. Multiple threads can safely call
     * this method concurrently - only one INSERT runs at a time.
     */
    void write_alert(const MatchResult& result);

    /**
     * @brief Get the total number of alerts stored in the database.
     * @return The count of rows in the alerts table.
     *
     * Thread Safety: Acquires the mutex to prevent reading during writes.
     */
    size_t get_alert_count();

private:
    /**
     * @brief Initialize the database schema (create tables if needed).
     */
    void initialize_schema();

    sqlite3*  db_;        ///< SQLite database handle
    Mutex     db_mutex_;  ///< Serializes all database operations
};

} // namespace forensic

#endif // FORENSIC_DB_WRITER_H

/**
 * @file db_writer.cpp
 * @brief SQLite database writer implementation for ForensicLogEngine.
 */

#include "db_writer.h"
#include "../vendor/sqlite3/sqlite3.h"
#include <stdexcept>
#include <iostream>

namespace forensic {

DatabaseWriter::DatabaseWriter(const std::string& db_path) : db_(nullptr) {
    // Open the database. SQLITE_OPEN_CREATE creates the file if it
    // doesn't exist. SQLITE_OPEN_READWRITE allows both reads and writes.
    int rc = sqlite3_open_v2(
        db_path.c_str(),
        &db_,
        SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE,
        nullptr  // Use default VFS
    );

    if (rc != SQLITE_OK) {
        std::string error = sqlite3_errmsg(db_);
        sqlite3_close(db_);
        db_ = nullptr;
        throw std::runtime_error("Failed to open database '" + db_path + "': " + error);
    }

    // Enable WAL (Write-Ahead Logging) journal mode for better
    // concurrent read performance. WAL allows readers to proceed
    // without being blocked by writers.
    char* err_msg = nullptr;
    sqlite3_exec(db_, "PRAGMA journal_mode=WAL;", nullptr, nullptr, &err_msg);
    if (err_msg) {
        // WAL failure is non-fatal - we can still operate in default mode.
        std::cerr << "[DatabaseWriter] WAL mode warning: " << err_msg << "\n";
        sqlite3_free(err_msg);
    }

    initialize_schema();
}

DatabaseWriter::~DatabaseWriter() {
    if (db_) {
        // sqlite3_close_v2 is used instead of sqlite3_close because it
        // handles the case where prepared statements haven't been finalized.
        sqlite3_close_v2(db_);
        db_ = nullptr;
    }
}

void DatabaseWriter::initialize_schema() {
    // Create the alerts table if it doesn't exist.
    // Using "IF NOT EXISTS" makes the engine idempotent.
    const char* sql =
        "CREATE TABLE IF NOT EXISTS alerts ("
        "    id           INTEGER PRIMARY KEY AUTOINCREMENT,"
        "    timestamp    TEXT NOT NULL,"
        "    rule_name    TEXT NOT NULL,"
        "    matched_line TEXT NOT NULL"
        ");";

    char* err_msg = nullptr;
    int rc = sqlite3_exec(db_, sql, nullptr, nullptr, &err_msg);

    if (rc != SQLITE_OK) {
        std::string error = err_msg ? err_msg : "unknown error";
        sqlite3_free(err_msg);
        throw std::runtime_error("Failed to create alerts table: " + error);
    }
}

void DatabaseWriter::write_alert(const MatchResult& result) {
    // Acquire the mutex before any database operation.
    // LockGuard is sufficient here (not UniqueLock) because we don't
    // need to wait on a condition variable - just exclusive access.
    LockGuard lock(db_mutex_);

    // Use a prepared statement with parameter binding to:
    // 1. Prevent SQL injection (matched_line could contain any content).
    // 2. Improve performance (SQLite caches the query plan).
    const char* sql =
        "INSERT INTO alerts (timestamp, rule_name, matched_line) VALUES (?, ?, ?);";

    sqlite3_stmt* stmt = nullptr;
    int rc = sqlite3_prepare_v2(db_, sql, -1, &stmt, nullptr);

    if (rc != SQLITE_OK) {
        std::cerr << "[DatabaseWriter] Prepare failed: " << sqlite3_errmsg(db_) << "\n";
        return;
    }

    // Bind parameters. SQLITE_TRANSIENT tells SQLite to make its own
    // copy of the string data, so it's safe even if our strings are
    // destroyed after this call.
    sqlite3_bind_text(stmt, 1, result.timestamp.c_str(),    -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 2, result.rule_name.c_str(),    -1, SQLITE_TRANSIENT);
    sqlite3_bind_text(stmt, 3, result.matched_line.c_str(), -1, SQLITE_TRANSIENT);

    rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        std::cerr << "[DatabaseWriter] Insert failed: " << sqlite3_errmsg(db_) << "\n";
    }

    // Always finalize the statement to free resources, even on error.
    sqlite3_finalize(stmt);
}

size_t DatabaseWriter::get_alert_count() {
    LockGuard lock(db_mutex_);

    const char* sql = "SELECT COUNT(*) FROM alerts;";
    sqlite3_stmt* stmt = nullptr;
    size_t count = 0;

    int rc = sqlite3_prepare_v2(db_, sql, -1, &stmt, nullptr);
    if (rc == SQLITE_OK && sqlite3_step(stmt) == SQLITE_ROW) {
        count = static_cast<size_t>(sqlite3_column_int64(stmt, 0));
    }

    sqlite3_finalize(stmt);
    return count;
}

} // namespace forensic

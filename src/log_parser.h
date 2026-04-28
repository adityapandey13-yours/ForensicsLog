/**
 * @file log_parser.h
 * @brief Log file parser for ForensicLogEngine.
 *
 * Design Rationale:
 * -----------------
 * The parser reads log files line by line and dispatches each line to the
 * thread pool for parallel processing. Each line is parsed into a structured
 * LogEntry containing timestamp, severity level, source process, and message.
 *
 * Expected log format:
 *   [YYYY-MM-DD HH:MM:SS] [LEVEL] [ProcessName] Message text here
 *
 * Parsing Strategy:
 * - We use std::regex to extract the structured fields from each line.
 * - Lines that don't match the expected format are still processed as raw
 *   entries with default fields, ensuring no data is silently dropped —
 *   important in forensic analysis where every line could be evidence.
 */

#ifndef FORENSIC_LOG_PARSER_H
#define FORENSIC_LOG_PARSER_H

#include <string>
#include <functional>

namespace forensic {

/**
 * @brief Structured representation of a single log entry.
 *
 * Fields are stored as strings for flexibility — forensic logs come from
 * many sources with inconsistent formatting, so strict typing (e.g.,
 * std::chrono for timestamps) would cause unnecessary parse failures.
 */
struct LogEntry {
    std::string timestamp;    ///< e.g. "2024-03-15 14:22:01"
    std::string level;        ///< e.g. "INFO", "WARN", "ERROR"
    std::string source;       ///< Process or service name
    std::string message;      ///< Raw message content
    std::string raw_line;     ///< Original unparsed line (preserved for alerts)
    size_t      line_number;  ///< 1-based line number in the file
};

/**
 * @brief Parse a log file and invoke a callback for each parsed entry.
 *
 * @param filepath     Path to the log file to parse.
 * @param on_entry     Callback invoked for each successfully parsed LogEntry.
 *                     The callback receives the entry by const reference.
 * @return             Total number of lines read from the file.
 *
 * The callback approach decouples parsing from processing — the caller
 * decides what to do with each entry (e.g., submit to thread pool,
 * match against IOC rules, etc.).
 *
 * @throws std::runtime_error if the file cannot be opened.
 */
size_t parse_log_file(const std::string& filepath,
                      std::function<void(const LogEntry&)> on_entry);

/**
 * @brief Parse a single raw log line into a LogEntry struct.
 *
 * @param line        The raw log line string.
 * @param line_num    The 1-based line number for reference.
 * @return            A populated LogEntry struct.
 *
 * If the line does not match the expected format, the entry is still
 * created with the raw line preserved and "UNKNOWN" as the level/source.
 */
LogEntry parse_line(const std::string& line, size_t line_num);

} // namespace forensic

#endif // FORENSIC_LOG_PARSER_H

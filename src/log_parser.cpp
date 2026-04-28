/**
 * @file log_parser.cpp
 * @brief Log file parser implementation for ForensicLogEngine.
 */

#include "log_parser.h"
#include <fstream>
#include <regex>
#include <stdexcept>
#include <iostream>

namespace forensic {

LogEntry parse_line(const std::string& line, size_t line_num) {
    LogEntry entry;
    entry.raw_line    = line;
    entry.line_number = line_num;

    // Regex to match the structured log format:
    //   [YYYY-MM-DD HH:MM:SS] [LEVEL] [ProcessName] Message
    //
    // Capture groups:
    //   1: Timestamp  (e.g., "2024-03-15 14:22:01")
    //   2: Log level  (e.g., "ERROR")
    //   3: Source     (e.g., "sshd")
    //   4: Message    (everything after the third bracket group)
    //
    // The regex is intentionally permissive with the message capture (.+)
    // to handle arbitrary message content, including special characters
    // that are common in forensic logs (file paths, IP addresses, etc.).
    static const std::regex log_pattern(
        R"(\[(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})\]\s*\[(\w+)\]\s*\[([^\]]+)\]\s*(.+))"
    );

    std::smatch match;
    if (std::regex_match(line, match, log_pattern)) {
        entry.timestamp = match[1].str();
        entry.level     = match[2].str();
        entry.source    = match[3].str();
        entry.message   = match[4].str();
    } else {
        // Forensic principle: never discard data silently.
        // If a line doesn't match the expected format, we still process it
        // with default fields. The raw_line is always preserved for IOC
        // matching, so malformed lines are still scanned for threats.
        entry.timestamp = "UNKNOWN";
        entry.level     = "UNKNOWN";
        entry.source    = "UNKNOWN";
        entry.message   = line;
    }

    return entry;
}

size_t parse_log_file(const std::string& filepath,
                      std::function<void(const LogEntry&)> on_entry) {
    std::ifstream file(filepath);
    if (!file.is_open()) {
        throw std::runtime_error("Failed to open log file: " + filepath);
    }

    std::string line;
    size_t line_count = 0;

    // Read line by line. std::getline handles both \n and \r\n line
    // endings, which is important since forensic logs may come from
    // different operating systems.
    while (std::getline(file, line)) {
        ++line_count;

        // Skip empty lines — they carry no forensic value.
        if (line.empty()) {
            continue;
        }

        LogEntry entry = parse_line(line, line_count);
        on_entry(entry);
    }

    return line_count;
}

} // namespace forensic

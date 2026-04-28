/**
 * @file pattern_matcher.h
 * @brief IOC (Indicator of Compromise) pattern matcher for ForensicLogEngine.
 *
 * Design Rationale:
 * -----------------
 * The pattern matcher applies a set of IOC rules against each parsed log
 * entry to detect potentially malicious activity. Each rule is a compiled
 * std::regex pattern with a human-readable name and description.
 *
 * Why regex for IOC detection:
 * - Regex provides flexible, expressive pattern matching suitable for
 *   the diverse formats found in forensic logs.
 * - Compiled regex objects (std::regex) are reused across all log lines,
 *   avoiding recompilation overhead.
 * - The rules are case-insensitive to catch variations in log formatting
 *   across different systems and services.
 *
 * Thread Safety:
 * - IOCRule objects are immutable after construction (const regex, const
 *   strings), making them inherently thread-safe for concurrent reads.
 * - std::regex_search is thread-safe when called on different input
 *   strings, which is our use case (each thread processes different lines).
 */

#ifndef FORENSIC_PATTERN_MATCHER_H
#define FORENSIC_PATTERN_MATCHER_H

#include "log_parser.h"
#include <string>
#include <vector>
#include <regex>

namespace forensic {

/**
 * @brief Represents a single IOC detection rule.
 *
 * Each rule has a compiled regex pattern for efficient repeated matching,
 * a human-readable name for alert reporting, and a description explaining
 * the forensic significance.
 */
struct IOCRule {
    std::string name;         ///< Short rule name (e.g., "AUTH_FAILURE_BURST")
    std::string description;  ///< Forensic context for why this matters
    std::regex  pattern;      ///< Compiled regex for matching

    IOCRule(const std::string& n, const std::string& desc, const std::string& pat)
        : name(n), description(desc),
          // ECMAScript syntax with case-insensitive flag.
          // icase is important because log sources vary in capitalization.
          pattern(pat, std::regex_constants::ECMAScript | std::regex_constants::icase) {}
};

/**
 * @brief Result of matching a log entry against an IOC rule.
 */
struct MatchResult {
    std::string rule_name;    ///< Name of the triggered rule
    std::string timestamp;    ///< Timestamp from the log entry
    std::string matched_line; ///< The raw log line that matched
    size_t      line_number;  ///< Line number in the original file
};

/**
 * @brief The pattern matching engine.
 *
 * Holds a set of IOC rules and provides a method to check each log entry
 * against all rules. The rules are loaded once at construction and are
 * immutable, making the matcher safe for concurrent use.
 */
class PatternMatcher {
public:
    /**
     * @brief Construct the matcher with default hardcoded IOC rules.
     *
     * Four built-in rules are loaded:
     * 1. Failed authentication bursts
     * 2. Privilege escalation (sudo failures)
     * 3. Suspicious process names (nc/netcat)
     * 4. Repeated access denied messages
     */
    PatternMatcher();

    /**
     * @brief Match a log entry against all IOC rules.
     * @param entry The parsed log entry to check.
     * @return Vector of MatchResult for each rule that fired.
     *
     * A single log line can trigger multiple rules simultaneously.
     * For example, a line containing both "authentication failure" and
     * "access denied" would produce two match results.
     *
     * Thread Safety: This method is safe to call concurrently from
     * multiple threads because:
     * - rules_ is immutable (read-only after construction).
     * - std::regex_search on different input strings is thread-safe.
     * - Each call operates on its own local MatchResult vector.
     */
    std::vector<MatchResult> match(const LogEntry& entry) const;

    /**
     * @brief Returns the number of loaded IOC rules.
     */
    size_t rule_count() const;

private:
    std::vector<IOCRule> rules_;  ///< Immutable set of IOC detection rules
};

} // namespace forensic

#endif // FORENSIC_PATTERN_MATCHER_H

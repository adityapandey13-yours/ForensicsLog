/**
 * @file pattern_matcher.cpp
 * @brief IOC pattern matcher implementation for ForensicLogEngine.
 */

#include "pattern_matcher.h"

namespace forensic {

PatternMatcher::PatternMatcher() {
    // ----------------------------------------------------------------
    // IOC Rule 1: Failed Authentication Burst Detection
    // ----------------------------------------------------------------
    // Detects lines containing "authentication fail" (covers both
    // "authentication failure" and "authentication failed").
    //
    // Forensic Significance:
    // Repeated authentication failures are a primary indicator of brute-
    // force attacks against SSH, RDP, or web login services. Even a
    // single instance warrants investigation, but bursts (multiple
    // failures in quick succession) are strongly indicative of automated
    // attack tools like Hydra or Medusa.
    rules_.emplace_back(
        "AUTH_FAILURE_BURST",
        "Detects failed authentication attempts indicating brute-force attacks",
        R"(authentication\s+fail)"
    );

    // ----------------------------------------------------------------
    // IOC Rule 2: Privilege Escalation Attempt
    // ----------------------------------------------------------------
    // Detects lines containing both "sudo" and "FAILED" (or "failed").
    //
    // Forensic Significance:
    // Failed sudo attempts indicate a user or process tried to elevate
    // privileges and was denied. This could be a compromised low-privilege
    // account attempting lateral movement or privilege escalation — a
    // critical step in most attack kill chains (MITRE ATT&CK T1548).
    //
    // Regex explanation: (?=.*sudo) is a lookahead ensuring "sudo" appears
    // somewhere in the line, and (?=.*failed) ensures "failed" also appears.
    // This handles any ordering of the two keywords.
    rules_.emplace_back(
        "PRIVILEGE_ESCALATION",
        "Detects failed sudo/privilege escalation attempts",
        R"((?=.*sudo)(?=.*failed))"
    );

    // ----------------------------------------------------------------
    // IOC Rule 3: Suspicious Process Detection
    // ----------------------------------------------------------------
    // Detects references to "nc", "netcat", or "ncat" — common network
    // utility tools frequently used to establish reverse shells.
    //
    // Forensic Significance:
    // Netcat is often called the "Swiss army knife" of networking and is
    // one of the most commonly used tools for establishing unauthorized
    // reverse shells after initial compromise. Its presence in logs
    // (especially as a running process) is a strong IOC.
    //
    // Regex explanation: \b word boundaries ensure we match "nc" as a
    // whole word and not as part of words like "once" or "function".
    rules_.emplace_back(
        "SUSPICIOUS_PROCESS",
        "Detects netcat/nc usage commonly associated with reverse shells",
        R"(\b(nc|netcat|ncat)\b)"
    );

    // ----------------------------------------------------------------
    // IOC Rule 4: Access Denied Detection
    // ----------------------------------------------------------------
    // Detects "access denied", "permission denied", or "unauthorized
    // access" patterns.
    //
    // Forensic Significance:
    // Repeated access denied messages can indicate unauthorized access
    // attempts, misconfigured permissions exploited by an attacker, or
    // a compromised account probing for accessible resources. In post-
    // incident analysis, these entries help reconstruct the attacker's
    // reconnaissance phase.
    rules_.emplace_back(
        "ACCESS_DENIED",
        "Detects repeated access denied / permission denied events",
        R"((access\s+denied|permission\s+denied|unauthorized\s+access))"
    );
}

std::vector<MatchResult> PatternMatcher::match(const LogEntry& entry) const {
    std::vector<MatchResult> results;

    // Check the log entry against every IOC rule.
    // We search both the parsed message AND the raw line to maximize
    // detection — some IOC indicators might appear in the source or
    // level fields that the message alone doesn't capture.
    for (const auto& rule : rules_) {
        // std::regex_search (not regex_match) is used because we want to
        // find the pattern ANYWHERE in the string, not require the entire
        // string to match the pattern.
        if (std::regex_search(entry.raw_line, rule.pattern)) {
            MatchResult result;
            result.rule_name    = rule.name;
            result.timestamp    = entry.timestamp;
            result.matched_line = entry.raw_line;
            result.line_number  = entry.line_number;
            results.push_back(std::move(result));
        }
    }

    return results;
}

size_t PatternMatcher::rule_count() const {
    return rules_.size();
}

} // namespace forensic

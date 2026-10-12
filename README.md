# ForensicLogEngine

A high-performance, multi-threaded log ingestion and IOC (Indicator of Compromise) pattern matching engine for forensic analysis. Built in modern C++17 with zero external dependencies beyond the vendored SQLite3 amalgamation. Fully compatible with MinGW.org GCC on Windows.

## Architecture Overview

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│  Log Parser  │────>│  Thread Pool  │────>│ Pattern Matcher  │────>│  DB Writer   │
│ (Sequential) │     │  (Parallel)   │     │  (Concurrent)    │     │ (Serialized) │
└─────────────┘     └──────────────┘     └─────────────────┘     └──────────────┘
     File I/O         N workers            IOC regex rules         SQLite alerts.db
```

### Components

| Component | Files | Purpose |
|-----------|-------|---------|
| **Threading** | `threading.h` | Custom thread primitives wrapping Windows API (CRITICAL_SECTION, CONDITION_VARIABLE, _beginthreadex) for MinGW compatibility. |
| **Thread Pool** | `thread_pool.h/.cpp` | Fixed-size worker pool with task queue, mutex, and condition variable. |
| **Log Parser** | `log_parser.h/.cpp` | Reads log files line-by-line, parses each into a structured `LogEntry`. |
| **Pattern Matcher** | `pattern_matcher.h/.cpp` | Matches entries against compiled IOC regex rules. Thread-safe (immutable). |
| **Database Writer** | `db_writer.h/.cpp` | Persists matched alerts to SQLite via mutex-serialized prepared statements. |

### Threading Model

- **File I/O**: Sequential (single-threaded) — disk I/O doesn't benefit from parallelism.
- **Processing**: Parallel via thread pool — each log line is processed independently.
- **Database writes**: Serialized via Mutex — SQLite requires single-writer access.
- Uses custom Windows API wrappers (`threading.h`) since MinGW.org GCC 6.x doesn't provide `<thread>`, `<mutex>`, or `<condition_variable>`.

## Build Instructions (MinGW on Windows)

### Prerequisites

- [MinGW](https://www.mingw-w64.org/) with g++ supporting C++17 (or C++14 minimum)
- [CMake](https://cmake.org/) 3.15 or newer

### Build Steps

```bash
cd ForensicLogEngine
mkdir build
cd build
cmake -G "MinGW Makefiles" ..
mingw32-make
```

The executable will be at `build/bin/forensic_log_engine.exe`.

## Usage

```bash
forensic_log_engine.exe logs/sample.log
```

### Sample Output

```
============================================
  ForensicLogEngine v1.0
  Multi-threaded IOC Detection Engine
============================================

[*] Thread pool initialized with 16 worker threads
[*] Pattern matcher loaded with 4 IOC rules
[*] Database writer initialized (alerts.db)

[*] Processing log file: logs/sample.log

  [ALERT] AUTH_FAILURE_BURST @ line 5: ...
  [ALERT] PRIVILEGE_ESCALATION @ line 10: ...
  [ALERT] SUSPICIOUS_PROCESS @ line 16: ...
  [ALERT] ACCESS_DENIED @ line 13: ...
  ...

============================================
  Analysis Summary
============================================
  Log file:         logs/sample.log
  Total lines:      31
  Alerts triggered: 12
  Alerts in DB:     12
  Processing time:  31 ms
  Worker threads:   16
============================================

[!] IOC matches detected! Review alerts.db for details.
```

### Querying the Database

```bash
sqlite3 alerts.db "SELECT * FROM alerts;"
sqlite3 alerts.db "SELECT rule_name, COUNT(*) FROM alerts GROUP BY rule_name;"
```

## IOC Rules

| Rule Name | Pattern | Forensic Significance |
|-----------|---------|----------------------|
| **AUTH_FAILURE_BURST** | `authentication fail` | Detects brute-force login attacks (SSH, RDP). Repeated failures indicate automated tools like Hydra. |
| **PRIVILEGE_ESCALATION** | `sudo` + `failed` | Catches failed privilege escalation — a key step in attack kill chains (MITRE ATT&CK T1548). |
| **SUSPICIOUS_PROCESS** | `nc` / `netcat` / `ncat` | Flags reverse shell tools. Netcat is the most common backdoor connection tool. |
| **ACCESS_DENIED** | `access denied` / `permission denied` / `unauthorized access` | Identifies reconnaissance activity where an attacker probes for accessible resources. |

## Project Structure

```
ForensicLogEngine/
├── CMakeLists.txt
├── README.md
├── logs/
│   └── sample.log          (31 lines, 12 IOC triggers)
├── vendor/
│   └── sqlite3/
│       ├── sqlite3.h
│       └── sqlite3.c
└── src/
    ├── main.cpp             (entry point, wires components)
    ├── threading.h          (Windows API thread wrappers)
    ├── thread_pool.h/.cpp   (worker pool + task queue)
    ├── log_parser.h/.cpp    (line parser + LogEntry struct)
    ├── pattern_matcher.h/.cpp (IOC regex engine)
    └── db_writer.h/.cpp     (SQLite persistence)
```

## License

This project is provided for educational and forensic analysis purposes.

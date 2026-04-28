cmake_minimum_required(VERSION 3.15)

project(ForensicLogEngine VERSION 1.0.0 DESCRIPTION "Multi-threaded IOC pattern matching engine" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
    add_compile_options(-Wall -Wextra)
endif()

# Vendored SQLite3
add_library(sqlite3_vendor STATIC vendor/sqlite3/sqlite3.c)
set_target_properties(sqlite3_vendor PROPERTIES LANGUAGE C C_STANDARD 11)
if(CMAKE_C_COMPILER_ID MATCHES "GNU|Clang")
    target_compile_options(sqlite3_vendor PRIVATE -w)
endif()
target_compile_definitions(sqlite3_vendor PRIVATE SQLITE_THREADSAFE=1)
target_include_directories(sqlite3_vendor PUBLIC ${CMAKE_SOURCE_DIR}/vendor/sqlite3)

# Main executable
add_executable(forensic_log_engine
    src/main.cpp
    src/thread_pool.cpp
    src/log_parser.cpp
    src/pattern_matcher.cpp
    src/db_writer.cpp
)

target_link_libraries(forensic_log_engine PRIVATE sqlite3_vendor)

find_package(Threads REQUIRED)
target_link_libraries(forensic_log_engine PRIVATE Threads::Threads)

target_include_directories(forensic_log_engine PRIVATE
    ${CMAKE_SOURCE_DIR}/src
    ${CMAKE_SOURCE_DIR}/vendor/sqlite3
)

set_target_properties(forensic_log_engine PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin
)

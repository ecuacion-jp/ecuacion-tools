# ecuacion-tools - Claude Code Guidelines

## Project Overview

Provides executable tools useful for server and application maintenance. Multi-module Maven project using Spring Batch.

- **Java**: 21
- **Build tool**: Maven
- **Main modules**: `ecuacion-tool-command-api`, `ecuacion-tool-housekeep-db`, `ecuacion-tool-housekeep-files`

## Java Coding Rules

### Style Standards
- Follows **Google Java Style Guide** (enforced by Checkstyle in CI)
- Indentation: **2 spaces** (no tabs)
- Max line length: **100 characters** (excluding package/import statements) — **applies to comments too**
- Encoding: **UTF-8**

### Imports
- Wildcard imports (`.*`) are **prohibited**
- Imports are sorted automatically (follow IDE auto-organize imports)

### Javadoc
- **All public classes, methods, and fields must have Javadoc**
- When editing existing files, review and update Javadoc for any modified methods

### License Header
- All Java files must have the Apache 2.0 license header at the top
- Follow the same format as existing files

## File Creation and Editing Rules

### Creating New Files
- Always refer to existing files in the same package before creating a new one
- When adding to a package that has `package-info.java`, check its contents first

## Work Style

- **Commit only when explicitly instructed**
- **Push only when explicitly instructed**
- Always confirm before destructive operations (file deletion, `git reset --hard`, etc.)
- Do not propose changes to code that has not been read first

## Build and Verification

```bash
# Build all modules
mvn compile

# Test a specific module
mvn test -pl ecuacion-tool-housekeep-files -am

# Checkstyle verification (run in CI)
mvn checkstyle:check
```

**Always run the following after editing Java files and fix any violations before finishing:**

```bash
mvn checkstyle:check spotbugs:check
mvn javadoc:javadoc
```

The most common violations are:
- Checkstyle: Line length over 100 characters (including comments and Javadoc)
- Checkstyle: Missing Javadoc on public members
- Checkstyle: Wildcard imports
- SpotBugs: Using reflection to access private fields (use `protected` scope workarounds where needed)

# ecuacion-tools

[![Java CI](https://github.com/ecuacion-jp/ecuacion-tools/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/ecuacion-jp/ecuacion-tools/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/ecuacion-jp/ecuacion-tools/branch/main/graph/badge.svg)](https://codecov.io/gh/ecuacion-jp/ecuacion-tools)
[![GitHub Release](https://img.shields.io/github/v/release/ecuacion-jp/ecuacion-tools)](https://github.com/ecuacion-jp/ecuacion-tools/releases)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/downloads/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## What is it?

`ecuacion-tools` provides executable tools useful for maintaining servers and applications.

- **ecuacion-tool-housekeep-db** — Deletes old records from a database based on configurable retention rules.
- **ecuacion-tool-housekeep-files** — Deletes or archives old files on local or remote (SFTP) filesystems based on configurable rules.

## Versioning

This project follows the spirit of [Semantic Versioning](https://semver.org/). Major version increments indicate breaking changes.

## System Requirements

- JDK 21 or above.

## Documentation

- See the `Documentation` section of the `README` in each module for details.

## Download & Usage

Download the executable JAR for the tool you need from [GitHub Releases](https://github.com/ecuacion-jp/ecuacion-tools/releases), then run it with:

```bash
java -jar ecuacion-tool-housekeep-files-x.x.x.jar
java -jar ecuacion-tool-housekeep-db-x.x.x.jar
```

See the `README` in each module for configuration details.

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for how to report bugs, suggest features, and submit pull requests.

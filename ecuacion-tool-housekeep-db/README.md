# ecuacion-tool-housekeep-db

## What is it?

`ecuacion-tool-housekeep-db` housekeeps records in a database. It handles soft and hard delete.
`PostgreSQL` and `MySQL` / `MariaDB` are supported.

- It deletes conditionally (only records with defined term passed).
- It is able to delete records in related tables at the same time.
- It is able to skip deletion when a record in related table exists.

## Documentation

- [ecuacion-references-tools](https://references.ecuacion.jp/ecuacion-references-tools/public/showMarkdown/page?id=housekeep-db/overview) — Official reference documentation

# ecuacion-tool-housekeep-db

## What is it?

`ecuacion-tool-housekeep-db` housekeeps records in a database. It handles soft and hard delete.
`PostgreSQL` and `MySQL` / `MariaDB` are supported.

- It deletes conditionally (only records with defined term passed).
- It is able to delete records in related tables at the same time.
- It is able to skip deletion when a record in related table exists.
- It is able to check that no records match given conditions (e.g. records left unprocessed for a defined term), sending a warning email instead of deleting them when any are found.

Check out the reference documentation below for setup instructions, a quick start guide, and more!

## Documentation

- Official reference documentation - [ecuacion-references-tools](https://references.ecuacion.jp/ecuacion-references-tools/public/showMarkdown/page?id=housekeep-db/overview)

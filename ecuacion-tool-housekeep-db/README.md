# ecuacion-tool-housekeep-db

## What is it?

`ecuacion-tool-housekeep-db` housekeeps records in a database. It handles soft and hard delete.

- It deletes conditionally (only records with defined term passed).
- It is able to delete records in related tables at the same time.
- It is able to skip deletion when a record in related table exists.

## System Requirements

- JDK 21 or above.
- Only `Postgresql` is supported (for now)

## Documentation

- [javadoc](https://javadoc.ecuacion.jp/apidocs/ecuacion-tool-housekeep-db/)

## Premises

1. Tables to be deleted need to have single column primary key or unique index (not composite primary key / unique index).
   (The column with primary key or unique index is referred to as `ID Column` in the document)
1. Soft Delete Column needs to be `bool`. (`true` means soft-deleted)

## Installation

1. Download the jar module from [here](https://maven-repo.ecuacion.jp/public/jp/ecuacion/tool/ecuacion-tool-housekeep-db/).  
   (full url should be like 'https://maven-repo.ecuacion.jp/public/jp/ecuacion/tool/ecuacion-tool-housekeep-db/14.3.0/ecuacion-tool-housekeep-db-14.3.0.jar')

1. Download the settings excel file template (housekeep-db(fmt-vx.x.x-xx)_sample.xlsx) from [here](https://github.com/ecuacion-jp/ecuacion-tools/tree/main/ecuacion-tool-housekeep-db/local-test) and update.

1. (Optional) Create `logback-spring.xml` file and put CLASSPATH directory (any directory is fine).  
   Maybe it's easier that you download [the sample](https://github.com/ecuacion-jp/ecuacion-tools/tree/main/ecuacion-tool-housekeep-db/src/envs/local/resources) and update `log-dir` directory path.

## Getting Started

1. Prepare database
   1. Create table
      ```
      CREATE TABLE test_table (num1 number, char1 varchar PRIMARY KEY (num1));
      ```
   1. Insert a record
      ```
      INSERT INTO test_table (num1, char1) VALUES (123, 'abc');
      ```

1. Prepare Settings Excel File
   1. Fill in `DB Connection Settings` sheet. (Let's say `DB Connection ID` is `test-conn` as an example)
   2. Fill in `Housekeep DB Settings` as follows. (only left 6 columns shown below. Ignore right columns in the table for now)
      | Task ID | DB Connection ID | Soft / Hard Delete | Table Name | ID Column Name | ID Column Literal Symbol |
      | ----    | ----             | ----               | ----       | ----           | ----                     |
      | task-1  | test-conn        | Hard Delete        | test_table | num1           | (none)                   |

1. Execute it with the command below.  
   (the filename of the excel file can be changed freely)

   ```
   java -jar ecuacion-tool-housekeep-db-14.3.0.jar [--classpath=/path/to/classpath/directory] excelPath=/path/to/housekeep-db\(fmt-vx.x.x-xx\)_sample.xlsx
   ```
   You can see the record created by insert statement has been deleted.

### Explanation

We don't think much of explanations are needed, but some supplement here.

* `ID Column Literal Symbol` specifies ID column needs '' when you insert values. `(none)` for `int` or `boolean`, `quotes(')` for `varchar`.

## Features

### Soft Delete

#### Basics

1. `Soft Delete` means that the record is not physically deleted, just `deleted` column is set to `true` or something like that instead.
1. In excel settings of `Getting Started` `Hard Delete` was selected in `Soft / Hard Delete` column. To execute soft-delete, set `Soft Delete` there, and you have to set `Soft Delete Column Name`.
   By executing the procedure with the excel of this settings, all the records are soft-deleted.
1. If you want to soft-delete records with specified terms passed only, set `Expiration Check: Timestamp Column Name`, `Expiration Check: Timestamp Column Data Type`, `Expiration Check: Validity Days` columns.
   In the case that the timestamp column name is `last_updated` with `LocalDateTime` datatype (= timestamp without time zone. Set `OffsetDateTime` when you treat timestamp with time zone): 
      | Expiration Check: Timestamp Column Name | Expiration Check: Timestamp Column Data Type | Expiration Check: Validity Days | 
      | ----                                    | ----                                         | ----                            | 
      | last_updated                            | LocalDateTime                                | 28                              |   
1. If you want to update a timestamp column on soft-delete, set `Soft Delete: Update Timestamp Column Name`.
1. If you want to update a user ID column (or a column for other usage is fine) on soft-delete records, set `Soft Delete: Update User ID Column Name`, `Soft Delete: Update User ID Column Literal Symbol` and `Soft Delete: Update User ID Column Value`.

### Hard Delete

1. Just like `Soft Delete`, On `Hard Delete` you can set days to delete by setting `Expiration Check: Timestamp Column Name`, `Expiration Check: Timestamp Column Data Type`, `Expiration Check: Validity Days`.
1. On hard-delete, It's not required to set `Soft Delete Column Name` column, but when you set it, hard-delete is executed only when the value of the column is `true`.   

### Search Condition Settings

1. On deletion you sometimes want to add more conditions. Maybe you want to soft-delete records with status 'completed', for example.
   In that case, you can set the condition in `Search Condition Settings` sheet.
     | Task ID | Search Condition Column Name | Search Condtion Column Literal Symbol | Search Condition Column Value |
     | ----    | ----                         | ----                                  | ----                          | 
     | task-1  | exit_code                    | quotes(')                             | COMPLETED                     |

### Related Table Settings

1. Sometimes you want to delete related tables at the same time.
   Maybe you have parent and child tables, you want to housekeep both tables. You can realize it by setting two tasks in `Housekeep DB Settings`, but sometimes records in child table wants to be deleted when a value of a timestamp column in PARENT table passed a certain term.
      | Task ID | Related Table Process Pattern | Target Table Column Name | Related Table Name | Related Table ID Column Name | Related Table ID Column Literal Symbol |
      | ----    | ----                          | ----                     | ----               | ----                         | ----                                          |
      | task-1  | Delete                        | child_id_column          | child_table        | id_column                    |                      (none)                   |
   
1. Sometimes you want to skip deletion when related tables has related record.
   In that case you set `Check and Skip Delete` to `Related Table Process Pattern` column.

## Specification

### Database Transaction

* Commit is executed when each task in `Housekeep DB Settings` finished.
* When `Table Name` of a task in `Housekeep DB Settings` has over 1000 records to be deleted, commit is executed in every 1000 records.


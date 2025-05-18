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
      | sample  | test-conn        | Hard Delete        | test_table | num1           | (none)                   |

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

1. `Soft Delete` means that the record is not deleted, just `Deleted` column is set to `true` or something like that.
1. In excel settings of `Getting Started` `Hard Delete` was selected in `Soft / Hard Delete` column. To execute Soft Delete, set `Soft Delete` there, and you have to set `Soft Delete Column Name`.
   By executing the procedure with this settings excel, all the records are soft-deleted.
1. If you want to soft-delete records with specified terms passed only, set `Expiration Check: Timestamp Column Name`, `Expiration Check: Timestamp Column Data Type`, `Expiration Check: Validity Days` columns.
   In the case that the timestamp column name is `last_updated` with `LocalDateTime` datatype (= timestamp without time zone. Set `OffsetDateTime` when you treat timestamp with time zone): 
      | Expiration Check: Timestamp Column Name | Expiration Check: Timestamp Column Data Type | Expiration Check: Validity Days | 
      | ----                                    | ----                                         | ----                            | 
      | last_updated                            | v test-conn        | Hard Delete        | test_table | num1           | (none)                   |
   


### Hard Delete

1. In `Getting Started` all the records in DB was deleted because no conditions on deletion added, but usually nobudy wants to delete all the records.
   `ecuacion-tool-housekeep-db` sets conditions on expiration terms and delete
   1. 
   


### 







## Specification

### Security

* To keep secure, Scripts cannot be executed without defining it in `ecuacion-tool-command-api.properties`.  
  Even if so, you can still define risky scripts like `script.delete=/path/to/delete-file.sh`. Think about it.  
  (We don't have any responsibilities however you use it)

* script ID (`scriptId` URL parameter) defined in `ecuacion-tool-command-api.properties` is validated with regular expression `^[a-zA-Z0-9.-_]*$`.

* script file path defined in `ecuacion-tool-command-api.properties` is validated with regular expression `^[a-zA-Z0-9.-_/${}]*$`.

### Response Status and Return Code

* HTTP 403 / 404 : URL (http[s]://yourdomain.com/ecuacion-tool-command-api/api/public/executeScript) is wrong.

* HTTP 400 : The script name specified by `scriptId=` is not defined in `ecuacion-tool-command-api.properties`.

* HTTP 500 :  

  - `ecuacion-tool-command-api.properties` file not found on classpath.
  
  ```json
  {
    "type": "about:blank",
    "title": "Internal Server Error",
    "status": 500,
    "detail": "'ecuacion-tool-command-api.properties' not found on classpath.",
    "instance": "/ecuacion-tool-command-api/api/public/executeScript"
  }
  ```

  - Script file not found.

  ```json
  {
    "type": "about:blank",
    "title": "Internal Server Error",
    "status": 500,
    "detail": "scriptFilePath '/path/to/script/directory/sayHello.sh' not found.",
    "instance": "/ecuacion-tool-command-api/api/public/executeScript"
  }
  ```

* HTTP 200 : Script executed. (the value of `returnCode` is `return code` or `exit status` obtained from shell script with `${?}`)

  ```bash
  {
    "returnCode": "0"
  }
  ```

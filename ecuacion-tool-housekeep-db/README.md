# ecuacion-tool-housekeep-db

## What is it?

`ecuacion-tool-housekeep-db` housekeeps records in a database. It handles soft and hard delete.

- It deletes conditionally (only records with defined term passed).
- It is able to delete records in related tables at the same time.
- It is able to skip deletion when a record in related table exists.

## System Requirements

- JDK 21 or above.
- Postgresql is supported for now

## Documentation

- [javadoc](https://javadoc.ecuacion.jp/apidocs/ecuacion-tool-housekeep-db/)

## Installation

1. Download the war module from [here](https://maven-repo.ecuacion.jp/public/jp/ecuacion/tool/ecuacion-tool-housekeep-db/).  
   (full url should be like 'https://maven-repo.ecuacion.jp/public/jp/ecuacion/tool/ecuacion-tool-housekeep-db/14.3.0/ecuacion-tool-housekeep-db-14.3.0.jar')

1. Download the settings excel file template (housekeep-db(fmt-vx.x.x-xx)_sample.xlsx) from [here](https://github.com/ecuacion-jp/ecuacion-tools/tree/main/ecuacion-tool-housekeep-db/local-test) and update.

1. (Optional) Create `logback-spring.xml` file and put classpath directory (any directory is fine).  
   Maybe it's easier that you download [the sample](https://github.com/ecuacion-jp/ecuacion-tools/tree/main/ecuacion-tool-housekeep-db/src/envs/local/resources) and update `log-dir` directory path.

1. Execute it with the command below.  
   (the filename of the excel file can be changed freely)

   ```
   java -jar ecuacion-tool-housekeep-db-14.3.0.jar [--classpath=/path/to/classpath/directory] excelPath=/path/to/housekeep-db\(fmt-vx.x.x-xx\)_sample.xlsx
   ```


1. Create file `logback-spring-ecuacion-tool-command-api.xml` with the content below and put it to the CLASSPATH directory.  
   (change '/path/to/logs/directory' to any directory in your serer)
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <!DOCTYPE xml>
   <configuration>

	   <!-- appenders -->
	   <property name="log-dir" value="/path/to/logs/directory" />
	   <property name="loglevel-spring" value="INFO" />
	   <include resource="logback-spring-appenders.xml" />
	   <include resource="logback-spring-appenders-local.xml" />

	   <!-- loggers -->
	   <property name="loglevel-jp.ecuacion" value="INFO" />
	   <property name="loglevel-security" value="INFO" />
	   <property name="loglevel-sql" value="INFO" />
	   <property name="loglevel-root" value="INFO" />
	   <include resource="logback-spring-loggers-for-local.xml" />
	   <include resource="logback-spring-loggers-web-for-local.xml" />

   </configuration>
   ```

1. Put a new file `ecuacion-tool-command-api.properties` to the CLASSPATH directory.  

## Getting Started

### Script Preparation

1. Add the content below to `ecuacion-tool-command-api.properties`. (change '/path/to/script/directory' to any directory in your serer)

   ```bash
   script.say-hello=/path/to/script/directory/sayHello.sh
   ```
1. Put a script named 'sayHello.sh' where the path specifies and set proper access privileges with the user the application server works with.

### Execute Script through ecuacion-tool-command-api

1. Access URL below and the script `sayHello.sh` is executed.

   ```URL
   http[s]://yourdomain.com/ecuacion-tool-command-api/api/public/executeScript?scriptId=script.say-hello
   ```

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

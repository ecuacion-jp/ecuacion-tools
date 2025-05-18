# ecuacion-tool-command-api

 ## What is it?

 `ecuacion-tool-command-api` receives commands through web API and execute them in the installed server.

 ## System Requirements

 - JDK 21 or above.
 - Linux or mac OS supported.

 ## Documentation

 - [javadoc](https://javadoc.ecuacion.jp/apidocs/ecuacion-tool-command-api/)

 ## Installation

 1. Download the war module from [here](https://maven-repo.ecuacion.jp/public/jp/ecuacion/tool/ecuacion-tool-command-api/).  
    (full url should be like 'https://maven-repo.ecuacion.jp/public/jp/ecuacion/tool/ecuacion-tool-command-api/14.3.0/ecuacion-tool-command-api-14.3.0.war')

 1. Maybe you want change its filename to `ecuacion-tool-command-api.war` or `ecuacion-tool-command-api##14.3.0.war` (See '[parallel deployment](https://tomcat.apache.org/tomcat-10.0-doc/config/context.html#Parallel_deployment)' feature in Tomcat) to make the context string independent to the module version.

 1. Deploy the war to some application server like Tomcat.

 1. Add CLASSPATH environment variable to the application server. Set an accessible directory.  
    If you use Tomcat, put `setenv.sh` script file to `${CATALINA_HOME}/bin` with the content below.

    ```bash
    CLASSPATH=/path/to/classpath/directory
    export CLASSPATH
    ```

 1. Create file `logback-spring-ecuacion-tool-command-api.xml` with the content below and put it to the CLASSPATH directory.  
    (Change '/path/to/logs/directory' to any directory in your server)
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
 1. Put a script named 'sayHello.sh' where the path specifies and set proper access privileges with the user the application server was started by.  
    Any script content is fine, but for example as follows.

    ```bash
    #!/bin/bash

    touch /path/to/script/directory/touch.file
    echo "Touch done."
    ```

 ### Execute Script through ecuacion-tool-command-api

 1. Access URL below and the script `sayHello.sh` is executed.

    ```URL
    http[s]://yourdomain.com/ecuacion-tool-command-api/api/public/executeScript?scriptId=script.say-hello
    ```

    Now you'll get the execution result.

 ## Specification

 ### Features

 * Script Parameters

   You can give parameters to the script.
   ```URL
   http[s]://yourdomain.com/ecuacion-tool-command-api/api/public/executeScript?scriptId=script.say-hello&parameter=param1,param2
   ```

   By sending the URL above, `sayHello.sh param1 param2` will be executed.
   (For now there's no way to escape comma character, so you cannot pass a parameter string which contains commas. (separated forcibly))

 * Environment Variable Resolution in Script Path

   You can set a path with environment variables to `ecuacion-tool-command-api.properties`.  
   For example: 
   ```bash
   script.say-hello=${USER_HOME}/script/directory/sayHello.sh
   ```

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

 * HTTP 200 : Script executed. (the value of `returnCode` is `return code` or `exit status` obtained from shell script by getting the value of `${?}`)

   ```bash
   {
     "returnCode": "0"
   }
   ```

 ### Security

 * To keep secure, scripts cannot be executed without defining it in `ecuacion-tool-command-api.properties`.  
   Even if so, you can still define risky scripts like `script.delete=/path/to/delete-file.sh`. Think about it.  
   (We are not responsible for any damages you may incur.)

 * script ID (`scriptId` URL parameter) defined in `ecuacion-tool-command-api.properties` is validated with regular expression `^[a-zA-Z0-9.-_]*$`.

 * script file path defined in `ecuacion-tool-command-api.properties` is validated with regular expression `^[a-zA-Z0-9.-_/${}]*$`.
 
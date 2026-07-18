# ecuacion-tool-command-api

 ## What is it?

 `ecuacion-tool-command-api` receives commands through web API and execute them in the installed server.
 Linux or mac OS supported.

 ## Documentation

 - [javadoc](https://docs.ecuacion.jp/javadoc/apidocs/ecuacion-tool-command-api/)

 ## Installation

 1. Download the war module from [here](https://maven-repo.ecuacion.jp/public/jp/ecuacion/tool/ecuacion-tool-command-api/).  
    (full url should be like 'https://maven-repo.ecuacion.jp/public/jp/ecuacion/tool/ecuacion-tool-command-api/14.3.0/ecuacion-tool-command-api-14.3.0.war')

 1. Run it as a standalone executable war (recommended), or deploy it to an existing application server.

    **Standalone (recommended)**

    ```bash
    java -jar ecuacion-tool-command-api-x.x.x.war
    ```

    **Deploy to an existing Tomcat**

    Maybe you want to change its filename to `ecuacion-tool-command-api.war` or `ecuacion-tool-command-api##14.3.0.war` (See '[parallel deployment](https://tomcat.apache.org/tomcat-10.0-doc/config/context.html#Parallel_deployment)' feature in Tomcat) to make the context string independent to the module version, then deploy the war to Tomcat as usual.

 ## Getting Started

 ### Script Preparation

 1. Register the script in `ecuacion-tool-command-api.properties`, placed next to the war (or in a `config` subdirectory alongside it — see [Configuration](#configuration) below for every supported location). Change '/path/to/script/directory' to any directory in your server.

    ```properties
    script.say-hello=/path/to/script/directory/sayHello.sh
    ```
 1. Put a script named 'sayHello.sh' where the path specifies and set proper access privileges with the user the application was started by.  
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

 * HTTP 400 :

   - The `scriptId=` value doesn't match `^[a-zA-Z0-9.-_]*$`.
   - The script name specified by `scriptId=` is not defined in `ecuacion-tool-command-api.properties`.

 * HTTP 500 :

   - The script file path registered for the `scriptId` doesn't match `^[a-zA-Z0-9.-_/${}]*$` (a misconfigured `ecuacion-tool-command-api.properties`).
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

 ## Configuration

 `ecuacion-tool-command-api.properties` is loaded the same way Spring Boot loads `application.properties` — it's merged in from any of these locations (highest priority first), instead of requiring a CLASSPATH directory:

 1. The path given by `-Dspring.config.location=...`
 1. `config/ecuacion-tool-command-api.properties`, in a `config` subdirectory next to the war
 1. `ecuacion-tool-command-api.properties`, right next to the war

 The same applies to `application.properties` itself, so app-level settings (e.g. server port) can live alongside the script registrations.
 
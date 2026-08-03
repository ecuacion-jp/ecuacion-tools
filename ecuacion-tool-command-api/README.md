# ecuacion-tool-command-api

 ## What is it?

 `ecuacion-tool-command-api` receives commands through web API and execute them in the installed server.
 Linux or mac OS supported.

 - Script Parameters — parameters can be passed to the script through the `parameter=` query parameter.
 - Environment Variable Resolution in Script Path — script paths registered in `ecuacion-tool-command-api.properties` can contain `${ENV_VAR}` placeholders.

 ## Documentation

 - [ecuacion-references-tools](https://references.ecuacion.jp/ecuacion-references-tools/public/showMarkdown/page?id=command-api/overview) — Official reference documentation

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

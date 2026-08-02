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
    script.say-hello=GET:/path/to/script/directory/sayHello.sh
    ```

    The `GET:` prefix here allows this script to also be called via GET on `api/key/executeScript` (handy for trying it out from a browser once you have an API key set up). See [Access Control](#access-control) for the full prefix rules — you can drop it and the script defaults to POST only.
 1. Put a script named 'sayHello.sh' where the path specifies and set proper access privileges with the user the application was started by.  
    Any script content is fine, but for example as follows.

    ```bash
    #!/bin/bash

    touch /path/to/script/directory/touch.file
    echo "Touch done."
    ```

 ### Execute Script through ecuacion-tool-command-api

 By default, `api/key/executeScript` always requires an `X-Api-Key` header, and only accepts POST unless the script's own definition opts into GET (see [Access Control](#access-control)). The no-key `api/public/executeScript` GET endpoint is disabled entirely by default.

 1. **`api/key/executeScript` with `X-Api-Key` header (recommended for production; always requires the key, regardless of `api-key-required`)**

    Register the shared secret in a file and point `jp.ecuacion.tool.command-api.api-key-file-path` at it (see [Configuration](#configuration)), then:

    ```bash
    curl -X POST "http[s]://yourdomain.com/ecuacion-tool-command-api/api/key/executeScript" \
         -H "X-Api-Key: your-shared-secret" \
         --data-urlencode "scriptId=script.say-hello"
    ```

    If `script.say-hello` is registered with a `GET:` or `ALL:` prefix (as in the [Getting Started](#getting-started) example above), the same call also works as GET:

    ```bash
    curl "http[s]://yourdomain.com/ecuacion-tool-command-api/api/key/executeScript?scriptId=script.say-hello" \
         -H "X-Api-Key: your-shared-secret"
    ```

 1. **`api/public/executeScript`, no key needed (only when `jp.ecuacion.tool.command-api.api-key-required=false`, e.g. trusted internal networks or manual testing)**

    ```URL
    http[s]://yourdomain.com/ecuacion-tool-command-api/api/public/executeScript?scriptId=script.say-hello
    ```

    This one is always GET, for every registered script, regardless of its `GET:`/`POST:`/`ALL:` prefix — see [Access Control](#access-control).

    Either way, you'll get the same execution result.

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

 * HTTP 403 / 404 : URL is wrong, a GET request to `api/public/executeScript` arrived while `jp.ecuacion.tool.command-api.api-key-required` is not `false`, or a request to `api/key/executeScript` used an HTTP method the script's definition doesn't allow (see [Access Control](#access-control)).

 * HTTP 401 : A GET or POST request to `api/key/executeScript` was missing the `X-Api-Key` header, the header didn't match, or the api-key file was unreadable / unconfigured on the server side. This is enforced by ecuacion-splib-rest before the request ever reaches this application's code; see its `SplibApiKeyAuthenticationFilter`. All of these causes are intentionally reported identically (so a caller can't distinguish a server misconfiguration from a wrong key); check the server-side log to tell them apart.

 * HTTP 400 :

   - The `scriptId=` value doesn't match `^[a-zA-Z0-9.-_]*$`.
   - The script name specified by `scriptId=` is not defined in `ecuacion-tool-command-api.properties`.

 * HTTP 500 :

   - The script file path registered for the `scriptId` doesn't match `^[a-zA-Z0-9.-_/${}]*$` (a misconfigured `ecuacion-tool-command-api.properties`).
   - Script file not found.
   - Script file is not executable, fails to start, or an `${ENV_VAR}` in its registered path can't be resolved.

   None of these responses include the actual resolved file path, environment variable name, or other server-side detail — only `scriptId` and a pointer to check the server log, so a caller (who may only hold a valid `X-Api-Key`, or, when `api-key-required=false`, may be unauthenticated) can't use them to map out the server's filesystem layout. The full detail is logged server-side.

   ```json
   {
     "type": "about:blank",
     "title": "Internal Server Error",
     "status": 500,
     "detail": "scriptFilePath for scriptId 'say-hello' was not found. See the server log for details.",
     "instance": "/ecuacion-tool-command-api/api/key/executeScript"
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

 * script file path defined in `ecuacion-tool-command-api.properties` is validated with regular expression `^[a-zA-Z0-9.-_/${}]*$` (after stripping the optional `GET:`/`POST:`/`ALL:` prefix described below).

 * `api/key/executeScript` always requires a matching `X-Api-Key` header, regardless of `api-key-required` — that flag only ever controls whether the separate no-key `api/public/executeScript` endpoint is reachable at all, it never weakens `api/key/executeScript`. The key is a simple shared secret compared against a file placed on the server — it is **not** an asymmetric (public/private) key pair, and the client-supplied value is never treated as a private key. See [Access Control](#access-control).

 * Which HTTP method(s) `api/key/executeScript` accepts for a given script is controlled per-script (see [Access Control](#access-control)) — it defaults to POST only, so exposing a script over GET is an explicit opt-in, not something granted automatically just by having a valid key.

 ## Configuration

 ### Access Control

 Two properties in `application.properties` control access to `executeScript` (they are intentionally **not** set in the shipped `application.properties`, so that leaving them unconfigured is logged as a warning at startup rather than silently defaulting):

 | Property | Type | Description |
 | --- | --- | --- |
 | `jp.ecuacion.tool.command-api.api-key-required` | boolean | `true` (default when unset): the no-key `api/public/executeScript` GET endpoint is disabled (403). `false`: it's enabled, reachable via GET with no key, for every registered script regardless of its `GET:`/`POST:`/`ALL:` prefix. Intended for trusted internal networks or manual testing only. Either way, `api/key/executeScript` always requires a valid `X-Api-Key` header — this flag never weakens it. |
 | `jp.ecuacion.tool.command-api.api-key-file-path` | String | Path to a file containing the shared secret(s) compared against the `X-Api-Key` header on `api/key/executeScript` requests. One key per line; a request is accepted if it matches any line. Blank lines are skipped, and lines starting with `#` are treated as comments and skipped too — handy for labeling which key belongs to which caller. Supports `${ENV_VAR}` resolution, same as script paths. Optional — see below for the default when unset. |

 Example:

 ```properties
 jp.ecuacion.tool.command-api.api-key-required=true
 jp.ecuacion.tool.command-api.api-key-file-path=${HOME}/secrets/command-api-key.txt
 ```

 If `api-key-file-path` is left unconfigured, a file named `ecuacion-tool-command-api-key.txt` is looked for next to the war — checking `config/ecuacion-tool-command-api-key.txt` first, then `ecuacion-tool-command-api-key.txt` right next to the war — the same two of the three locations `ecuacion-tool-command-api.properties` itself supports (see further below). This is a convenient zero-config default for casual/local use; for production, prefer setting `api-key-file-path` explicitly to a path outside the deployment directory (e.g. a secrets volume, or a location with tighter file permissions), so the key isn't bundled, backed up, or overwritten alongside the app.

 Independently, each script registered in `ecuacion-tool-command-api.properties` can declare which HTTP method(s) `api/key/executeScript` accepts for it, via an optional, case-insensitive prefix on its path value:

 | Prefix | Methods allowed on `api/key/executeScript` |
 | --- | --- |
 | *(none)* | POST only (default — existing script definitions keep working unchanged) |
 | `POST:` | POST only (same as no prefix, just explicit) |
 | `GET:` | GET only |
 | `ALL:` | GET and POST |

 ```properties
 # POST only (default)
 script.deploy=/path/to/script/directory/deploy.sh

 # Also reachable via GET on api/key/executeScript — for read-only scripts
 script.say-hello=GET:/path/to/script/directory/sayHello.sh
 ```

 This is deliberately independent of `api-key-required`: it only ever governs the authenticated `api/key/executeScript` endpoint. The no-key `api/public/executeScript` endpoint, when enabled, stays GET-only and unconditionally reachable for every script — it is a consciously-accepted-risk convenience endpoint, not something that needs per-script tuning.

 The api-key file may contain more than one key, one per line (blank lines are ignored); a request is accepted if it presents any of them. This is useful when each caller is issued its own key — a single leaked or retired key can then be dropped by deleting its line, without having to rotate everyone else's. Example:

 ```text
 caller-a-s3cr3t-key
 caller-b-s3cr3t-key
 ```

 The file's content is read fresh on every request rather than cached, so keys can be added, removed, or rotated by editing the file's content without restarting the app.

 `ecuacion-tool-command-api.properties` is loaded the same way Spring Boot loads `application.properties` — it's merged in from any of these locations (highest priority first), instead of requiring a CLASSPATH directory:

 1. The path given by `-Dspring.config.location=...`
 1. `config/ecuacion-tool-command-api.properties`, in a `config` subdirectory next to the war
 1. `ecuacion-tool-command-api.properties`, right next to the war

 The same applies to `application.properties` itself, so app-level settings (e.g. server port) can live alongside the script registrations.
 
/*
 * Copyright © 2012 ecuacion.jp (info@ecuacion.jp)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.ecuacion.util.commandapi.web.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.EmbeddedVariableUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides the function to exescute specified script file.
 */
@RestController
public class CommandApiController {

  private static final String PROP_ALLOW_INSECURE_ACCESS =
      "jp.ecuacion.tool.command-api.allow-insecure-access";
  private static final String PROP_API_KEY_FILE_PATH =
      "jp.ecuacion.tool.command-api.api-key-file-path";

  private Environment env;
  private DetailLogger dtlLogger = new DetailLogger(this);
  private final boolean allowInsecureAccess;

  /**
   * Constructs a new instance.
   */
  public CommandApiController(Environment env) {
    this.env = env;

    if (!env.containsProperty(PROP_ALLOW_INSECURE_ACCESS)) {
      dtlLogger.warn("'" + PROP_ALLOW_INSECURE_ACCESS + "' is not configured. Falling back to "
          + "the secure default (false): GET access is disabled and POST requests require a "
          + "valid 'apiKey'. Set this property explicitly to silence this warning.");
    }

    this.allowInsecureAccess =
        env.getProperty(PROP_ALLOW_INSECURE_ACCESS, Boolean.class, false);
  }

  /**
   * Execute the script specified by the URL parameters.
   *
   * @param scriptId It's the key to the script file path defined
   *     in {@code ecuacion-tool-command-api.properties}.<br>
   *     Since it's unsecure for API to be able to execute any scripts,
   *     executable scripts from API must be pre-defined.
   * @param parameter parameter given to the script.
   *     multiple parameters are able to be passed as comma-separated values.<br>
   *     When you pass parameters like {@code parameter=param1,param2},
   *     then {@code script.sh param1 param2} (or {@code script.bat param1 param2} on Windows)
   *     will be executed.
   *     (parameters are splitted at "," and each csv element will be an parameter.)
   * @throws Exception Exception
   */
  @GetMapping("api/public/executeScript")
  public Map<String, String> executeCommandByGet(@RequestParam String scriptId,
      @RequestParam(required = false) String parameter) throws Exception {

    if (!allowInsecureAccess) {
      throwException(HttpStatus.FORBIDDEN, "GET access is disabled. Set '"
          + PROP_ALLOW_INSECURE_ACCESS + "=true' to allow it, or use POST with 'apiKey'.");
    }

    return executeCommand(scriptId, parameter);
  }

  /**
   * Execute the script specified by the request parameters.
   *
   * @param scriptId It's the key to the script file path defined
   *     in {@code ecuacion-tool-command-api.properties}.<br>
   *     Since it's unsecure for API to be able to execute any scripts,
   *     executable scripts from API must be pre-defined.
   * @param parameter parameter given to the script.
   *     multiple parameters are able to be passed as comma-separated values.<br>
   *     When you pass parameters like {@code parameter=param1,param2},
   *     then {@code script.sh param1 param2} (or {@code script.bat param1 param2} on Windows)
   *     will be executed.
   *     (parameters are splitted at "," and each csv element will be an parameter.)
   * @param apiKey the shared secret compared against the file specified by
   *     {@code jp.ecuacion.tool.command-api.api-key-file-path}.
   *     Required unless {@code jp.ecuacion.tool.command-api.allow-insecure-access=true}.
   * @throws Exception Exception
   */
  @PostMapping("api/public/executeScript")
  public Map<String, String> executeCommandByPost(@RequestParam String scriptId,
      @RequestParam(required = false) String parameter,
      @RequestParam(required = false) String apiKey) throws Exception {

    if (!allowInsecureAccess) {
      verifyApiKey(apiKey);
    }

    return executeCommand(scriptId, parameter);
  }

  private Map<String, String> executeCommand(String scriptId, String parameter)
      throws Exception {

    dtlLogger.info("===== executeScript started =====");

    // scriptId input validation
    if (!Pattern.compile("^[a-zA-Z0-9.\\-_]*$").matcher(scriptId).find()) {
      throwException(HttpStatus.BAD_REQUEST,
          "String scriptId (" + scriptId + ") should consists of alphanumerics, '.', '-' and '_'.");
    }

    // Obtain scriptFilePath from scriptId
    dtlLogger.info("scriptId      : " + scriptId);
    String scriptFilePath = env.getProperty(scriptId);
    if (scriptFilePath == null) {
      throwException(HttpStatus.BAD_REQUEST, "scriptId '" + scriptId + "' not found.");
    }

    Objects.requireNonNull(scriptFilePath);

    // scriptFilePath input validation
    if (!Pattern.compile("^[a-zA-Z0-9/.\\-_\\$\\{\\}]*$").matcher(scriptFilePath).find()) {
      throwException(HttpStatus.INTERNAL_SERVER_ERROR, "String script file path (" + scriptFilePath
          + ") should consists of alphanumerics, '.', '-', '_', '/', '$', '{', '}'.");
    }

    // Resolve environment variables
    scriptFilePath = resolveEnvironmentVariables(scriptFilePath);

    // Cause an error if scriptFilePath not found
    dtlLogger.info("scriptFilePath: " + scriptFilePath);
    File scriptFile = new File(scriptFilePath);
    if (!scriptFile.exists()) {
      throwException(HttpStatus.INTERNAL_SERVER_ERROR,
          "scriptFilePath '" + scriptFilePath + "' not found.");
    }

    // Cause an error if scriptFilePath is not executable
    if (!scriptFile.canExecute()) {
      throwException(HttpStatus.INTERNAL_SERVER_ERROR,
          "scriptId '" + scriptId + "' is not executable. Check file permission.");
    }

    // Obtain paramsString
    String paramsString = parameter == null ? "" : parameter.replaceAll(",", " ");
    dtlLogger
        .info("parameter(s)  : " + (paramsString.equals("") ? "(not specified)" : paramsString));

    // Execute script
    List<String> commandList = new ArrayList<>();
    if (isWindows()) {
      // Windows has no shebang mechanism, so the script is run via "cmd /c"
      // instead of being executed directly (e.g. script.bat).
      commandList.add("cmd.exe");
      commandList.add("/c");
    }
    commandList.add(scriptFile.getAbsolutePath());
    commandList.addAll(Arrays.asList(paramsString.split(" ")));

    Runtime runtime = Runtime.getRuntime();
    Process p = runtime.exec(commandList.toArray(new String[commandList.size()]));
    dtlLogger.info("command start : " + scriptFile.getAbsolutePath() + " " + paramsString);

    // Read the script's standard output and standard error, and log them.
    // Both streams are consumed concurrently (stderr on a separate thread)
    // before waitFor(), since reading them one after another can deadlock
    // the child process if the buffer of the not-yet-read stream fills up.
    AtomicReference<IOException> stderrException = new AtomicReference<>();
    Thread stderrThread = new Thread(() -> {
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(p.getErrorStream(), Charset.defaultCharset()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          dtlLogger.info("stderr        : " + line);
        }
      } catch (IOException e) {
        stderrException.set(e);
      }
    });
    stderrThread.start();

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        dtlLogger.info("stdout        : " + line);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    stderrThread.join();
    if (stderrException.get() != null) {
      throw new RuntimeException(stderrException.get());
    }

    // wait for the end of the process
    int rtn = p.waitFor();
    p.destroy();

    dtlLogger.info("command end   : return code: " + rtn);

    // Return "return code" in a json format
    return Map.of("returnCode", Integer.toString(rtn));
  }

  private boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase().contains("win");
  }

  /**
   * Searches ${XXX} format (not $XXX) and replaces it to the environment valuable value.
   *
   * @param string any string
   * @return string with environment variables resolved
   */
  private String resolveEnvironmentVariables(String string) {
    Function<String, String> func = (key) -> {
      return System.getenv(key);
    };
    try {
      return EmbeddedVariableUtil.getVariableReplacedString(string, "${", "}", func);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void throwException(HttpStatus status, String message) {
    throw new ResponseStatusException(status, message);
  }

  /**
   * Verifies the {@code apiKey} request parameter against the shared secret file specified by
   * {@code jp.ecuacion.tool.command-api.api-key-file-path}.
   *
   * <p>All failure causes (missing parameter, missing configuration, unreadable file, or a
   * mismatched value) return the same generic client-facing message so that a caller cannot
   * distinguish a server misconfiguration from a wrong key. Details are logged server-side
   * only.</p>
   *
   * @param apiKey the value supplied by the client, possibly {@code null}
   */
  private void verifyApiKey(@Nullable String apiKey) {
    if (apiKey == null || apiKey.isEmpty()) {
      dtlLogger.warn("apiKey parameter was not supplied on a POST request "
          + "while " + PROP_ALLOW_INSECURE_ACCESS + "=false.");
      throwException(HttpStatus.UNAUTHORIZED, "Invalid or missing apiKey.");
    }

    String apiKeyFilePath = env.getProperty(PROP_API_KEY_FILE_PATH);
    if (apiKeyFilePath == null || apiKeyFilePath.isEmpty()) {
      dtlLogger.warn("'" + PROP_API_KEY_FILE_PATH + "' is not configured "
          + "while " + PROP_ALLOW_INSECURE_ACCESS + "=false. All POST requests are rejected.");
      throwException(HttpStatus.UNAUTHORIZED, "Invalid or missing apiKey.");
    }

    String resolvedPath = resolveEnvironmentVariables(Objects.requireNonNull(apiKeyFilePath));
    String expectedApiKey;
    try {
      expectedApiKey = Files.readString(Path.of(resolvedPath), StandardCharsets.UTF_8).strip();
    } catch (IOException e) {
      dtlLogger.warn("Failed to read api-key file '" + resolvedPath + "': " + e.getMessage());
      throwException(HttpStatus.UNAUTHORIZED, "Invalid or missing apiKey.");
      return;
    }

    // MessageDigest.isEqual() is used instead of String.equals() to avoid a timing attack.
    boolean matches = MessageDigest.isEqual(
        Objects.requireNonNull(apiKey).getBytes(StandardCharsets.UTF_8),
        expectedApiKey.getBytes(StandardCharsets.UTF_8));

    if (!matches) {
      dtlLogger.warn("apiKey mismatch on POST request.");
      throwException(HttpStatus.UNAUTHORIZED, "Invalid or missing apiKey.");
    }
  }
}

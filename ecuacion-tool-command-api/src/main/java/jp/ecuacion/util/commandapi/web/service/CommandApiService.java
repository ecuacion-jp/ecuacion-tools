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
package jp.ecuacion.util.commandapi.web.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.EmbeddedVariableUtil;
import jp.ecuacion.lib.core.util.PropertiesFileUtil;
import jp.ecuacion.util.commandapi.web.config.CommandApiKeyFileLocator;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves a {@code scriptId} to its configured script file and executes it, enforcing the
 * access-control rules described on {@code CommandApiController}'s endpoints (the
 * {@code api-key-required} gate for the API-key-less GET endpoint, and each script's declared
 * {@code GET:} / {@code POST:} / {@code ALL:} allowed method).
 */
@Service
public class CommandApiService {

  private static final String PROP_API_KEY_REQUIRED =
      "jp.ecuacion.tool.command-api.api-key-required";

  private static final String PREFIX_GET = "GET:";
  private static final String PREFIX_POST = "POST:";
  private static final String PREFIX_ALL = "ALL:";

  /**
   * A substring unique to the {@link PropertySource} name Spring Boot assigns to a config file
   * loaded via {@code spring.config.name} (see {@code WebApplication}) — e.g. {@code "Config
   * resource 'class path resource [ecuacion-tool-command-api.properties]' via location
   * 'optional:classpath:/'"}. Used to resolve {@code scriptId} only from this dedicated file,
   * not from the full merged {@link Environment}
   * (which also includes {@code application.properties},
   * JVM system properties, and OS environment variables) — otherwise a client-supplied
   * {@code scriptId} could coincidentally match an unrelated property/env var (e.g. {@code HOME},
   * {@code AWS_SECRET_ACCESS_KEY}) and leak its value via the "not found" error response.
   */
  private static final String SCRIPT_PROPERTIES_SOURCE_NAME_MARKER =
      "[ecuacion-tool-command-api.properties]";

  /**
   * Which HTTP method(s) a script definition allows on {@code executeScript}, expressed by an
   * optional {@code GET:} / {@code POST:} / {@code ALL:} prefix on the script definition's value
   * (case-insensitive); no prefix means {@link #POST}. Enforced identically on both
   * {@code api/key/executeScript} and {@code api/public/executeScript} (once the latter is
   * enabled via {@code api-key-required=false}) — the two endpoints differ only in whether an
   * {@code X-Api-Key} is required, not in which methods a script accepts.
   */
  private enum AllowedHttpMethod {
    GET, POST, ALL;

    boolean accepts(HttpMethod method) {
      return switch (this) {
        case GET -> method.equals(HttpMethod.GET);
        case POST -> method.equals(HttpMethod.POST);
        case ALL -> true;
      };
    }
  }

  private record ScriptDefinition(String scriptFilePath, AllowedHttpMethod allowedMethod) {}

  private ConfigurableEnvironment env;
  private DetailLogger dtlLogger = new DetailLogger(this);
  private final boolean apiKeyRequired;

  /**
   * Constructs a new instance.
   *
   * @throws IllegalStateException if {@code ecuacion-tool-command-api.properties} is missing
   *     altogether (no scriptId could ever be resolved), or if {@code api-key-required} is true
   *     (explicitly or by default) while {@code api-key-file-path} is unset (no scriptId could
   *     ever be executed through either endpoint) — both states leave this module unable to do
   *     anything useful, so startup is failed fast rather than deferring to a per-request error.
   */
  public CommandApiService(ConfigurableEnvironment env) {
    this.env = env;

    if (env.getPropertySources().stream()
        .noneMatch(source -> source.getName().contains(SCRIPT_PROPERTIES_SOURCE_NAME_MARKER))) {
      String message =
          "ecuacion-tool-command-api.properties was not found. Without it no scriptId can ever "
              + "be resolved, so this module cannot execute any script. Place the file next to "
              + "the deployed jar/war (e.g. under ./config/) or add it to the classpath.";
      dtlLogger.error(message);
      throw new IllegalStateException(message);
    }

    if (!env.containsProperty(PROP_API_KEY_REQUIRED)) {
      dtlLogger.warn("'" + PROP_API_KEY_REQUIRED + "' is not configured. Falling back to "
          + "the secure default (true): the api/public/executeScript GET endpoint (no API key "
          + "needed) is disabled; use api/key/executeScript with a valid 'X-Api-Key' header "
          + "instead. Set this property explicitly to silence this warning.");
    }

    this.apiKeyRequired = env.getProperty(PROP_API_KEY_REQUIRED, Boolean.class, true);

    if (this.apiKeyRequired && CommandApiKeyFileLocator.resolve(env) == null) {
      String message =
          "'" + PROP_API_KEY_REQUIRED + "' is true (either explicitly set, or defaulted for not "
              + "being configured), which disables the API-key-less api/public/executeScript "
              + "endpoint. But '" + CommandApiKeyFileLocator.PROP_API_KEY_FILE_PATH + "' is not "
              + "configured, and no '" + CommandApiKeyFileLocator.DEFAULT_FILE_NAME + "' file was "
              + "found in ./config/ or next to the deployed war either, so no key presented to "
              + "api/key/executeScript can ever match and no script can ever be executed through "
              + "either endpoint. Set '" + CommandApiKeyFileLocator.PROP_API_KEY_FILE_PATH
              + "', place a '" + CommandApiKeyFileLocator.DEFAULT_FILE_NAME + "' file there, or "
              + "set '" + PROP_API_KEY_REQUIRED + "=false' to allow api/public/executeScript "
              + "instead.";
      dtlLogger.error(message);
      throw new IllegalStateException(message);
    }
  }

  /**
   * Executes the script specified by {@code scriptId}, without requiring an API key.
   *
   * <p>Only reachable when {@code jp.ecuacion.tool.command-api.api-key-required=false} (default
   *     {@code true}) — this exists purely as a manual-testing convenience (e.g. from a browser
   *     or a bare {@code curl}, without having to set a header), not for production use. Once
   *     enabled, which scripts accept {@code requestMethod} here follows the exact same
   *     {@code GET:} / {@code POST:} / {@code ALL:} declaration as {@link #executeScriptByKey}
   *     (see {@link AllowedHttpMethod}) — this endpoint differs from that one only in requiring
   *     no {@code X-Api-Key}. For production access, use {@link #executeScriptByKey} instead.</p>
   *
   * @param requestMethod the HTTP method the request arrived on ({@code GET} or {@code POST}),
   *     checked against the script's declared {@code GET:} / {@code POST:} / {@code ALL:} prefix
   *     in {@code ecuacion-tool-command-api.properties}; no prefix means {@code POST} only.
   * @param scriptId It's the key to the script file path defined
   *     in {@code ecuacion-tool-command-api.properties}.<br>
   *     Since it's unsecure for API to be able to execute any scripts,
   *     executable scripts from API must be pre-defined.
   * @param parameters parameters given to the script.
   *     multiple parameters are able to be passed as comma-separated values.<br>
   *     When you pass parameters like {@code parameters=param1,param2},
   *     then {@code script.sh param1 param2} (or {@code script.bat param1 param2} on Windows)
   *     will be executed.
   *     (parameters are splitted at "," and each csv element will be an parameter.)
   * @throws Exception Exception
   */
  public Map<String, String> executeScriptWithoutApiKey(HttpMethod requestMethod, String scriptId,
      @Nullable String parameters) throws Exception {

    if (apiKeyRequired) {
      throwException(HttpStatus.FORBIDDEN,
          "Access without an API key is disabled. Set '" + PROP_API_KEY_REQUIRED
              + "=false' to allow it, or use api/key/executeScript "
              + "with a valid 'X-Api-Key' header.");
    }

    return executeScript(requestMethod, scriptId, parameters);
  }

  /**
   * Executes the script specified by {@code scriptId}, on behalf of an API-key-authenticated
   * request.
   *
   * <p>Callers are expected to already sit behind ecuacion-splib-rest's
   *     {@code SplibApiKeyAuthenticationFilter} (checked against the application-registered
   *     {@code SplibApiKeyExpectedValueProvider} bean) — this method only enforces the script's
   *     own declared {@link AllowedHttpMethod} for {@code requestMethod} (see
   *     {@link AllowedHttpMethod}).</p>
   *
   * @param requestMethod the HTTP method the request arrived on ({@code GET} or {@code POST}),
   *     checked against the script's declared {@code GET:} / {@code POST:} / {@code ALL:} prefix
   *     in {@code ecuacion-tool-command-api.properties}; no prefix means {@code POST} only.
   * @param scriptId see {@link #executeScriptWithoutApiKey}
   * @param parameters see {@link #executeScriptWithoutApiKey}
   * @throws Exception Exception
   */
  public Map<String, String> executeScriptByKey(HttpMethod requestMethod, String scriptId,
      @Nullable String parameters) throws Exception {

    return executeScript(requestMethod, scriptId, parameters);
  }

  /**
   * Resolve the script by {@code scriptId} and execute it with the given parameters.
   *
   * @param requestMethod the HTTP method the request arrived on, checked against the script's
   *     declared {@link AllowedHttpMethod} (see {@link #resolveScriptDefinition}); the same rule
   *     applies regardless of whether the caller came in via {@link #executeScriptWithoutApiKey}
   *     or {@link #executeScriptByKey}.
   * @param scriptId see {@link #executeScriptByKey}
   * @param parameters see {@link #executeScriptByKey}
   * @throws Exception Exception
   */
  private Map<String, String> executeScript(HttpMethod requestMethod, String scriptId,
      @Nullable String parameters) throws Exception {

    dtlLogger.info("===== executeScript started =====");

    // scriptId input validation
    if (!Pattern.compile("^[a-zA-Z0-9.\\-_]*$").matcher(scriptId).matches()) {
      throwException(HttpStatus.BAD_REQUEST,
          "String scriptId (" + scriptId + ") should consists of alphanumerics, '.', '-' and '_'.");
    }

    // Obtain the script definition from scriptId
    dtlLogger.info("scriptId      : " + scriptId);
    ScriptDefinition scriptDefinition = resolveScriptDefinition(scriptId);
    if (scriptDefinition == null) {
      throwException(HttpStatus.BAD_REQUEST, "scriptId '" + scriptId + "' not found.");
    }

    Objects.requireNonNull(scriptDefinition);

    if (!scriptDefinition.allowedMethod().accepts(requestMethod)) {
      throwException(HttpStatus.FORBIDDEN,
          "scriptId '" + scriptId + "' does not allow " + requestMethod
              + " access. Add a matching 'GET:' / 'POST:' / 'ALL:' "
              + "prefix to its definition in ecuacion-tool-command-api.properties to allow it.");
    }

    String scriptFilePath = scriptDefinition.scriptFilePath();

    // scriptFilePath input validation
    if (!Pattern.compile("^[a-zA-Z0-9/.\\-_\\$\\{\\}]*$").matcher(scriptFilePath).matches()) {
      throwServerConfigError(
          "scriptId '" + scriptId + "': registered script file path (" + scriptFilePath
              + ") should consists of alphanumerics, '.', '-', '_', '/', '$', '{', '}'.",
          "scriptId '" + scriptId + "' has an invalid script file path registered. "
              + "See the server log for details.");
    }

    // Resolve environment variables
    scriptFilePath = resolveEnvironmentVariables(scriptId, scriptFilePath);

    // Cause an error if scriptFilePath not found
    dtlLogger.info("scriptFilePath: " + scriptFilePath);
    File scriptFile = new File(scriptFilePath);
    if (!scriptFile.exists()) {
      throwServerConfigError("scriptFilePath '" + scriptFilePath + "' not found. (scriptId '"
          + scriptId + "')", "scriptFilePath for scriptId '" + scriptId
              + "' was not found. See the server log for details.");
    }

    // Cause an error if scriptFilePath is not executable
    if (!scriptFile.canExecute()) {
      throwException(HttpStatus.INTERNAL_SERVER_ERROR,
          "scriptId '" + scriptId + "' is not executable. Check file permission.");
    }

    // Obtain paramsString
    String paramsString = parameters == null ? "" : parameters.replaceAll(",", " ");
    dtlLogger
        .info("parameter(s)  : " + (paramsString.equals("") ? "(not specified)" : paramsString));

    // paramsString input validation.
    // On Windows the script is run via "cmd.exe /c", which re-parses metacharacters
    // (e.g. '&', '|', '<', '>', '^', '%', quotes) within each argument of the command line,
    // allowing argument injection into cmd.exe itself. Restricting to a safe character
    // whitelist prevents that. The same restriction is applied on all platforms so behavior
    // does not depend on which OS the server happens to run on.
    if (!Pattern.compile("^[a-zA-Z0-9 ./:_=@\\-]*$").matcher(paramsString).matches()) {
      throwException(HttpStatus.BAD_REQUEST, "String parameters (" + paramsString
          + ") should consists of alphanumerics, ' ', '.', '/', ':', '=', '@', '-' and '_'.");
    }

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
    Process p;
    try {
      p = runtime.exec(commandList.toArray(new String[commandList.size()]));
    } catch (IOException e) {
      // scriptFile.canExecute() already passed, so this means the OS itself refused/failed to
      // start it (e.g. missing/invalid shebang interpreter, scriptFile is actually a directory,
      // or a permission check finer-grained than canExecute()'s) — a config/environment problem
      // on the server, not a client-caused error.
      throw serverConfigError(
          "Failed to start scriptId '" + scriptId + "' (" + scriptFile.getAbsolutePath() + "): "
              + e.getMessage(),
          "Failed to start scriptId '" + scriptId + "'. See the server log for details.");
    }
    dtlLogger.info("command start : " + scriptFile.getAbsolutePath() + " " + paramsString);

    // Read the script's standard output and standard error, logging them and collecting them
    // for the response. Both streams are consumed concurrently (stderr on a separate thread)
    // before waitFor(), since reading them one after another can deadlock
    // the child process if the buffer of the not-yet-read stream fills up.
    List<String> stderrLines = new ArrayList<>();
    AtomicReference<IOException> stderrException = new AtomicReference<>();
    Thread stderrThread = new Thread(() -> {
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(p.getErrorStream(), Charset.defaultCharset()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          dtlLogger.info("stderr        : " + line);
          stderrLines.add(line);
        }
      } catch (IOException e) {
        stderrException.set(e);
      }
    });
    stderrThread.start();

    List<String> stdoutLines = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        dtlLogger.info("stdout        : " + line);
        stdoutLines.add(line);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // stderrThread only ever writes stderrLines, and always completes (join() below) before
    // stderrLines is read after this point, so no further synchronization is needed.
    stderrThread.join();
    if (stderrException.get() != null) {
      throw new RuntimeException(stderrException.get());
    }

    // wait for the end of the process
    int rtn = p.waitFor();
    p.destroy();

    dtlLogger.info("command end   : return code: " + rtn);

    // Return the return code plus the script's captured output in a json format.
    return Map.of(
        "returnCode", Integer.toString(rtn),
        "stdout", String.join(System.lineSeparator(), stdoutLines),
        "stderr", String.join(System.lineSeparator(), stderrLines));
  }

  @SuppressWarnings("null")
  private boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
  }

  /**
   * Resolves {@code scriptId} to a {@link ScriptDefinition}, consulting only the
   * {@link PropertySource}s backed by {@code ecuacion-tool-command-api.properties} (see
   * {@link #SCRIPT_PROPERTIES_SOURCE_NAME_MARKER}) rather than the full merged
   * {@link org.springframework.core.env.Environment}.
   *
   * <p>Iterates {@code env.getPropertySources()} in priority order so that, if the file is
   * present at more than one of Spring Boot's search locations (e.g. bundled on the classpath
   * and also dropped in {@code ./config/} next to the deployed jar/war for an ops override),
   * the higher-priority one wins — matching how {@code spring.config.name} multi-location
   * resolution already behaves for every other property.</p>
   *
   * @param scriptId the client-supplied script identifier
   * @return the configured script definition, or {@code null} if no matching, dedicated
   *     property source defines {@code scriptId}
   */
  private @Nullable ScriptDefinition resolveScriptDefinition(String scriptId) {
    for (PropertySource<?> source : env.getPropertySources()) {
      if (!source.getName().contains(SCRIPT_PROPERTIES_SOURCE_NAME_MARKER)) {
        continue;
      }

      Object value = source.getProperty(scriptId);
      if (value != null) {
        return parseScriptDefinition(value.toString());
      }
    }

    return null;
  }

  /**
   * Splits a script definition's raw property value into its script file path and its
   * {@link AllowedHttpMethod}, based on an optional, case-insensitive {@code GET:} / {@code
   * POST:} / {@code ALL:} literal prefix; no recognized prefix means the whole value is the path
   * and {@link AllowedHttpMethod#POST}. Case-insensitivity is done by upper-casing (with
   * {@link Locale#ROOT}, so behavior does not depend on the server's default locale) the same
   * number of leading characters as the prefix and comparing that against the prefix itself,
   * rather than by splitting on the first colon — so it can never misfire on a script path that
   * itself starts with a colon (e.g. a Windows drive letter like {@code C:\...}): no such path's
   * upper-cased leading characters equal {@code "GET:"}, {@code "POST:"}, or {@code "ALL:"}.
   */
  private ScriptDefinition parseScriptDefinition(String rawValue) {
    if (hasPrefix(rawValue, PREFIX_GET)) {
      return new ScriptDefinition(rawValue.substring(PREFIX_GET.length()), AllowedHttpMethod.GET);

    } else if (hasPrefix(rawValue, PREFIX_POST)) {
      return new ScriptDefinition(rawValue.substring(PREFIX_POST.length()),
          AllowedHttpMethod.POST);

    } else if (hasPrefix(rawValue, PREFIX_ALL)) {
      return new ScriptDefinition(rawValue.substring(PREFIX_ALL.length()), AllowedHttpMethod.ALL);

    } else {
      return new ScriptDefinition(rawValue, AllowedHttpMethod.POST);
    }
  }

  /**
   * Checks whether {@code rawValue} starts with {@code prefix}, case-insensitively.
   *
   * @param prefix an upper-case literal, e.g. {@link #PREFIX_GET}
   * @return {@code true} if {@code rawValue}'s leading {@code prefix.length()} characters,
   *     upper-cased, equal {@code prefix}
   */
  private boolean hasPrefix(String rawValue, String prefix) {
    if (rawValue.length() < prefix.length()) {
      return false;
    }

    return rawValue.substring(0, prefix.length()).toUpperCase(Locale.ROOT).equals(prefix);
  }

  /**
   * Searches ${XXX} format (not $XXX) and replaces it to the environment valuable value.
   *
   * @param scriptId the scriptId {@code string} was configured under, used only to identify
   *     which script definition is misconfigured if resolution fails
   * @param string any string
   * @return string with environment variables resolved
   */
  private String resolveEnvironmentVariables(String scriptId, String string) {
    Function<String, String> func = (key) -> {
      return System.getenv(key);
    };
    try {
      return EmbeddedVariableUtil.getVariableReplacedString(string, "${", "}", func);
    } catch (ViolationException e) {
      // Thrown for a malformed "${...}" (unmatched braces) or a referenced environment
      // variable that isn't set — both are a misconfigured script.<id> entry in
      // ecuacion-tool-command-api.properties, not a client-caused error.
      throw serverConfigError(
          "Failed to resolve environment variable(s) for scriptId '" + scriptId + "' ("
              + string + "): " + describeViolations(e),
          "Failed to resolve environment variable(s) for scriptId '" + scriptId
              + "'. See the server log for details.");
    }
  }

  /**
   * Throws a {@link ResponseStatusException} carrying {@code message}.
   *
   * <p>Logging this happens centrally in {@code SplibRestExceptionHandler.
   * handleExceptionInternal} (in {@code ecuacion-splib-rest}), not here — every
   * {@code ResponseStatusException} reaching that handler is logged there (4xx at WARN, other
   * statuses at ERROR), regardless of which application/service threw it.</p>
   *
   * @param status the HTTP status to respond with
   * @param message the detail message set on the thrown exception
   */
  private void throwException(HttpStatus status, String message) {
    throw newResponseStatusException(status, message);
  }

  /**
   * Builds (without throwing) a {@link ResponseStatusException} carrying {@code message}.
   *
   * <p>Used instead of {@link #throwException} at call sites inside a non-{@code void} method
   * where the compiler's definite-return check requires a {@code return} statement — writing
   * {@code throw newResponseStatusException(...)} satisfies that check without the enclosing
   * method having to fake a return value.</p>
   *
   * @param status the HTTP status to respond with
   * @param message the detail message set on the returned exception
   */
  private ResponseStatusException newResponseStatusException(HttpStatus status, String message) {
    return new ResponseStatusException(status, message);
  }

  /**
   * Builds (without throwing) a {@link ResponseStatusException} reporting a server-side
   * config/environment problem (always {@link HttpStatus#INTERNAL_SERVER_ERROR}), for a failure
   * whose full detail (e.g. a resolved absolute file path, or which environment variable is
   * missing) must not reach the client — such detail would leak server-side filesystem layout to
   * whoever holds a valid API key (or, when {@code api-key-required=false}, to anyone).
   *
   * <p>{@code logDetail} is logged here directly (at ERROR) so an operator can still diagnose the
   * failure from the server log; {@code clientMessage} is what the API caller actually receives,
   * and must not itself contain any such detail.</p>
   *
   * @param logDetail the full failure detail, logged server-side only
   * @param clientMessage the detail message set on the returned exception, safe to expose to the
   *     API caller
   */
  private ResponseStatusException serverConfigError(String logDetail, String clientMessage) {
    dtlLogger.error(logDetail);
    return newResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, clientMessage);
  }

  /**
   * Throws the {@link ResponseStatusException} {@link #serverConfigError} builds. See that
   * method's javadoc.
   *
   * @param logDetail see {@link #serverConfigError}
   * @param clientMessage see {@link #serverConfigError}
   */
  private void throwServerConfigError(String logDetail, String clientMessage) {
    throw serverConfigError(logDetail, clientMessage);
  }

  /**
   * Renders a {@link ViolationException}'s violations as a single human-readable string, joining
   * multiple violations (rare in practice) with {@code "; "}.
   */
  private String describeViolations(ViolationException e) {
    return e.getViolations().getBusinessViolations().stream()
        .map(v -> PropertiesFileUtil.getMessage(Locale.ROOT, v.getMessageId(), v.getMessageArgs()))
        .collect(Collectors.joining("; "));
  }
}

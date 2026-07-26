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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.EmbeddedVariableUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpMethod;
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
   * Which HTTP method(s) a script definition allows on {@code api/key/executeScript}, expressed
   * by an optional {@code GET:} / {@code POST:} / {@code ALL:} prefix on the script definition's
   * value (case-insensitive); no prefix means {@link #POST}. Never consulted for
   * {@code api/public/executeScript}, which — once enabled by {@code api-key-required=false} — is
   * always reachable via GET regardless of a script's declared method, since it is a
   * consciously-accepted-risk convenience endpoint covering every registered script alike.
   */
  private enum AllowedHttpMethod {
    GET, POST, ALL;

    boolean accepts(HttpMethod method) {
      return switch (this) {
        case GET -> method == HttpMethod.GET;
        case POST -> method == HttpMethod.POST;
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
   */
  public CommandApiController(ConfigurableEnvironment env) {
    this.env = env;

    if (!env.containsProperty(PROP_API_KEY_REQUIRED)) {
      dtlLogger.warn("'" + PROP_API_KEY_REQUIRED + "' is not configured. Falling back to "
          + "the secure default (true): the api/public/executeScript GET endpoint (no API key "
          + "needed) is disabled; use api/key/executeScript with a valid 'X-Api-Key' header "
          + "instead. Set this property explicitly to silence this warning.");
    }

    this.apiKeyRequired = env.getProperty(PROP_API_KEY_REQUIRED, Boolean.class, true);
  }

  /**
   * Execute the script specified by the URL parameters, without requiring an API key.
   *
   * <p>Only reachable when {@code jp.ecuacion.tool.command-api.api-key-required=false} (default
   *     {@code true}) — this exists purely as a manual-testing convenience (e.g. from a browser
   *     or a bare {@code curl}, without having to set a header), not for production use. Every
   *     registered script is reachable via GET here regardless of its declared allowed method,
   *     since enabling this endpoint at all is already an explicit, deployment-wide acceptance of
   *     that risk. For programmatic / production access, use {@link #executeCommandByKeyGet} or
   *     {@link #executeCommandByKeyPost} on {@code api/key/executeScript} instead.</p>
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

    if (apiKeyRequired) {
      throwException(HttpStatus.FORBIDDEN,
          "GET access is disabled. Set '" + PROP_API_KEY_REQUIRED
              + "=false' to allow it, or use api/key/executeScript "
              + "with a valid 'X-Api-Key' header.");
    }

    return executeCommand(null, scriptId, parameter);
  }

  /**
   * Execute the script specified by the request parameters, via GET.
   *
   * <p>Mapped under {@code api/key/**}, so ecuacion-splib-rest's
   *     {@code SplibApiKeyAuthenticationFilter} requires a valid {@code X-Api-Key} header
   *     (checked against the application-registered
   *     {@code SplibApiKeyExpectedValueProvider} bean) before this method is ever invoked. On top
   *     of that, the script's own definition must declare itself GET-reachable via a
   *     {@code GET:} or {@code ALL:} prefix in {@code ecuacion-tool-command-api.properties} (see
   *     {@link AllowedHttpMethod}); scripts with no prefix, or an explicit {@code POST:} prefix,
   *     reject GET here even with a valid key.</p>
   *
   * @param scriptId see {@link #executeCommandByGet}
   * @param parameter see {@link #executeCommandByGet}
   * @throws Exception Exception
   */
  @GetMapping("api/key/executeScript")
  public Map<String, String> executeCommandByKeyGet(@RequestParam String scriptId,
      @RequestParam(required = false) String parameter) throws Exception {

    return executeCommand(HttpMethod.GET, scriptId, parameter);
  }

  /**
   * Execute the script specified by the request parameters, via POST.
   *
   * <p>Mapped under {@code api/key/**}, so ecuacion-splib-rest's
   *     {@code SplibApiKeyAuthenticationFilter} requires a valid {@code X-Api-Key} header
   *     (checked against the application-registered
   *     {@code SplibApiKeyExpectedValueProvider} bean) before this method is ever invoked. POST
   *     is accepted for any script whose definition has no prefix, or an explicit {@code POST:}
   *     or {@code ALL:} prefix (see {@link AllowedHttpMethod}) — i.e. every script except ones
   *     narrowed to {@code GET:} only.</p>
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
  @PostMapping("api/key/executeScript")
  public Map<String, String> executeCommandByKeyPost(@RequestParam String scriptId,
      @RequestParam(required = false) String parameter) throws Exception {

    return executeCommand(HttpMethod.POST, scriptId, parameter);
  }

  /**
   * Resolve the script by {@code scriptId} and execute it with the given parameter.
   *
   * @param requestMethod the HTTP method the request arrived on, used to enforce a script's
   *     declared {@link AllowedHttpMethod}; {@code null} skips that check entirely (used by
   *     {@link #executeCommandByGet}, which is unconditionally GET-reachable for every script
   *     once enabled — see its javadoc).
   * @param scriptId see {@link #executeCommandByKeyPost}
   * @param parameter see {@link #executeCommandByKeyPost}
   * @throws Exception Exception
   */
  private Map<String, String> executeCommand(@Nullable HttpMethod requestMethod, String scriptId,
      String parameter) throws Exception {

    dtlLogger.info("===== executeScript started =====");

    // scriptId input validation
    if (!Pattern.compile("^[a-zA-Z0-9.\\-_]*$").matcher(scriptId).find()) {
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

    if (requestMethod != null && !scriptDefinition.allowedMethod().accepts(requestMethod)) {
      throwException(HttpStatus.FORBIDDEN,
          "scriptId '" + scriptId + "' does not allow " + requestMethod
              + " access on api/key/executeScript. Add a matching 'GET:' / 'POST:' / 'ALL:' "
              + "prefix to its definition in ecuacion-tool-command-api.properties to allow it.");
    }

    String scriptFilePath = scriptDefinition.scriptFilePath();

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

    // paramsString input validation.
    // On Windows the script is run via "cmd.exe /c", which re-parses metacharacters
    // (e.g. '&', '|', '<', '>', '^', '%', quotes) within each argument of the command line,
    // allowing argument injection into cmd.exe itself. Restricting to a safe character
    // whitelist prevents that. The same restriction is applied on all platforms so behavior
    // does not depend on which OS the server happens to run on.
    if (!Pattern.compile("^[a-zA-Z0-9 ./:_=@\\-]*$").matcher(paramsString).find()) {
      throwException(HttpStatus.BAD_REQUEST, "String parameter (" + paramsString
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
    Process p = runtime.exec(commandList.toArray(new String[commandList.size()]));
    dtlLogger.info("command start : " + scriptFile.getAbsolutePath() + " " + paramsString);

    // Read the script's standard output and standard error, and log them.
    // Both streams are consumed concurrently (stderr on a separate thread)
    // before waitFor(), since reading them one after another can deadlock
    // the child process if the buffer of the not-yet-read stream fills up.
    AtomicReference<IOException> stderrException = new AtomicReference<>();
    Thread stderrThread = new Thread(() -> {
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(p.getErrorStream(), Charset.defaultCharset()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          dtlLogger.info("stderr        : " + line);
        }
      } catch (IOException e) {
        stderrException.set(e);
      }
    });
    stderrThread.start();

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
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
}

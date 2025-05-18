package jp.ecuacion.util.commandapi.web.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import jp.ecuacion.lib.core.logging.DetailLogger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides the function to exescute specified script file.
 */
@RestController
public class CommandApiController {

  private static final String PROPERTIES_FILE = "ecuacion-tool-command-api.properties";
  private DetailLogger dtlLogger = new DetailLogger(this);

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
   *     then {@code script.sh param1 param2} will be executed. 
   *     (parameters are splitted at "," and each csv element will be an parameter.)
   * @throws Exception Exception
   */
  @GetMapping("api/public/executeScript")
  public Map<String, String> executeCommand(@RequestParam String scriptId,
      @RequestParam(required = false) String parameter) throws Exception {

    dtlLogger.info("===== executeScript started =====");

    // Obtain ecuacion-tool-command-api.properties from classpath
    InputStream inClassPathResource = ClassLoader.getSystemResourceAsStream(PROPERTIES_FILE);
    if (inClassPathResource == null) {
      throwException("'" + PROPERTIES_FILE + "' not found on classpath.");
    }

    Properties inClassPathProperties = new Properties();
    try {
      inClassPathProperties.load(inClassPathResource);

    } catch (IOException e) {
      throwException("IOException occurred while reading '" + PROPERTIES_FILE + "'.");
    }

    // scriptId input validation
    if (!Pattern.compile("^[a-zA-Z0-9.\\-_]*$").matcher(scriptId).find()) {
      throwException("String scriptId (" + scriptId
          + ") should consists of alphanumerics, '.', '-' and '_'.");
    }

    // Obtain scriptFilePath from scriptId
    dtlLogger.info("scriptId      : " + scriptId);
    String scriptFilePath = inClassPathProperties.getProperty(scriptId);
    if (scriptFilePath == null) {
      throwException("scriptId '" + scriptId + "' not found.");
    }

    // scriptFilePath input validation
    if (!Pattern.compile("^[a-zA-Z0-9/.\\-_\\$\\{\\}]*$").matcher(scriptFilePath).find()) {
      throwException("String script file path (" + scriptFilePath
          + ") should consists of alphanumerics, '.', '-', '_', '/', '$', '{', '}'.");
    }

    // Resolve environment variables
    scriptFilePath = resolveEnvironmentVariables(scriptFilePath);

    // Cause an error if scriptFilePath not found
    dtlLogger.info("scriptFilePath: " + scriptFilePath);
    File scriptFile = new File(scriptFilePath);
    if (!scriptFile.exists()) {
      throwException("scriptFilePath '" + scriptFilePath + "' not found.");
    }

    // Obtain paramsString
    String paramsString = parameter == null ? "" : parameter.replaceAll(",", " ");
    dtlLogger
        .info("parameter     : " + (paramsString.equals("") ? "(not specified)" : paramsString));

    // Execute script
    List<String> commandList = new ArrayList<>();
    commandList.add(scriptFile.getAbsolutePath());
    commandList.addAll(Arrays.asList(paramsString.split(" ")));

    Runtime runtime = Runtime.getRuntime();
    Process p = runtime.exec(commandList.toArray(new String[commandList.size()]));
    dtlLogger.info("command start : " + scriptFile.getAbsolutePath() + " " + paramsString);

    // wait for the end of the process
    int rtn = p.waitFor();
    p.destroy();

    dtlLogger.info("command end   : return code: " + rtn);

    // Return "return code" in a json format
    return Map.of("returnCode", Integer.toString(rtn));
  }

  /**
   * Searches ${XXX} format (not $XXX) and replaces it to the environment valuable value.
   * 
   * @param string any string
   * @return string with environment variables resolved
   */
  private String resolveEnvironmentVariables(String string) {
    String rtnStr = string;
    while (true) {
      if (!rtnStr.contains("${")) {
        return rtnStr;
      }

      int startIndex = rtnStr.indexOf("${");
      int endIndex = rtnStr.indexOf("}");

      if (startIndex > endIndex) {
        throwException("\"}\" shows up before \"${\" does, or \"}\" not exist. (string: " + string);
      }

      String valuableName = rtnStr.substring(0, endIndex).substring(startIndex + 2);
      String valuableWithBracket = "${" + valuableName + "}";
      String value = System.getenv(valuableName);
      if (value == null) {
        throwException("Environment variable '" + valuableName + "' is not defined in the server.");
      }

      rtnStr = rtnStr.replace(valuableWithBracket, value);
    }
  }

  private void throwException(String message) {
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }
}

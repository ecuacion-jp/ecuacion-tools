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

import java.util.Map;
import jp.ecuacion.util.commandapi.web.service.CommandApiService;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides the function to exescute specified script file.
 */
@RestController
public class CommandApiController {

  private final CommandApiService commandApiService;

  /**
   * Constructs a new instance.
   */
  public CommandApiController(CommandApiService commandApiService) {
    this.commandApiService = commandApiService;
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

    return commandApiService.executeScriptWithoutApiKey(scriptId, parameter);
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
   *     {@code CommandApiService}); scripts with no prefix, or an explicit {@code POST:} prefix,
   *     reject GET here even with a valid key.</p>
   *
   * @param scriptId see {@link #executeCommandByGet}
   * @param parameter see {@link #executeCommandByGet}
   * @throws Exception Exception
   */
  @GetMapping("api/key/executeScript")
  public Map<String, String> executeCommandByKeyGet(@RequestParam String scriptId,
      @RequestParam(required = false) String parameter) throws Exception {

    return commandApiService.executeScriptByKey(HttpMethod.GET, scriptId, parameter);
  }

  /**
   * Execute the script specified by the request parameters, via POST.
   *
   * <p>Mapped under {@code api/key/**}, so ecuacion-splib-rest's
   *     {@code SplibApiKeyAuthenticationFilter} requires a valid {@code X-Api-Key} header
   *     (checked against the application-registered
   *     {@code SplibApiKeyExpectedValueProvider} bean) before this method is ever invoked. POST
   *     is accepted for any script whose definition has no prefix, or an explicit {@code POST:}
   *     or {@code ALL:} prefix (see {@code CommandApiService}) — i.e. every script except ones
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

    return commandApiService.executeScriptByKey(HttpMethod.POST, scriptId, parameter);
  }
}

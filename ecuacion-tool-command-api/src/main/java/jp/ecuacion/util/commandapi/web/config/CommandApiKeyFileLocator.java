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
package jp.ecuacion.util.commandapi.web.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import jp.ecuacion.lib.core.util.EmbeddedVariableUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;

/**
 * Resolves the api-key file path shared by {@link CommandApiKeyProvider} (which reads the file
 * per request) and {@code CommandApiService} (which only needs to know whether one can be
 * resolved at all, as part of its startup fail-fast check).
 *
 * <p>When {@code jp.ecuacion.tool.command-api.api-key-file-path} is set, its value (with
 *     {@code ${ENV_VAR}} resolved) is returned as-is, even if no file exists there yet —
 *     existence is validated lazily, per request, by {@link CommandApiKeyProvider}. When unset,
 *     the conventional locations next to the deployed war are checked, mirroring where
 *     {@code ecuacion-tool-command-api-scripts.properties} itself is searched for, and the
 *     first one that actually exists is returned.</p>
 */
public final class CommandApiKeyFileLocator {

  public static final String PROP_API_KEY_FILE_PATH =
      "jp.ecuacion.tool.command-api.api-key-file-path";

  public static final String DEFAULT_FILE_NAME = "ecuacion-tool-command-api-keys.txt";

  private CommandApiKeyFileLocator() {}

  /**
   * Returns the api-key file path to use, or {@code null} if the property is unset and no
   * conventional default file exists either.
   */
  public static @Nullable Path resolve(Environment env) {
    String configured = env.getProperty(PROP_API_KEY_FILE_PATH);
    if (configured != null && !configured.isBlank()) {
      return Path.of(resolveEnvironmentVariables(configured));
    }

    Path baseDir = Path.of(System.getProperty("user.dir"));
    List<Path> defaultCandidates = List.of(baseDir.resolve("config").resolve(DEFAULT_FILE_NAME),
        baseDir.resolve(DEFAULT_FILE_NAME));

    return defaultCandidates.stream().filter(Files::exists).findFirst().orElse(null);
  }

  /**
   * Searches ${XXX} format (not $XXX) and replaces it to the environment variable value.
   *
   * @param string any string
   * @return string with environment variables resolved
   */
  private static String resolveEnvironmentVariables(String string) {
    Function<String, String> func = System::getenv;
    try {
      return EmbeddedVariableUtil.getVariableReplacedString(string, "${", "}", func);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

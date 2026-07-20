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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.EmbeddedVariableUtil;
import jp.ecuacion.splib.rest.apikey.SplibApiKeyExpectedValueProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Supplies the expected API-key value read from the file specified by
 * {@code jp.ecuacion.tool.command-api.api-key-file-path}, backing {@code api/key/executeScript}
 * authentication (see {@link jp.ecuacion.splib.rest.config.SplibRestSecurityConfig}).
 *
 * <p>Only a single, application-wide key is supported: {@code apiKeyId} is ignored, and the
 *     same expected value is returned regardless of which key was presented.</p>
 */
@Component
public class CommandApiKeyProvider implements SplibApiKeyExpectedValueProvider {

  private static final String PROP_API_KEY_FILE_PATH =
      "jp.ecuacion.tool.command-api.api-key-file-path";

  private final Environment env;
  private final DetailLogger dtlLogger = new DetailLogger(this);

  /**
   * Constructs a new instance.
   */
  public CommandApiKeyProvider(Environment env) {
    this.env = env;
  }

  /**
   * Reads and returns the current contents of the api-key file on every call, rather than
   * caching it, so that rotating the key only requires replacing the file's contents — no
   * application restart needed.
   */
  @Override
  public @Nullable String getExpectedValue(@Nullable String apiKeyId, String presentedApiKey) {
    String apiKeyFilePath = env.getProperty(PROP_API_KEY_FILE_PATH);
    if (apiKeyFilePath == null || apiKeyFilePath.isEmpty()) {
      dtlLogger.warn("'" + PROP_API_KEY_FILE_PATH + "' is not configured. "
          + "All api/key/executeScript requests are rejected.");
      return null;
    }

    String resolvedPath = resolveEnvironmentVariables(apiKeyFilePath);
    try {
      return Files.readString(Path.of(resolvedPath), StandardCharsets.UTF_8).strip();
    } catch (IOException e) {
      dtlLogger.warn("Failed to read api-key file '" + resolvedPath + "': " + e.getMessage());
      return null;
    }
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
}

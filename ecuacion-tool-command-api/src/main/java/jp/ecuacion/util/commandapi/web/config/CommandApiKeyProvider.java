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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.splib.rest.apikey.SplibApiKeyComparisonMode;
import jp.ecuacion.splib.rest.apikey.SplibApiKeyExpectedValue;
import jp.ecuacion.splib.rest.apikey.SplibApiKeyExpectedValueProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Supplies the expected API-key values read from the file resolved by
 * {@link CommandApiKeyFileLocator}, backing {@code api/key/execute} authentication
 * (see {@link jp.ecuacion.splib.rest.config.SplibRestSecurityConfig}).
 *
 * <p>The file may contain more than one key, one per line, so that individual keys (e.g. one
 *     issued per caller) can be revoked by deleting their line without invalidating the others.
 *     {@code apiKeyId} is ignored: any key present in the file is accepted regardless of which
 *     one was presented.</p>
 *
 * <p>Blank lines are skipped, and lines whose stripped content starts with {@code #} are treated
 *     as comments and skipped as well — handy for labeling which key belongs to which caller
 *     (e.g. {@code # key for company A}) so the right line can be found and deleted when a key
 *     needs revoking.</p>
 *
 * <p>Every key in the file is compared the same way, controlled by
 *     {@value #PROP_API_KEY_COMPARISON_MODE} (default {@link SplibApiKeyComparisonMode#BCRYPT}):
 *     either every line is a plain-text key, or every line is a bcrypt hash. Mixing the two
 *     within one file is not supported — the mode applies to the whole file, not per line.</p>
 */
@Component
public class CommandApiKeyProvider implements SplibApiKeyExpectedValueProvider {

  /**
   * Selects how every key in the api-key file is compared: {@code PLAIN} or {@code BCRYPT} (see
   * {@link SplibApiKeyComparisonMode}). Defaults to {@code BCRYPT} when unset. An unrecognized
   * value throws a {@link RuntimeException} at read time.
   */
  public static final String PROP_API_KEY_COMPARISON_MODE =
      "jp.ecuacion.tool.command-api.api-key-comparison-mode";

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
   * caching it, so that rotating keys only requires replacing the file's contents — no
   * application restart needed.
   */
  @SuppressWarnings("null")
  @Override
  public @Nullable Collection<SplibApiKeyExpectedValue> getExpectedValues(
      @Nullable String apiKeyId, String presentedApiKey) {
    Path resolvedPath = CommandApiKeyFileLocator.resolve(env);
    if (resolvedPath == null) {
      dtlLogger.warn("'" + CommandApiKeyFileLocator.PROP_API_KEY_FILE_PATH + "' is not "
          + "configured, and no '" + CommandApiKeyFileLocator.DEFAULT_FILE_NAME + "' file was "
          + "found in ./config/ or next to the deployed war. All api/key/execute requests "
          + "are rejected.");
      return null;
    }

    List<String> keys;
    try {
      keys = Files.readAllLines(resolvedPath, StandardCharsets.UTF_8);
    } catch (IOException e) {
      dtlLogger.warn("Failed to read api-key file '" + resolvedPath + "': " + e.getMessage());
      return null;
    }

    SplibApiKeyComparisonMode mode = resolveComparisonMode();

    return keys.stream().map(String::strip)
        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
        .map(key -> new SplibApiKeyExpectedValue(key, mode)).toList();
  }

  /**
   * Resolves {@value #PROP_API_KEY_COMPARISON_MODE} to the {@link SplibApiKeyComparisonMode}
   * applied to every key in the file, defaulting to {@code BCRYPT} when unset.
   *
   * @throws RuntimeException if the property is set to a value other than {@code PLAIN} or
   *     {@code BCRYPT}
   */
  private SplibApiKeyComparisonMode resolveComparisonMode() {
    String rawValue = env.getProperty(PROP_API_KEY_COMPARISON_MODE);
    if (rawValue == null || rawValue.isBlank()) {
      return SplibApiKeyComparisonMode.BCRYPT;
    }

    try {
      return SplibApiKeyComparisonMode.valueOf(rawValue.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("'" + PROP_API_KEY_COMPARISON_MODE + "' has an unrecognized "
          + "value '" + rawValue + "'. Valid values: PLAIN, BCRYPT.");
    }
  }
}

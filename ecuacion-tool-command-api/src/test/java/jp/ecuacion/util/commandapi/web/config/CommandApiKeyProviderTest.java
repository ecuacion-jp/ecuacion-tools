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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Plain (no Spring context) unit tests for {@link CommandApiKeyProvider}. It only needs an
 * {@code Environment}, so {@link MockEnvironment} is enough to exercise its branches directly,
 * without going through {@code CommandApiControllerTest}'s full MockMvc/api-key-header round
 * trip (which already covers the "valid file" / "multiple keys" / "missing file" cases as an
 * integration behavior, but not the property-unset/empty or blank-file-content edge cases below).
 */
class CommandApiKeyProviderTest {

  private static final String PROP_API_KEY_FILE_PATH =
      "jp.ecuacion.tool.command-api.api-key-file-path";

  private static Path createApiKeyFile(String content) {
    try {
      Path dir = Files.createTempDirectory("command-api-key-provider-test");
      Path file = dir.resolve("api-key.txt");
      Files.writeString(file, content);
      return file;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  void propertyNotConfiguredReturnsNull() {
    CommandApiKeyProvider provider = new CommandApiKeyProvider(new MockEnvironment());

    assertNull(provider.getExpectedValues(null, "any-key"));
  }

  @Test
  void propertyEmptyReturnsNull() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty(PROP_API_KEY_FILE_PATH, "");
    CommandApiKeyProvider provider = new CommandApiKeyProvider(env);

    assertNull(provider.getExpectedValues(null, "any-key"));
  }

  @Test
  void fileMissingReturnsNull() throws IOException {
    Path missing =
        Files.createTempDirectory("command-api-key-provider-test-missing").resolve("absent.txt");
    MockEnvironment env = new MockEnvironment();
    env.setProperty(PROP_API_KEY_FILE_PATH, missing.toString());
    CommandApiKeyProvider provider = new CommandApiKeyProvider(env);

    assertNull(provider.getExpectedValues(null, "any-key"));
  }

  @Test
  void validFileReturnsTrimmedNonBlankLinesOnly() {
    Path file = createApiKeyFile("\n  key-one  \n\nkey-two\n");
    MockEnvironment env = new MockEnvironment();
    env.setProperty(PROP_API_KEY_FILE_PATH, file.toString());
    CommandApiKeyProvider provider = new CommandApiKeyProvider(env);

    Collection<String> keys = provider.getExpectedValues(null, "any-key");

    assertEquals(List.of("key-one", "key-two"), keys);
  }

  @Test
  void blankFileReturnsEmptyList() {
    Path file = createApiKeyFile("\n\n  \n");
    MockEnvironment env = new MockEnvironment();
    env.setProperty(PROP_API_KEY_FILE_PATH, file.toString());
    CommandApiKeyProvider provider = new CommandApiKeyProvider(env);

    Collection<String> keys = provider.getExpectedValues(null, "any-key");

    assertEquals(List.of(), keys);
  }

  @Test
  void unresolvableEnvironmentVariableInFilePathThrows() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty(PROP_API_KEY_FILE_PATH,
        "${THIS_ENV_VAR_SHOULD_NOT_EXIST_XYZ123}/api-key.txt");
    CommandApiKeyProvider provider = new CommandApiKeyProvider(env);

    assertThrows(RuntimeException.class, () -> provider.getExpectedValues(null, "any-key"));
  }
}

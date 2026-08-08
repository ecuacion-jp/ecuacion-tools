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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests for {@link CommandApiKeyFileLocator}. The default-location branches depend on the
 * {@code user.dir} system property (mirroring where a real deployment's war/config directory
 * would be), so each test that exercises them points it at a fresh temp directory and restores
 * the original value afterward to avoid leaking state into other tests.
 */
class CommandApiKeyFileLocatorTest {

  private @Nullable String originalUserDir;

  @BeforeEach
  void saveUserDir() {
    originalUserDir = System.getProperty("user.dir");
  }

  @SuppressWarnings("null")
  @AfterEach
  void restoreUserDir() {
    System.setProperty("user.dir", originalUserDir);
  }

  private static Path createTempBaseDir() {
    try {
      return Files.createTempDirectory("command-api-key-file-locator-test");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  void configuredPropertyIsReturnedEvenIfFileDoesNotExist() {
    MockEnvironment env = new MockEnvironment();
    Path missing = createTempBaseDir().resolve("absent.txt");
    env.setProperty(CommandApiKeyFileLocator.PROP_API_KEY_FILE_PATH, missing.toString());

    assertEquals(missing, CommandApiKeyFileLocator.resolve(env));
  }

  @Test
  void unresolvableEnvironmentVariableInConfiguredPropertyThrows() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty(CommandApiKeyFileLocator.PROP_API_KEY_FILE_PATH,
        "${THIS_ENV_VAR_SHOULD_NOT_EXIST_XYZ123}/api-key.txt");

    assertThrows(RuntimeException.class, () -> CommandApiKeyFileLocator.resolve(env));
  }

  @Test
  void propertyUnconfiguredAndNoDefaultFileReturnsNull() {
    System.setProperty("user.dir", createTempBaseDir().toString());
    MockEnvironment env = new MockEnvironment();

    assertNull(CommandApiKeyFileLocator.resolve(env));
  }

  @Test
  void propertyUnconfiguredFallsBackToConfigSubdirDefaultFile() throws IOException {
    Path baseDir = createTempBaseDir();
    Path configDir = Files.createDirectory(baseDir.resolve("config"));
    Path defaultFile = Files.createFile(configDir.resolve(CommandApiKeyFileLocator.DEFAULT_FILE_NAME));
    System.setProperty("user.dir", baseDir.toString());
    MockEnvironment env = new MockEnvironment();

    assertEquals(defaultFile, CommandApiKeyFileLocator.resolve(env));
  }

  @Test
  void propertyUnconfiguredFallsBackToBareDefaultFileWhenConfigSubdirAbsent() throws IOException {
    Path baseDir = createTempBaseDir();
    Path defaultFile = Files.createFile(baseDir.resolve(CommandApiKeyFileLocator.DEFAULT_FILE_NAME));
    System.setProperty("user.dir", baseDir.toString());
    MockEnvironment env = new MockEnvironment();

    assertEquals(defaultFile, CommandApiKeyFileLocator.resolve(env));
  }

  @Test
  void configSubdirDefaultFileTakesPriorityOverBareDefaultFile() throws IOException {
    Path baseDir = createTempBaseDir();
    Path configDir = Files.createDirectory(baseDir.resolve("config"));
    Path configDefaultFile =
        Files.createFile(configDir.resolve(CommandApiKeyFileLocator.DEFAULT_FILE_NAME));
    Files.createFile(baseDir.resolve(CommandApiKeyFileLocator.DEFAULT_FILE_NAME));
    System.setProperty("user.dir", baseDir.toString());
    MockEnvironment env = new MockEnvironment();

    assertEquals(configDefaultFile, CommandApiKeyFileLocator.resolve(env));
  }
}

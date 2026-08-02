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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

/**
 * Plain (no Spring context) unit tests for {@link CommandApiService}'s script-resolution and
 * execution error branches. {@link CommandApiControllerTest} already covers the access-control
 * behavior end-to-end via MockMvc; this class targets branches only reachable through a specific,
 * deliberately-broken script definition (invalid path characters, missing file, non-executable
 * file, environment variable resolution) that would be awkward to wire up as a full Spring Boot
 * test context per case. {@link CommandApiService}'s constructor needs only a
 * {@code ConfigurableEnvironment}, so {@link MockEnvironment} is enough.
 */
class CommandApiServiceTest {

  private static final String SCRIPT_ID = "script.under-test";

  /**
   * Matches {@code CommandApiService.SCRIPT_PROPERTIES_SOURCE_NAME_MARKER}, so the registered
   * script definition resolves the same way a real {@code ecuacion-tool-command-api.properties}
   * entry would.
   */
  private static final String SCRIPT_PROPERTIES_SOURCE_NAME =
      "Config resource 'class path resource [ecuacion-tool-command-api.properties]' "
          + "via location 'test'";

  @SuppressWarnings("null")
  private static CommandApiService newService(String scriptDefinitionValue) {
    MockEnvironment env = new MockEnvironment();
    env.getPropertySources().addFirst(
        new MapPropertySource(SCRIPT_PROPERTIES_SOURCE_NAME, Map.of(SCRIPT_ID, scriptDefinitionValue)));
    // Only script resolution/execution is under test here (via executeScriptByKey, which never
    // consults this flag), so api-key-required is turned off purely to satisfy the constructor's
    // fail-fast check that api-key-file-path be set whenever it's left true.
    env.setProperty("jp.ecuacion.tool.command-api.api-key-required", "false");
    return new CommandApiService(env);
  }

  @Test
  void constructorThrowsWhenScriptPropertiesFileIsAbsent() {
    // No PropertySource matching SCRIPT_PROPERTIES_SOURCE_NAME_MARKER is registered at all,
    // simulating ecuacion-tool-command-api.properties missing entirely.
    MockEnvironment env = new MockEnvironment();
    env.setProperty("jp.ecuacion.tool.command-api.api-key-required", "false");

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> new CommandApiService(env));

    assertTrue(ex.getMessage().contains("ecuacion-tool-command-api.properties"));
  }

  @SuppressWarnings("null")
  @Test
  void constructorThrowsWhenApiKeyRequiredButNoApiKeyFilePathConfigured() {
    // api-key-required is left unset (defaults to true), and api-key-file-path is unset too,
    // so no scriptId could ever be executed through either endpoint.
    MockEnvironment env = new MockEnvironment();
    env.getPropertySources().addFirst(new MapPropertySource(SCRIPT_PROPERTIES_SOURCE_NAME,
        Map.of(SCRIPT_ID, "ALL:/tmp/unused.sh")));

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> new CommandApiService(env));

    assertTrue(ex.getMessage().contains("api-key-file-path"));
  }

  private static Path createExecutableScript(String scriptBody) {
    try {
      Path dir = Files.createTempDirectory("command-api-service-test-script");
      Path script = dir.resolve("script.sh");
      Files.writeString(script, scriptBody);
      script.toFile().setExecutable(true);
      return script;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  void scriptFilePathWithInvalidCharacterIsRejected() {
    CommandApiService service = newService("ALL:/tmp/some#script.sh");

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> service.executeScriptByKey(HttpMethod.POST, SCRIPT_ID, null));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
  }

  @Test
  void scriptFileNotFoundIsRejected() throws IOException {
    Path missing = Files.createTempDirectory("command-api-service-test-missing")
        .resolve("does-not-exist.sh");
    CommandApiService service = newService("ALL:" + missing);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> service.executeScriptByKey(HttpMethod.POST, SCRIPT_ID, null));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    // The whole point of this message is to tell the caller *why* it failed (a missing file,
    // as opposed to e.g. a permission problem) and *which* path was missing — both need to
    // actually be in the message, not just a generic "something went wrong".
    assertTrue(Objects.requireNonNull(ex.getReason()).contains("not found"));
    assertTrue(Objects.requireNonNull(ex.getReason()).contains(missing.toString()));
  }

  @Test
  void scriptFileNotExecutableIsRejected() throws IOException {
    Path dir = Files.createTempDirectory("command-api-service-test-noexec");
    Path script = dir.resolve("script.sh");
    Files.writeString(script, "#!/bin/bash\necho hello\n");
    script.toFile().setExecutable(false);
    CommandApiService service = newService("ALL:" + script);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> service.executeScriptByKey(HttpMethod.POST, SCRIPT_ID, null));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
  }

  @Test
  void environmentVariableInScriptFilePathIsResolved() {
    // "PATH" is expected to be set in any environment this test runs in. The resolved script
    // file certainly does not exist, so this only verifies the "${PATH}" placeholder itself was
    // substituted away (a literal, unresolved "${PATH}" would also fail with "not found", so a
    // passing "not found" assertion alone wouldn't prove substitution happened).
    CommandApiService service =
        newService("ALL:${PATH}/definitely-not-a-real-script-xyz123.sh");

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> service.executeScriptByKey(HttpMethod.POST, SCRIPT_ID, null));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    assertFalse(Objects.requireNonNull(ex.getReason()).contains("${PATH}"));
  }

  @Test
  void unresolvableEnvironmentVariableInScriptFilePathThrows() {
    CommandApiService service =
        newService("ALL:${THIS_ENV_VAR_SHOULD_NOT_EXIST_XYZ123}/script.sh");

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> service.executeScriptByKey(HttpMethod.POST, SCRIPT_ID, null));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    assertTrue(Objects.requireNonNull(ex.getReason())
        .contains("THIS_ENV_VAR_SHOULD_NOT_EXIST_XYZ123"));
  }

  @Test
  void malformedEnvironmentVariablePlaceholderInScriptFilePathThrows() {
    // "${" with no closing "}" is a malformed placeholder, distinct from a well-formed
    // placeholder naming a variable that just isn't set (the case above).
    CommandApiService service = newService("ALL:${UNCLOSED/script.sh");

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> service.executeScriptByKey(HttpMethod.POST, SCRIPT_ID, null));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
  }

  @Test
  void scriptPathPointingToADirectoryFailsToStart() throws IOException {
    // A directory passes both exists() and canExecute() (its executable bit means
    // "traversable", not "runnable"), so this can only be caught once Runtime.exec() itself
    // rejects it — verifying that failure is reported clearly rather than as a generic error.
    Path dir = Files.createTempDirectory("command-api-service-test-dir-as-script");
    CommandApiService service = newService("ALL:" + dir);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> service.executeScriptByKey(HttpMethod.POST, SCRIPT_ID, null));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    assertTrue(Objects.requireNonNull(ex.getReason()).contains("Failed to start"));
  }

  @Test
  void commaSeparatedParametersAreSplitIntoSeparateArguments() throws Exception {
    Path script = createExecutableScript(
        "#!/bin/bash\necho \"count:$#\"\necho \"1:$1\"\necho \"2:$2\"\n");
    CommandApiService service = newService("ALL:" + script);

    Map<String, String> result =
        service.executeScriptByKey(HttpMethod.POST, SCRIPT_ID, "param1,param2");

    assertEquals("0", result.get("returnCode"));
    String stdout = result.get("stdout");
    assertEquals("count:2" + System.lineSeparator() + "1:param1" + System.lineSeparator()
        + "2:param2", stdout);
  }
}

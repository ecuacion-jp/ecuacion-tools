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

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import jp.ecuacion.splib.rest.apikey.SplibApiKeyAuthenticationFilter;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests access control added to {@link CommandApiController}: the {@code allow-insecure-access}
 * gated {@code api/public/executeScript} GET endpoint, and the always-key-required
 * {@code api/key/executeScript} POST endpoint (authenticated by ecuacion-splib-rest's
 * {@link SplibApiKeyAuthenticationFilter} via {@link jp.ecuacion.util.commandapi.web.config.CommandApiKeyProvider}).
 */
class CommandApiControllerTest {

  private static final String SCRIPT_ID = "script.say-hello";
  private static final String ALLOW_INSECURE_ACCESS_PROP =
      "jp.ecuacion.tool.command-api.allow-insecure-access";
  private static final String API_KEY_FILE_PATH_PROP =
      "jp.ecuacion.tool.command-api.api-key-file-path";
  private static final String CORRECT_API_KEY = "s3cr3t-key";

  private static Path createExecutableScript() {
    try {
      Path dir = Files.createTempDirectory("command-api-test-script");
      Path script = dir.resolve("sayHello.sh");
      Files.writeString(script, "#!/bin/bash\necho hello\n");
      script.toFile().setExecutable(true);
      return script;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Registers {@link #SCRIPT_ID} under a {@code PropertySource} name matching
   * {@code CommandApiController.SCRIPT_PROPERTIES_SOURCE_NAME_MARKER}, so it resolves the same
   * way a real {@code ecuacion-tool-command-api.properties} entry would — as opposed to
   * {@code @DynamicPropertySource}, whose "Dynamic Test Properties" source does not match that
   * filter. The other test-only properties (allow-insecure-access, api-key-file-path) are not
   * scoped that way in production, so they stay on {@code @DynamicPropertySource} as before.
   */
  private static class ScriptPropertySourceInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @SuppressWarnings("null")
    @Override
    public void initialize(@Nullable ConfigurableApplicationContext applicationContext) {
      Objects.requireNonNull(applicationContext).getEnvironment().getPropertySources()
          .addFirst(
              new MapPropertySource(
                  "Config resource 'class path resource [ecuacion-tool-command-api.properties]' "
                      + "via location 'test'",
                  Map.of(SCRIPT_ID, createExecutableScript().toString())));
    }
  }

  private static Path createApiKeyFile(String content) {
    try {
      Path dir = Files.createTempDirectory("command-api-test-key");
      Path file = dir.resolve("api-key.txt");
      Files.writeString(file, content);
      return file;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** When neither access-control property is configured, GET must stay disabled (secure default). */
  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @AutoConfigureMockMvc
  @ContextConfiguration(initializers = ScriptPropertySourceInitializer.class)
  class WhenAccessControlPropertiesAreUnset {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getIsForbidden() throws Exception {
      mockMvc.perform(get("/api/public/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isForbidden());
    }
  }

  /** A correctly configured api-key file: {@code api/key/executeScript} POST behavior. */
  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @AutoConfigureMockMvc
  @ContextConfiguration(initializers = ScriptPropertySourceInitializer.class)
  class WhenApiKeyFileIsValid {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
      registry.add(ALLOW_INSECURE_ACCESS_PROP, () -> "false");
      // Trailing newline verifies the api-key file content is trimmed before comparison.
      registry.add(API_KEY_FILE_PATH_PROP,
          () -> createApiKeyFile(CORRECT_API_KEY + "\n").toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getIsForbidden() throws Exception {
      mockMvc.perform(get("/api/public/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isForbidden());
    }

    @Test
    void postWithoutApiKeyHeaderIsUnauthorized() throws Exception {
      mockMvc.perform(post("/api/key/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithWrongApiKeyHeaderIsUnauthorized() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, "wrong-key"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithCorrectApiKeyHeaderSucceeds() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isOk()).andExpect(jsonPath("$.returnCode").value("0"));
    }
  }

  /** The api-key file is missing: must fail closed without leaking the path. */
  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @AutoConfigureMockMvc
  @ContextConfiguration(initializers = ScriptPropertySourceInitializer.class)
  class WhenApiKeyFileIsMissing {

    @SuppressWarnings({"NullAway.Init", "null"})
    private static Path missingFilePath;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
      registry.add(ALLOW_INSECURE_ACCESS_PROP, () -> "false");
      missingFilePath = Files.createTempDirectory("command-api-test-key").resolve("absent.txt");
      registry.add(API_KEY_FILE_PATH_PROP, () -> missingFilePath.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postIsUnauthorizedAndDoesNotLeakServerConfiguration() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(not(Matchers.containsString(missingFilePath.toString()))));
    }
  }

  /**
   * {@code allow-insecure-access=true}: GET is allowed. POST must still require a valid
   * {@code X-Api-Key} regardless — this flag only ever adds the GET convenience endpoint, it
   * never weakens {@code api/key/executeScript}. No api-key file is configured in this class, so
   * every POST here is necessarily unauthorized.
   */
  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @AutoConfigureMockMvc
  @ContextConfiguration(initializers = ScriptPropertySourceInitializer.class)
  class WhenAllowInsecureAccessIsTrue {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
      registry.add(ALLOW_INSECURE_ACCESS_PROP, () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getSucceeds() throws Exception {
      mockMvc.perform(get("/api/public/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isOk()).andExpect(jsonPath("$.returnCode").value("0"));
    }

    @Test
    void postWithoutApiKeyHeaderIsStillUnauthorized() throws Exception {
      mockMvc.perform(post("/api/key/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void getWithAllowlistedParameterSucceeds() throws Exception {
      mockMvc
          .perform(get("/api/public/executeScript").param("scriptId", SCRIPT_ID).param("parameter",
              "param1,param2"))
          .andExpect(status().isOk()).andExpect(jsonPath("$.returnCode").value("0"));
    }

    /**
     * Regression test: {@code scriptId} must resolve only against the dedicated
     * {@code ecuacion-tool-command-api.properties}-backed property source, not the full merged
     * {@code Environment} — otherwise a {@code scriptId} that happens to match an unrelated JVM
     * system property or OS environment variable name would be treated as a valid script
     * definition, and its (possibly sensitive) value would be echoed back in the "not found"
     * error response.
     */
    @Test
    void getWithScriptIdMatchingUnrelatedSystemPropertyIsRejectedAndDoesNotLeakItsValue()
        throws Exception {
      String unrelatedKey = "some.unrelated.secret";
      String unrelatedValue = "s3cr3t-value-that-must-not-leak";
      System.setProperty(unrelatedKey, unrelatedValue);
      try {
        mockMvc.perform(get("/api/public/executeScript").param("scriptId", unrelatedKey))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(not(Matchers.containsString(unrelatedValue))));
      } finally {
        System.clearProperty(unrelatedKey);
      }
    }

    @Test
    void getWithShellMetacharacterInParameterIsRejected() throws Exception {
      mockMvc.perform(get("/api/public/executeScript").param("scriptId", SCRIPT_ID)
          .param("parameter", "param1 & calc.exe")).andExpect(status().isBadRequest());
    }
  }
}

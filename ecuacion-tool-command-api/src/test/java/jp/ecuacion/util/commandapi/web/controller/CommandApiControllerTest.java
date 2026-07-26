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
 * Tests access control added to {@link CommandApiController}: the {@code api-key-required} gated
 * {@code api/public/executeScript} GET/POST endpoints, and the always-key-required
 * {@code api/key/executeScript} GET/POST endpoints (authenticated by ecuacion-splib-rest's
 * {@link SplibApiKeyAuthenticationFilter} via
 * {@link jp.ecuacion.util.commandapi.web.config.CommandApiKeyProvider}), including the per-script
 * {@code GET:} / {@code POST:} / {@code ALL:} method restriction.
 */
class CommandApiControllerTest {

  private static final String SCRIPT_ID = "script.say-hello";
  private static final String GET_ONLY_SCRIPT_ID = "script.get-only";
  private static final String POST_ONLY_SCRIPT_ID = "script.post-only";
  private static final String ALL_METHODS_SCRIPT_ID = "script.all-methods";
  private static final String MIXED_CASE_GET_SCRIPT_ID = "script.mixed-case-get";
  private static final String MIXED_CASE_POST_SCRIPT_ID = "script.mixed-case-post";
  private static final String MIXED_CASE_ALL_SCRIPT_ID = "script.mixed-case-all";
  private static final String API_KEY_REQUIRED_PROP =
      "jp.ecuacion.tool.command-api.api-key-required";
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
   * Registers script definitions under a {@code PropertySource} name matching
   * {@code CommandApiService.SCRIPT_PROPERTIES_SOURCE_NAME_MARKER}, so they resolve the same
   * way real {@code ecuacion-tool-command-api.properties} entries would — as opposed to
   * {@code @DynamicPropertySource}, whose "Dynamic Test Properties" source does not match that
   * filter. The other test-only properties ({@code api-key-required}, {@code api-key-file-path})
   * are not scoped that way in production, so they stay on {@code @DynamicPropertySource}.
   */
  private abstract static class AbstractScriptPropertySourceInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    abstract Map<String, String> scriptDefinitions();

    @Override
    public void initialize(@Nullable ConfigurableApplicationContext applicationContext) {
      Objects.requireNonNull(applicationContext).getEnvironment().getPropertySources()
          .addFirst(
              new MapPropertySource(
                  "Config resource 'class path resource [ecuacion-tool-command-api.properties]' "
                      + "via location 'test'",
                  Map.copyOf(scriptDefinitions())));
    }
  }

  /** Registers {@link #SCRIPT_ID} with no method prefix (default: POST only). */
  private static class ScriptPropertySourceInitializer
      extends AbstractScriptPropertySourceInitializer {

    @SuppressWarnings("null")
    @Override
    Map<String, String> scriptDefinitions() {
      return Map.of(SCRIPT_ID, createExecutableScript().toString());
    }
  }

  /**
   * Registers one script per {@code GET:} / {@code POST:} / {@code ALL:} prefix (plus a
   * mixed-case variant of each and a no-prefix one), to exercise per-script method restriction on
   * {@code api/key/executeScript}. The mixed-case variants (e.g. {@code gEt:}, not just
   * {@code get:}) verify prefix matching upper-cases and compares rather than only recognizing
   * one specific alternate casing.
   */
  private static class MethodRestrictedScriptPropertySourceInitializer
      extends AbstractScriptPropertySourceInitializer {

    @SuppressWarnings("null")
    @Override
    Map<String, String> scriptDefinitions() {
      return Map.of(
          SCRIPT_ID, createExecutableScript().toString(),
          GET_ONLY_SCRIPT_ID, "GET:" + createExecutableScript(),
          POST_ONLY_SCRIPT_ID, "POST:" + createExecutableScript(),
          ALL_METHODS_SCRIPT_ID, "ALL:" + createExecutableScript(),
          MIXED_CASE_GET_SCRIPT_ID, "gEt:" + createExecutableScript(),
          MIXED_CASE_POST_SCRIPT_ID, "PoSt:" + createExecutableScript(),
          MIXED_CASE_ALL_SCRIPT_ID, "aLl:" + createExecutableScript());
    }
  }

  private static Path createStdoutAndStderrScript() {
    try {
      Path dir = Files.createTempDirectory("command-api-test-script");
      Path script = dir.resolve("stdoutAndStderr.sh");
      Files.writeString(script, "#!/bin/bash\necho out-line\necho err-line >&2\n");
      script.toFile().setExecutable(true);
      return script;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Registers a script that writes to both stdout and stderr, one line each. */
  private static class StdoutAndStderrScriptPropertySourceInitializer
      extends AbstractScriptPropertySourceInitializer {

    static final String STDOUT_AND_STDERR_SCRIPT_ID = "script.stdout-and-stderr";

    @SuppressWarnings("null")
    @Override
    Map<String, String> scriptDefinitions() {
      return Map.of(STDOUT_AND_STDERR_SCRIPT_ID, "ALL:" + createStdoutAndStderrScript());
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
      registry.add(API_KEY_REQUIRED_PROP, () -> "true");
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
          .andExpect(status().isOk()).andExpect(jsonPath("$.returnCode").value("0"))
          .andExpect(jsonPath("$.stdout").value("hello"))
          .andExpect(jsonPath("$.stderr").value(""));
    }

    @Test
    void getWithCorrectApiKeyHeaderIsForbiddenSinceScriptDefaultsToPostOnly() throws Exception {
      mockMvc
          .perform(get("/api/key/executeScript").param("scriptId", SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isForbidden());
    }
  }

  /**
   * The api-key file lists more than one key, one per line: a request presenting any of them
   * must succeed, and one presenting neither must still be rejected. Also verifies blank lines
   * are ignored.
   */
  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @AutoConfigureMockMvc
  @ContextConfiguration(initializers = ScriptPropertySourceInitializer.class)
  class WhenApiKeyFileListsMultipleKeys {

    private static final String OTHER_API_KEY = "another-s3cr3t-key";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
      registry.add(API_KEY_REQUIRED_PROP, () -> "true");
      registry.add(API_KEY_FILE_PATH_PROP,
          () -> createApiKeyFile("\n" + CORRECT_API_KEY + "\n\n" + OTHER_API_KEY + "\n").toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postWithFirstApiKeyHeaderSucceeds() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isOk()).andExpect(jsonPath("$.returnCode").value("0"));
    }

    @Test
    void postWithSecondApiKeyHeaderSucceeds() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, OTHER_API_KEY))
          .andExpect(status().isOk()).andExpect(jsonPath("$.returnCode").value("0"));
    }

    @Test
    void postWithWrongApiKeyHeaderIsUnauthorized() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, "wrong-key"))
          .andExpect(status().isUnauthorized());
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
      registry.add(API_KEY_REQUIRED_PROP, () -> "true");
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
   * {@code api-key-required=false} enables both {@code GET} and {@code POST} on
   * {@code api/public/executeScript}; which of the two a given script accepts is governed by the
   * exact same {@code GET:} / {@code POST:} / {@code ALL:} declaration as
   * {@code api/key/executeScript} (see {@link WhenScriptDeclaresAnAllowedMethod}) — the only
   * difference from that endpoint is that no {@code X-Api-Key} is required here.
   * {@code api/key/executeScript} itself is untouched by this flag and must still require a
   * valid {@code X-Api-Key} regardless. No api-key file is configured in this class, so every
   * request there is necessarily unauthorized.
   */
  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @AutoConfigureMockMvc
  @ContextConfiguration(initializers = MethodRestrictedScriptPropertySourceInitializer.class)
  class WhenApiKeyIsNotRequired {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
      registry.add(API_KEY_REQUIRED_PROP, () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getOnGetOnlyScriptSucceeds() throws Exception {
      mockMvc.perform(get("/api/public/executeScript").param("scriptId", GET_ONLY_SCRIPT_ID))
          .andExpect(status().isOk()).andExpect(jsonPath("$.returnCode").value("0"))
          .andExpect(jsonPath("$.stdout").value("hello"))
          .andExpect(jsonPath("$.stderr").value(""));
    }

    @Test
    void getOnPostOnlyScriptIsForbidden() throws Exception {
      mockMvc.perform(get("/api/public/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isForbidden());
    }

    @Test
    void postOnPostOnlyScriptSucceeds() throws Exception {
      mockMvc.perform(post("/api/public/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isOk()).andExpect(jsonPath("$.returnCode").value("0"));
    }

    @Test
    void postOnGetOnlyScriptIsForbidden() throws Exception {
      mockMvc.perform(post("/api/public/executeScript").param("scriptId", GET_ONLY_SCRIPT_ID))
          .andExpect(status().isForbidden());
    }

    @Test
    void getAndPostOnAllMethodsScriptSucceed() throws Exception {
      mockMvc.perform(get("/api/public/executeScript").param("scriptId", ALL_METHODS_SCRIPT_ID))
          .andExpect(status().isOk());
      mockMvc.perform(post("/api/public/executeScript").param("scriptId", ALL_METHODS_SCRIPT_ID))
          .andExpect(status().isOk());
    }

    @Test
    void postWithoutApiKeyHeaderIsStillUnauthorized() throws Exception {
      mockMvc.perform(post("/api/key/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void getWithAllowlistedParameterSucceeds() throws Exception {
      mockMvc
          .perform(get("/api/public/executeScript").param("scriptId", ALL_METHODS_SCRIPT_ID)
              .param("parameter", "param1,param2"))
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
      mockMvc.perform(get("/api/public/executeScript").param("scriptId", ALL_METHODS_SCRIPT_ID)
          .param("parameter", "param1 & calc.exe")).andExpect(status().isBadRequest());
    }
  }

  /**
   * With a valid API key, {@code api/key/executeScript} enforces each script's declared
   * {@code GET:} / {@code POST:} / {@code ALL:} method restriction (case-insensitive prefix; no
   * prefix means POST only).
   */
  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @AutoConfigureMockMvc
  @ContextConfiguration(initializers = MethodRestrictedScriptPropertySourceInitializer.class)
  class WhenScriptDeclaresAnAllowedMethod {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
      registry.add(API_KEY_REQUIRED_PROP, () -> "true");
      registry.add(API_KEY_FILE_PATH_PROP,
          () -> createApiKeyFile(CORRECT_API_KEY).toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getOnGetOnlyScriptSucceeds() throws Exception {
      mockMvc
          .perform(get("/api/key/executeScript").param("scriptId", GET_ONLY_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isOk());
    }

    @Test
    void postOnGetOnlyScriptIsForbidden() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", GET_ONLY_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isForbidden());
    }

    @Test
    void postOnPostOnlyScriptSucceeds() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", POST_ONLY_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isOk());
    }

    @Test
    void getOnPostOnlyScriptIsForbidden() throws Exception {
      mockMvc
          .perform(get("/api/key/executeScript").param("scriptId", POST_ONLY_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isForbidden());
    }

    @Test
    void getOnAllMethodsScriptSucceeds() throws Exception {
      mockMvc
          .perform(get("/api/key/executeScript").param("scriptId", ALL_METHODS_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isOk());
    }

    @Test
    void postOnAllMethodsScriptSucceeds() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", ALL_METHODS_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isOk());
    }

    @Test
    void mixedCaseGetPrefixIsRecognized() throws Exception {
      mockMvc
          .perform(get("/api/key/executeScript").param("scriptId", MIXED_CASE_GET_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isOk());
    }

    @Test
    void postOnMixedCaseGetPrefixScriptIsForbidden() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", MIXED_CASE_GET_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isForbidden());
    }

    @Test
    void mixedCasePostPrefixIsRecognized() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", MIXED_CASE_POST_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isOk());
    }

    @Test
    void getOnMixedCasePostPrefixScriptIsForbidden() throws Exception {
      mockMvc
          .perform(get("/api/key/executeScript").param("scriptId", MIXED_CASE_POST_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isForbidden());
    }

    @Test
    void mixedCaseAllPrefixIsRecognizedOnGet() throws Exception {
      mockMvc
          .perform(get("/api/key/executeScript").param("scriptId", MIXED_CASE_ALL_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isOk());
    }

    @Test
    void mixedCaseAllPrefixIsRecognizedOnPost() throws Exception {
      mockMvc
          .perform(post("/api/key/executeScript").param("scriptId", MIXED_CASE_ALL_SCRIPT_ID)
              .header(SplibApiKeyAuthenticationFilter.HEADER_API_KEY, CORRECT_API_KEY))
          .andExpect(status().isOk());
    }
  }

  /** Verifies stdout and stderr are captured into separate response fields, not merged. */
  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @AutoConfigureMockMvc
  @ContextConfiguration(initializers = StdoutAndStderrScriptPropertySourceInitializer.class)
  class WhenScriptWritesToStdoutAndStderr {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
      registry.add(API_KEY_REQUIRED_PROP, () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void stdoutAndStderrAreCapturedSeparately() throws Exception {
      mockMvc
          .perform(get("/api/public/executeScript").param("scriptId",
              StdoutAndStderrScriptPropertySourceInitializer.STDOUT_AND_STDERR_SCRIPT_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.stdout").value("out-line"))
          .andExpect(jsonPath("$.stderr").value("err-line"));
    }
  }
}

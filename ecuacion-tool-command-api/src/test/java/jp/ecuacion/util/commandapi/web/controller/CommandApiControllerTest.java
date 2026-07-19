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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests access control (GET/POST + apiKey) added to {@link CommandApiController}.
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
  class WhenAccessControlPropertiesAreUnset {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
      registry.add(SCRIPT_ID, () -> createExecutableScript().toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getIsForbidden() throws Exception {
      mockMvc.perform(get("/api/public/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isForbidden());
    }
  }

  /** {@code allow-insecure-access=false} (secure mode) with a correctly configured api-key file. */
  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @AutoConfigureMockMvc
  class WhenSecureModeWithValidApiKeyFile {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
      registry.add(SCRIPT_ID, () -> createExecutableScript().toString());
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
    void postWithoutApiKeyIsUnauthorized() throws Exception {
      mockMvc.perform(post("/api/public/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithWrongApiKeyIsUnauthorized() throws Exception {
      mockMvc
          .perform(post("/api/public/executeScript").param("scriptId", SCRIPT_ID)
              .param("apiKey", "wrong-key"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithCorrectApiKeySucceeds() throws Exception {
      mockMvc
          .perform(post("/api/public/executeScript").param("scriptId", SCRIPT_ID)
              .param("apiKey", CORRECT_API_KEY))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.returnCode").value("0"));
    }
  }

  /** Secure mode where the api-key file is missing: must fail closed without leaking the path. */
  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @AutoConfigureMockMvc
  class WhenSecureModeWithMissingApiKeyFile {

    @SuppressWarnings("null")
    private static Path missingFilePath;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
      registry.add(SCRIPT_ID, () -> createExecutableScript().toString());
      registry.add(ALLOW_INSECURE_ACCESS_PROP, () -> "false");
      missingFilePath = Files.createTempDirectory("command-api-test-key").resolve("absent.txt");
      registry.add(API_KEY_FILE_PATH_PROP, () -> missingFilePath.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postIsUnauthorizedAndDoesNotLeakServerConfiguration() throws Exception {
      mockMvc
          .perform(post("/api/public/executeScript").param("scriptId", SCRIPT_ID)
              .param("apiKey", CORRECT_API_KEY))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(not(Matchers.containsString(missingFilePath.toString()))));
    }
  }

  /** {@code allow-insecure-access=true}: GET is allowed and POST skips apiKey verification. */
  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
  @AutoConfigureMockMvc
  class WhenAllowInsecureAccessIsTrue {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
      registry.add(SCRIPT_ID, () -> createExecutableScript().toString());
      registry.add(ALLOW_INSECURE_ACCESS_PROP, () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getSucceeds() throws Exception {
      mockMvc.perform(get("/api/public/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.returnCode").value("0"));
    }

    @Test
    void postWithoutApiKeySucceeds() throws Exception {
      mockMvc.perform(post("/api/public/executeScript").param("scriptId", SCRIPT_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.returnCode").value("0"));
    }
  }
}

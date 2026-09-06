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
package jp.ecuacion.tool.housekeepfiles.bl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Function;
import jp.ecuacion.tool.housekeepfiles.constant.Constants;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** Tests for {@link HousekeepFilesBl}. */
@DisplayName("HousekeepFilesBl")
class HousekeepFilesBlTest {

  private final HousekeepFilesBl bl = new HousekeepFilesBl();

  private HousekeepFilesForm form(String sysName) {
    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().setSysName(sysName);
    return form;
  }

  @Nested
  @DisplayName("createBuiltInVariableMap()")
  class CreateBuiltInVariableMap {

    @Test
    @DisplayName("contains SYS_NAME, YYYYMMDD, TIMESTAMP and HOSTNAME")
    void containsAllBuiltInVariables() throws Exception {
      Map<String, String> map = bl.createBuiltInVariableMap(form("test-system"));

      assertThat(map).containsKey(Constants.ENV_VAR_SYS_NAME)
          .containsKey(Constants.ENV_VAR_DATE).containsKey(Constants.ENV_VAR_TIMESTAMP)
          .containsKey(Constants.ENV_VAR_HOSTNAME);
      assertThat(map.get(Constants.ENV_VAR_SYS_NAME)).isEqualTo("test-system");
    }
  }

  @Nested
  @DisplayName("createEnvVarValueGetter()")
  class CreateEnvVarValueGetter {

    @Test
    @DisplayName("a built-in variable takes precedence over env")
    void builtInVariableTakesPrecedenceOverEnv() throws Exception {
      Map<String, String> builtInVariableMap = bl.createBuiltInVariableMap(form("test-system"));

      MockEnvironment env = new MockEnvironment();
      env.setProperty(Constants.ENV_VAR_SYS_NAME, "overridden-by-env");

      Function<String, String> getter = bl.createEnvVarValueGetter(builtInVariableMap, env);

      assertThat(getter.apply(Constants.ENV_VAR_SYS_NAME)).isEqualTo("test-system");
    }

    @Test
    @DisplayName("a non-built-in key falls back to env")
    void nonBuiltInKeyFallsBackToEnv() throws Exception {
      Map<String, String> builtInVariableMap = bl.createBuiltInVariableMap(form("test-system"));

      MockEnvironment env = new MockEnvironment();
      env.setProperty("BASE_DIR", "/tmp/base-dir");

      Function<String, String> getter = bl.createEnvVarValueGetter(builtInVariableMap, env);

      assertThat(getter.apply("BASE_DIR")).isEqualTo("/tmp/base-dir");
    }

    @Test
    @DisplayName("when env is null, a non-built-in key resolves to null")
    void nonBuiltInKeyResolvesToNullWhenEnvIsNull() throws Exception {
      Map<String, String> builtInVariableMap = bl.createBuiltInVariableMap(form("test-system"));

      Function<String, String> getter = bl.createEnvVarValueGetter(builtInVariableMap, null);

      assertThat(getter.apply("BASE_DIR")).isNull();
    }
  }
}

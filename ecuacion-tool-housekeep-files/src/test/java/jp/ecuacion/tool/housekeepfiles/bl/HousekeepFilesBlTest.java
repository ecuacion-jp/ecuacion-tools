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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.function.Function;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.tool.housekeepfiles.constant.Constants;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** Tests for {@link HousekeepFilesBl}. */
@SuppressWarnings("null")
@DisplayName("HousekeepFilesBl")
class HousekeepFilesBlTest {

  private final HousekeepFilesBl bl = new HousekeepFilesBl();

  private HousekeepFilesTaskRecord createDirRecord(String taskId, String taskName,
      String destPath) {
    return new HousekeepFilesTaskRecord(taskId, taskName, "CREATE_DIR", null, null, null, null,
        null, destPath, "TRUE", "FALSE", "IGNORE");
  }

  @Nested
  @DisplayName("consistencyCheckBetweenMultipleData()")
  class ConsistencyCheckBetweenMultipleData {

    @Test
    @DisplayName("throws MSG_ERR_AT_LEAST_ONE_TASK_NEEDED when the task list is empty")
    void emptyTaskList() {
      HousekeepFilesForm form = new HousekeepFilesForm();

      assertThatThrownBy(() -> bl.consistencyCheckBetweenMultipleData(form))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId)
                  .containsExactly("MSG_ERR_AT_LEAST_ONE_TASK_NEEDED"));
    }

    @Test
    @DisplayName("throws MSG_ERR_TASK_ID_DUPLICATED when taskId is duplicated")
    void duplicatedTaskId() {
      HousekeepFilesForm form = new HousekeepFilesForm();
      form.getTaskInfoHdRec().recList.add(createDirRecord("01", "task01", "/a"));
      form.getTaskInfoHdRec().recList.add(createDirRecord("01", "task02", "/b"));

      assertThatThrownBy(() -> bl.consistencyCheckBetweenMultipleData(form))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId)
                  .containsExactly("MSG_ERR_TASK_ID_DUPLICATED"));
    }

    @Test
    @DisplayName("throws MSG_ERR_TASK_NAME_DUPLICATED when taskName is duplicated")
    void duplicatedTaskName() {
      HousekeepFilesForm form = new HousekeepFilesForm();
      form.getTaskInfoHdRec().recList.add(createDirRecord("01", "task01", "/a"));
      form.getTaskInfoHdRec().recList.add(createDirRecord("02", "task01", "/b"));

      assertThatThrownBy(() -> bl.consistencyCheckBetweenMultipleData(form))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId)
                  .containsExactly("MSG_ERR_TASK_NAME_DUPLICATED"));
    }

    @Test
    @DisplayName("passes when taskId and taskName are both unique")
    void distinctTaskIdAndName() {
      HousekeepFilesForm form = new HousekeepFilesForm();
      form.getTaskInfoHdRec().recList.add(createDirRecord("01", "task01", "/a"));
      form.getTaskInfoHdRec().recList.add(createDirRecord("02", "task02", "/b"));

      bl.consistencyCheckBetweenMultipleData(form);
    }
  }

  @Nested
  @DisplayName("createBuiltInVariableMap()")
  class CreateBuiltInVariableMap {

    @Test
    @DisplayName("contains DATE, DATETIME, TIMESTAMP and HOSTNAME")
    void containsAllBuiltInVariables() throws Exception {
      Map<String, String> map = bl.createBuiltInVariableMap();

      assertThat(map).containsKey(Constants.ENV_VAR_DATE).containsKey(Constants.ENV_VAR_DATETIME)
          .containsKey(Constants.ENV_VAR_TIMESTAMP).containsKey(Constants.ENV_VAR_HOSTNAME);
    }
  }

  @Nested
  @DisplayName("createEnvVarValueGetter()")
  class CreateEnvVarValueGetter {

    @Test
    @DisplayName("a built-in variable takes precedence over env")
    void builtInVariableTakesPrecedenceOverEnv() throws Exception {
      Map<String, String> builtInVariableMap = bl.createBuiltInVariableMap();
      String actualHostname = builtInVariableMap.get(Constants.ENV_VAR_HOSTNAME);

      MockEnvironment env = new MockEnvironment();
      env.setProperty(Constants.ENV_VAR_HOSTNAME, "overridden-by-env");

      Function<String, String> getter = bl.createEnvVarValueGetter(builtInVariableMap, env);

      assertThat(getter.apply(Constants.ENV_VAR_HOSTNAME)).isEqualTo(actualHostname);
    }

    @Test
    @DisplayName("a non-built-in key falls back to env")
    void nonBuiltInKeyFallsBackToEnv() throws Exception {
      Map<String, String> builtInVariableMap = bl.createBuiltInVariableMap();

      MockEnvironment env = new MockEnvironment();
      env.setProperty("BASE_DIR", "/tmp/base-dir");

      Function<String, String> getter = bl.createEnvVarValueGetter(builtInVariableMap, env);

      assertThat(getter.apply("BASE_DIR")).isEqualTo("/tmp/base-dir");
    }

    @Test
    @DisplayName("when env is null, a non-built-in key resolves to null")
    void nonBuiltInKeyResolvesToNullWhenEnvIsNull() throws Exception {
      Map<String, String> builtInVariableMap = bl.createBuiltInVariableMap();

      Function<String, String> getter = bl.createEnvVarValueGetter(builtInVariableMap, null);

      assertThat(getter.apply("BASE_DIR")).isNull();
    }
  }
}

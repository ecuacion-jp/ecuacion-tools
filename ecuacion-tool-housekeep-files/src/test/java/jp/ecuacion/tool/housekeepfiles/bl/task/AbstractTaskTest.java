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
package jp.ecuacion.tool.housekeepfiles.bl.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the input rule check logic of {@link AbstractTask}, exercised through {@link Move} as
 * a representative concrete task.
 */
@DisplayName("AbstractTask")
class AbstractTaskTest {

  @Nested
  @DisplayName("checkTaskItem()")
  class CheckTaskItem {

    @ParameterizedTest(name = "checkPtn={0}, value={1} -> {2}")
    @MethodSource("provideCheckPatterns")
    @DisplayName("adds a BusinessViolation only when the input rule is broken")
    void checkTaskItem(TaskAttrCheckPtnEnum checkPtn, @Nullable String itemValue,
        @Nullable String expectedMessageId) {
      Violations violations = new Violations();

      new Move().checkTaskItem(violations, "aTaskId", TaskPtnEnum.MOVE, checkPtn, "unit",
          itemValue);

      if (expectedMessageId == null) {
        assertThat(violations.getBusinessViolations()).isEmpty();

      } else {
        assertThat(violations.getBusinessViolations())
            .extracting(BusinessViolation::getMessageId).containsExactly(expectedMessageId);
      }
    }

    static Stream<Arguments> provideCheckPatterns() {
      return Stream.of(
          Arguments.of(TaskAttrCheckPtnEnum.REQUIRED, null, "MSG_ERR_TASK_REQUIRED_CHECK"),
          Arguments.of(TaskAttrCheckPtnEnum.REQUIRED, "", "MSG_ERR_TASK_REQUIRED_CHECK"),
          Arguments.of(TaskAttrCheckPtnEnum.REQUIRED, "aValue", null),
          Arguments.of(TaskAttrCheckPtnEnum.PROHIBITED, null, null),
          Arguments.of(TaskAttrCheckPtnEnum.PROHIBITED, "", null),
          Arguments.of(TaskAttrCheckPtnEnum.PROHIBITED, "aValue", "MSG_ERR_TASK_PROHIBITED_CHECK"),
          Arguments.of(TaskAttrCheckPtnEnum.ARBITRARY, null, null),
          Arguments.of(TaskAttrCheckPtnEnum.ARBITRARY, "", null),
          Arguments.of(TaskAttrCheckPtnEnum.ARBITRARY, "aValue", null));
    }
  }

  @Nested
  @DisplayName("check()")
  class Check {

    @Test
    @DisplayName("checks each record item through checkTaskItem()")
    void eachItemIsCheckedThroughCheckTaskItem() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE",
          "aHost", "aPath", "TRUE", "DAY", "7", "IGNORE", "aPath", "TRUE", "FALSE", "IGNORE",
          null);

      AtomicInteger checkTaskItemCallCount = new AtomicInteger();
      Move move = new Move() {
        @Override
        public void checkTaskItem(Violations violations, String taskId, TaskPtnEnum taskPtn,
            TaskAttrCheckPtnEnum checkPtn, String itemTitle, @Nullable Object itemValue) {
          checkTaskItemCallCount.incrementAndGet();
        }
      };

      move.check(rec);

      assertThat(checkTaskItemCallCount.get()).isEqualTo(3);
    }
  }
}

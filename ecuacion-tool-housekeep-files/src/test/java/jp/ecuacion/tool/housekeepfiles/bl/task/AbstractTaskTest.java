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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.dto.other.FileInfo;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

    @SuppressWarnings("null")
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
          "aHost", "aPath", "TRUE", "7", "IGNORE", "aPath", "TRUE", "FALSE", "IGNORE");

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

  @Nested
  @DisplayName("getLocalFileInfo() / getLocalFileInfoList()")
  class LockDetection {

    // No test exercises the isLocked()=true branch here: java.nio.channels.FileChannel.tryLock()
    // (which FileUtil.isLocked() uses) throws OverlappingFileLockException rather than returning
    // "already locked" for a lock held elsewhere in the *same* JVM, so it can only be observed
    // with a lock held by another OS process - ecuacion-lib's own FileUtilTest (the source of
    // truth for isLocked() itself) has the same gap for the same reason. These tests instead
    // guard the regression this fix addresses: that FileUtil.isLocked()'s result actually reaches
    // FileInfo.isLocked() (it used to be discarded, leaving isLocked() permanently false - see
    // 2026-09-10-security-review-housekeep-files.md, finding 8).

    @Test
    @DisplayName("getLocalFileInfo() reports isLocked=false for a normal, unlocked file")
    void getLocalFileInfoReportsUnlocked(@TempDir Path tempDir) throws Exception {
      Path file = tempDir.resolve("plain.txt");
      Files.writeString(file, "content");
      Move move = new Move();

      FileInfo fi = Objects.requireNonNull(move.getLocalFileInfo(file.toString()));

      assertThat(fi.isLocked()).isFalse();
    }

    @Test
    @DisplayName("getLocalFileInfoList() reports isLocked=false for a normal, unlocked file")
    void getLocalFileInfoListReportsUnlocked(@TempDir Path tempDir) throws Exception {
      Path file = tempDir.resolve("plain.txt");
      Files.writeString(file, "content");
      Move move = new Move();

      List<FileInfo> list = move.getLocalFileInfoList(file.toString());

      assertThat(list).singleElement().satisfies(fi -> assertThat(fi.isLocked()).isFalse());
    }
  }
}

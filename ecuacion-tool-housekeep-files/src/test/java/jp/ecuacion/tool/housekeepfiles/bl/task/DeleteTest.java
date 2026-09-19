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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link Delete}, executed through {@link HousekeepFilesBlf}. */
@SuppressWarnings("null")
@DisplayName("Delete")
class DeleteTest {

  @TempDir
  Path tempDir;

  private HousekeepFilesForm form(HousekeepFilesTaskRecord taskRec) {
    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);

    return form;
  }

  @Nested
  @DisplayName("input validation")
  class InputValidation {

    @Test
    @DisplayName("remoteServer is prohibited")
    void remoteServerNotEmpty() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "DELETE",
          "aHost", "aPath", "FALSE", "0", "IGNORE", null, null, null, null);
      HousekeepFilesForm form = form(rec);

      assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId)
                  .containsExactly("MSG_ERR_TASK_PROHIBITED_CHECK"));
    }

    @Test
    @DisplayName("destPath is prohibited")
    void destPathNotEmpty() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "DELETE",
          null, "aPath", "FALSE", "0", "IGNORE", "aPath", "FALSE", "FALSE", "IGNORE");
      HousekeepFilesForm form = form(rec);

      assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId)
                  .containsExactly("MSG_ERR_TASK_PROHIBITED_CHECK"));
    }

    @Test
    @DisplayName("srcPath is required")
    void srcPathEmpty() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "DELETE",
          null, null, null, null, null, null, null, null, null);
      HousekeepFilesForm form = form(rec);

      assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId)
                  .containsExactly("MSG_ERR_TASK_REQUIRED_CHECK"));
    }
  }

  @Nested
  @DisplayName("deletion")
  class Deletion {

    private HousekeepFilesTaskRecord deleteRecord(String srcPath, String isSrcPathDir) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "DELETE", null, srcPath,
          isSrcPathDir, "0", "IGNORE", null, null, null, null);
    }

    @Test
    @DisplayName("deletes a file")
    void deletesFile() throws Exception {
      File file = tempDir.resolve("test.txt").toFile();
      file.createNewFile();

      new HousekeepFilesBlf().execute(form(deleteRecord(file.getAbsolutePath(), "FALSE")));

      assertThat(file).doesNotExist();
    }

    @Test
    @DisplayName("deletes a directory with its contents")
    void deletesDirectoryWithContents() throws Exception {
      File dir = tempDir.resolve("test-dir").toFile();
      dir.mkdir();
      Files.writeString(dir.toPath().resolve("child.txt"), "content");

      new HousekeepFilesBlf().execute(form(deleteRecord(dir.getAbsolutePath(), "TRUE")));

      assertThat(dir).doesNotExist();
    }

    @Test
    @DisplayName("a nonexistent source path with actionForNoSrcPath=IGNORE: no-op, no violation")
    void nonexistentSrcPathIgnored() throws Exception {
      String path = tempDir.resolve("nonexistent.txt").toString();

      new HousekeepFilesBlf().execute(form(deleteRecord(path, "FALSE")));
    }
  }
}

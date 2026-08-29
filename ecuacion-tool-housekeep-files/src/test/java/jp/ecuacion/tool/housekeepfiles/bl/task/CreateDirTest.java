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
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.tool.housekeepfiles.bl.HousekeepFilesBl;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesHdRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link CreateDir}, executed through {@link HousekeepFilesBlf}. */
@SuppressWarnings("null")
@DisplayName("CreateDir")
class CreateDirTest {

  @TempDir
  Path tempDir;

  private HousekeepFilesForm form(HousekeepFilesTaskRecord taskRec) {
    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);
    form.getTaskInfoHdRec().setSysName("test-system");

    return form;
  }

  private static void assertSingleBusinessViolation(HousekeepFilesForm form, String messageId) {
    assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form))
        .isInstanceOfSatisfying(ViolationException.class,
            ex -> assertThat(ex.getViolations().getBusinessViolations())
                .extracting(BusinessViolation::getMessageId).containsExactly(messageId));
  }

  @Nested
  @DisplayName("input validation")
  class InputValidation {

    @Test
    @DisplayName("remoteServer is prohibited")
    void remoteServerNotEmpty() {
      HousekeepFilesTaskRecord rec =
          new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "CREATE_DIR", "aHost", null, null,
              null, null, null, "aPath", "TRUE", "FALSE", "IGNORE", null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_PROHIBITED_CHECK");
    }

    @Test
    @DisplayName("srcPath is prohibited")
    void srcPathNotEmpty() {
      HousekeepFilesTaskRecord rec =
          new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "CREATE_DIR", null, "a", "TRUE",
              "5", "0", "IGNORE", "aPath", "TRUE", "FALSE", "IGNORE", null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_PROHIBITED_CHECK");
    }

    @Test
    @DisplayName("destPath is required")
    void destPathEmpty() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "CREATE_DIR", null, null, null, null, null, null, null, null, null, null, null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_REQUIRED_CHECK");
    }

    @Test
    @DisplayName("isDestPathDir cannot be FALSE")
    void isDestPathDirFalse() {
      HousekeepFilesTaskRecord rec =
          new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "CREATE_DIR", null, null, null,
              null, null, null, "aPath", "FALSE", "FALSE", "IGNORE", null);

      assertSingleBusinessViolation(form(rec),
          "MSG_ERR_TASK_CANNOT_SET_IS_DEST_PATH_DIR_TO_VALUE");
    }

    @Test
    @DisplayName("doesOverwriteDestPath cannot be TRUE")
    void doesOverwriteDestPathTrue() {
      HousekeepFilesTaskRecord rec =
          new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "CREATE_DIR", null, null, null,
              null, null, null, "aPath", "TRUE", "TRUE", "IGNORE", null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_CANNOT_SET_OVERWRITE_TO_VALUE");
    }
  }

  @Nested
  @DisplayName("destPath already exists")
  class DestPathAlreadyExists {

    private HousekeepFilesTaskRecord createDirRecord(String destPath, String whenDestPathExists) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "CREATE_DIR", null, null, null,
          null, null, null, destPath, "TRUE", "FALSE", whenDestPathExists, null);
    }

    @Test
    @DisplayName("existing directory with IGNORE: passes")
    void dirExistsIgnore() throws Exception {
      String path = tempDir.resolve("test-dir").toString();
      new File(path).mkdirs();

      new HousekeepFilesBlf().execute(form(createDirRecord(path, "IGNORE")));
    }

    @Test
    @DisplayName("existing directory with WARN: warn mail is sent")
    void dirExistsWarn() throws Exception {
      String path = tempDir.resolve("test-dir").toString();
      new File(path).mkdirs();

      // Replace HousekeepFilesBl#sendWarnMail to detect when it is called.
      AtomicBoolean warnMailSent = new AtomicBoolean();
      HousekeepFilesBl bl = new HousekeepFilesBl() {
        @Override
        public void sendWarnMail(List<BusinessViolation> warnList, HousekeepFilesHdRecord hdE)
            throws Exception {
          warnMailSent.set(true);
        }
      };

      new HousekeepFilesBlf(bl).execute(form(createDirRecord(path, "WARN")));

      assertThat(warnMailSent).isTrue();
    }

    @Test
    @DisplayName("existing directory with ERROR: MSG_ERR_DEST_PATH_EXISTS")
    void dirExistsError() {
      String path = tempDir.resolve("test-dir").toString();
      new File(path).mkdirs();

      assertSingleBusinessViolation(form(createDirRecord(path, "ERROR")),
          "MSG_ERR_DEST_PATH_EXISTS");
    }

    @Test
    @DisplayName("existing file with IGNORE: MSG_ERR_DEST_PATH_IS_FILE")
    void fileExistsIgnore() throws Exception {
      String path = tempDir.resolve("test-dir").toString();
      new File(path).createNewFile();

      assertSingleBusinessViolation(form(createDirRecord(path, "IGNORE")),
          "MSG_ERR_DEST_PATH_IS_FILE");
    }

    @Test
    @DisplayName("existing file with WARN: MSG_ERR_DEST_PATH_IS_FILE")
    void fileExistsWarn() throws Exception {
      String path = tempDir.resolve("test-dir").toString();
      new File(path).createNewFile();

      assertSingleBusinessViolation(form(createDirRecord(path, "WARN")),
          "MSG_ERR_DEST_PATH_IS_FILE");
    }

    @Test
    @DisplayName("existing file with ERROR: MSG_ERR_DEST_PATH_IS_FILE")
    void fileExistsError() throws Exception {
      String path = tempDir.resolve("test-dir").toString();
      new File(path).createNewFile();

      assertSingleBusinessViolation(form(createDirRecord(path, "ERROR")),
          "MSG_ERR_DEST_PATH_IS_FILE");
    }
  }

  @Nested
  @DisplayName("directory creation")
  class DirectoryCreation {

    private HousekeepFilesTaskRecord createDirRecord(String destPath) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "CREATE_DIR", null, null, null,
          null, null, null, destPath, "TRUE", "FALSE", "IGNORE", null);
    }

    @Test
    @DisplayName("creates a single-level directory")
    void singleLevel() throws Exception {
      File dir = tempDir.resolve("test-dir").toFile();
      assertThat(dir).doesNotExist();

      new HousekeepFilesBlf().execute(form(createDirRecord(dir.getPath())));

      assertThat(dir).exists();
    }

    @Test
    @DisplayName("creates a multi-level directory")
    void multipleLevels() throws Exception {
      File dir = tempDir.resolve("test-dir/1/2").toFile();
      assertThat(dir).doesNotExist();

      new HousekeepFilesBlf().execute(form(createDirRecord(dir.getPath())));

      assertThat(dir).exists();
    }
  }
}

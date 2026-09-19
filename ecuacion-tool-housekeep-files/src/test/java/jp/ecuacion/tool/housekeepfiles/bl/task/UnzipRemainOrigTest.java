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
import jp.ecuacion.tool.housekeepfiles.util.CompressUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link UnzipRemainOrig}, executed through {@link HousekeepFilesBlf}.
 *
 * <p>Also covers the shared behavior of {@link AbstractTaskUnzip}; {@link UnzipDeleteOrigTest}
 *     only tests what differs from here (deleting the original after unzipping).</p>
 */
@SuppressWarnings("null")
@DisplayName("UnzipRemainOrig")
class UnzipRemainOrigTest {

  @TempDir
  Path tempDir;

  private HousekeepFilesForm form(HousekeepFilesTaskRecord taskRec) {
    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);

    return form;
  }

  private static void assertSingleBusinessViolation(HousekeepFilesForm form, String messageId) {
    assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form))
        .isInstanceOfSatisfying(ViolationException.class,
            ex -> assertThat(ex.getViolations().getBusinessViolations())
                .extracting(BusinessViolation::getMessageId).containsExactly(messageId));
  }

  /** Creates a zip file at {@code zipPath} containing a single entry "data.txt". */
  private File createZipFixture(Path zipPath) throws Exception {
    File dataFile = tempDir.resolve("data.txt").toFile();
    Files.writeString(dataFile.toPath(), "content");
    File zipFile = zipPath.toFile();
    new CompressUtil().zipFile(dataFile.getAbsolutePath(), zipFile.getAbsolutePath());
    dataFile.delete();

    return zipFile;
  }

  @Nested
  @DisplayName("input validation")
  class InputValidation {

    @Test
    @DisplayName("remoteServer is prohibited")
    void remoteServerNotEmpty() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "UNZIP_REMAIN_ORIG", "aHost", "aPath", "FALSE", "0", "IGNORE", null, null, null, null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_PROHIBITED_CHECK");
    }

    @Test
    @DisplayName("srcPath is required")
    void srcPathEmpty() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "UNZIP_REMAIN_ORIG", null, null, null, null, null, null, null, null, null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_REQUIRED_CHECK");
    }

    @Test
    @DisplayName("isDestPathDir cannot be FALSE")
    void isDestPathDirFalse() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "UNZIP_REMAIN_ORIG", null, "aPath", "FALSE", "0", "IGNORE", "aPath", "FALSE", "FALSE",
          "IGNORE");

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_CANNOT_SET_IS_DEST_PATH_DIR_TO_VALUE");
    }
  }

  @Nested
  @DisplayName("unzip without destination specified")
  class UnzipWithoutDestination {

    private HousekeepFilesTaskRecord unzipRecord(String srcPath) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "UNZIP_REMAIN_ORIG", null,
          srcPath, "FALSE", "0", "IGNORE", null, null, null, null);
    }

    @Test
    @DisplayName("extracts alongside the zip file, keeping the original")
    void extractsNextToZipFile() throws Exception {
      File zipFile = createZipFixture(tempDir.resolve("archive.zip"));

      new HousekeepFilesBlf().execute(form(unzipRecord(zipFile.getAbsolutePath())));

      assertThat(tempDir.resolve("data.txt")).hasContent("content");
      assertThat(zipFile).exists();
    }
  }

  @Nested
  @DisplayName("unzip with destination specified")
  class UnzipWithDestination {

    private HousekeepFilesTaskRecord unzipRecord(String srcPath, String destPath) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "UNZIP_REMAIN_ORIG", null,
          srcPath, "FALSE", "0", "IGNORE", destPath, "TRUE", "FALSE", "IGNORE");
    }

    @Test
    @DisplayName("extracts into the specified directory")
    void extractsIntoSpecifiedDirectory() throws Exception {
      File zipFile = createZipFixture(tempDir.resolve("archive.zip"));
      File destDir = tempDir.resolve("extracted").toFile();

      new HousekeepFilesBlf().execute(
          form(unzipRecord(zipFile.getAbsolutePath(), destDir.getAbsolutePath())));

      assertThat(destDir.toPath().resolve("data.txt")).hasContent("content");
    }

    @Test
    @DisplayName("destination path exists as a file: MSG_ERR_DEST_PATH_IS_FILE")
    void destinationIsExistingFile() throws Exception {
      File zipFile = createZipFixture(tempDir.resolve("archive.zip"));
      File destFile = tempDir.resolve("not-a-dir").toFile();
      destFile.createNewFile();

      assertSingleBusinessViolation(
          form(unzipRecord(zipFile.getAbsolutePath(), destFile.getAbsolutePath())),
          "MSG_ERR_DEST_PATH_IS_FILE");
    }
  }
}

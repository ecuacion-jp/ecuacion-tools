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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.tool.housekeepfiles.bl.HousekeepFilesBl;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link Copy}, executed through {@link HousekeepFilesBlf}. */
@SuppressWarnings("null")
@DisplayName("Copy")
class CopyTest {

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

  private HousekeepFilesBl warnMailDetectingBl(AtomicBoolean warnMailSent) {
    return new HousekeepFilesBl() {
      @Override
      public void sendWarnMail(List<BusinessViolation> warnList, @Nullable String systemName)
          throws Exception {
        warnMailSent.set(true);
      }
    };
  }

  @Nested
  @DisplayName("input validation")
  class InputValidation {

    @Test
    @DisplayName("remoteServer is prohibited")
    void remoteServerNotEmpty() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "COPY",
          "aHost", "aPath", "FALSE", "0", "IGNORE", "aPath", "FALSE", "FALSE", "IGNORE");

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_PROHIBITED_CHECK");
    }

    @Test
    @DisplayName("srcPath is required")
    void srcPathEmpty() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "COPY",
          null, null, null, null, null, "aPath", "FALSE", "FALSE", "IGNORE");

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_REQUIRED_CHECK");
    }

    @Test
    @DisplayName("destPath is required")
    void destPathEmpty() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "COPY",
          null, "aPath", "FALSE", "0", "IGNORE", null, null, null, null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_REQUIRED_CHECK");
    }
  }

  @Nested
  @DisplayName("file copy")
  class FileCopy {

    private HousekeepFilesTaskRecord copyRecord(String srcPath, String destPath,
        String doesOverwriteDestPath, String actionForDestFileExists) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "COPY", null, srcPath, "FALSE",
          "0", "IGNORE", destPath, "FALSE", doesOverwriteDestPath, actionForDestFileExists);
    }

    @Test
    @DisplayName("copies a file to a nonexistent destination path")
    void copiesToNewFile() throws Exception {
      Path from = tempDir.resolve("from.txt");
      Files.writeString(from, "content");
      Path to = tempDir.resolve("to.txt");

      new HousekeepFilesBlf()
          .execute(form(copyRecord(from.toString(), to.toString(), "FALSE", "IGNORE")));

      assertThat(from).exists().hasContent("content");
      assertThat(to).exists().hasContent("content");
    }

    @Test
    @DisplayName("destPath exists, doesOverwriteDestPath=FALSE: MSG_ERR_DEST_PATH_EXISTSS when "
        + "actionForDestFileExists=ERROR")
    void destExistsErrorAction() throws Exception {
      Path from = tempDir.resolve("from.txt");
      Files.writeString(from, "new-content");
      Path to = tempDir.resolve("to.txt");
      Files.writeString(to, "old-content");

      assertSingleBusinessViolation(
          form(copyRecord(from.toString(), to.toString(), "FALSE", "ERROR")),
          "MSG_ERR_DEST_PATH_EXISTSS");
    }

    @Test
    @DisplayName("destPath exists, doesOverwriteDestPath=FALSE, actionForDestFileExists=WARN: "
        + "warn mail sent, destination left unchanged")
    void destExistsWarnActionNoOverwrite() throws Exception {
      Path from = tempDir.resolve("from.txt");
      Files.writeString(from, "new-content");
      Path to = tempDir.resolve("to.txt");
      Files.writeString(to, "old-content");

      AtomicBoolean warnMailSent = new AtomicBoolean();
      new HousekeepFilesBlf(warnMailDetectingBl(warnMailSent))
          .execute(form(copyRecord(from.toString(), to.toString(), "FALSE", "WARN")));

      assertThat(warnMailSent).isTrue();
      assertThat(to).hasContent("old-content");
    }

    @Test
    @DisplayName("destPath exists, doesOverwriteDestPath=TRUE, actionForDestFileExists=WARN: "
        + "warn mail sent, destination overwritten")
    void destExistsWarnActionWithOverwrite() throws Exception {
      Path from = tempDir.resolve("from.txt");
      Files.writeString(from, "new-content");
      Path to = tempDir.resolve("to.txt");
      Files.writeString(to, "old-content");

      AtomicBoolean warnMailSent = new AtomicBoolean();
      new HousekeepFilesBlf(warnMailDetectingBl(warnMailSent))
          .execute(form(copyRecord(from.toString(), to.toString(), "TRUE", "WARN")));

      assertThat(warnMailSent).isTrue();
      assertThat(to).hasContent("new-content");
    }

    @Test
    @DisplayName("destPath exists, doesOverwriteDestPath=FALSE, actionForDestFileExists=IGNORE: "
        + "no violation, destination left unchanged")
    void destExistsIgnoreActionNoOverwrite() throws Exception {
      Path from = tempDir.resolve("from.txt");
      Files.writeString(from, "new-content");
      Path to = tempDir.resolve("to.txt");
      Files.writeString(to, "old-content");

      new HousekeepFilesBlf()
          .execute(form(copyRecord(from.toString(), to.toString(), "FALSE", "IGNORE")));

      assertThat(to).hasContent("old-content");
    }
  }

  @Nested
  @DisplayName("directory copy")
  class DirectoryCopy {

    private HousekeepFilesTaskRecord copyDirRecord(String srcPath, String destPath) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "COPY", null, srcPath, "TRUE",
          "0", "IGNORE", destPath, "TRUE", "FALSE", "IGNORE");
    }

    @Test
    @DisplayName("copies a directory (with contents) under the destination directory, named "
        + "after the source directory")
    void copiesDirectoryUnderDest() throws Exception {
      File srcDir = tempDir.resolve("srcDir").toFile();
      srcDir.mkdir();
      Files.writeString(srcDir.toPath().resolve("child.txt"), "child-content");
      File destDir = tempDir.resolve("destDir").toFile();
      destDir.mkdir();

      new HousekeepFilesBlf()
          .execute(form(copyDirRecord(srcDir.getAbsolutePath(), destDir.getAbsolutePath())));

      assertThat(destDir.toPath().resolve("srcDir").resolve("child.txt")).hasContent(
          "child-content");
      // Source is left intact for Copy.
      assertThat(srcDir.toPath().resolve("child.txt")).exists();
    }

    @Test
    @DisplayName("destination directory already contains a same-named entry: "
        + "MSG_ERR_TO_DIR_EXISTS_AND_COPY_SETTING_VAGUE")
    void destDirAlreadyContainsSameNamedEntry() throws Exception {
      File srcDir = tempDir.resolve("srcDir").toFile();
      srcDir.mkdir();
      File destDir = tempDir.resolve("destDir").toFile();
      destDir.mkdir();
      new File(destDir, "srcDir").mkdir();

      assertSingleBusinessViolation(
          form(copyDirRecord(srcDir.getAbsolutePath(), destDir.getAbsolutePath())),
          "MSG_ERR_TO_DIR_EXISTS_AND_COPY_SETTING_VAGUE");
    }

    @Test
    @DisplayName("source directory contains a symbolic link: MSG_ERR_SRC_PATH_CONTAINS_SYMLINK, "
        + "nothing copied")
    void srcDirContainsSymbolicLink() throws Exception {
      File srcDir = tempDir.resolve("srcDir").toFile();
      srcDir.mkdir();
      Path linkTarget = tempDir.resolve("outsideTarget.txt");
      Files.writeString(linkTarget, "outside-content");
      Files.createSymbolicLink(srcDir.toPath().resolve("link"), linkTarget);
      File destDir = tempDir.resolve("destDir").toFile();
      destDir.mkdir();

      assertSingleBusinessViolation(
          form(copyDirRecord(srcDir.getAbsolutePath(), destDir.getAbsolutePath())),
          "MSG_ERR_SRC_PATH_CONTAINS_SYMLINK");
      assertThat(destDir.toPath().resolve("srcDir")).doesNotExist();
    }
  }
}

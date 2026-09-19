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

/**
 * Tests for {@link ZipRemainOrig}, executed through {@link HousekeepFilesBlf}.
 *
 * <p>Also covers the shared behavior of {@link AbstractTaskZip}; {@link ZipDeleteOrigTest} only
 *     tests what differs from here (deleting the original after zipping).</p>
 */
@SuppressWarnings("null")
@DisplayName("ZipRemainOrig")
class ZipRemainOrigTest {

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
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "ZIP_REMAIN_ORIG", "aHost", "aPath", "FALSE", "0", "IGNORE", null, null, null, null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_PROHIBITED_CHECK");
    }

    @Test
    @DisplayName("srcPath is required")
    void srcPathEmpty() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "ZIP_REMAIN_ORIG", null, null, null, null, null, null, null, null, null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_REQUIRED_CHECK");
    }
  }

  @Nested
  @DisplayName("zip without destination specified")
  class ZipWithoutDestination {

    private HousekeepFilesTaskRecord zipRecord(String srcPath) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "ZIP_REMAIN_ORIG", null,
          srcPath, "FALSE", "0", "IGNORE", null, null, null, null);
    }

    @Test
    @DisplayName("zips a file next to itself, keeping the original")
    void zipsFileNextToItself() throws Exception {
      File from = tempDir.resolve("test.txt").toFile();
      Files.writeString(from.toPath(), "content");
      File zipped = new File(from.getAbsolutePath() + ".zip");

      new HousekeepFilesBlf().execute(form(zipRecord(from.getAbsolutePath())));

      assertThat(zipped).exists();
      assertThat(from).exists();
    }
  }

  @Nested
  @DisplayName("zip with destination specified")
  class ZipWithDestination {

    private HousekeepFilesTaskRecord zipRecord(String srcPath, String destPath,
        String isDestPathDir, String actionForDestFileExists) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "ZIP_REMAIN_ORIG", null, srcPath,
          "FALSE", "0", "IGNORE", destPath, isDestPathDir, "FALSE", actionForDestFileExists);
    }

    @Test
    @DisplayName("destination is a directory: creates {destDir}/{srcName}.zip")
    void destinationIsDirectory() throws Exception {
      File from = tempDir.resolve("test.txt").toFile();
      Files.writeString(from.toPath(), "content");
      File destDir = tempDir.resolve("destDir").toFile();
      destDir.mkdir();

      new HousekeepFilesBlf().execute(
          form(zipRecord(from.getAbsolutePath(), destDir.getAbsolutePath(), "TRUE", "IGNORE")));

      assertThat(destDir.toPath().resolve("test.txt.zip")).exists();
    }

    @Test
    @DisplayName("destination is a file: creates the zip exactly there")
    void destinationIsFile() throws Exception {
      File from = tempDir.resolve("test.txt").toFile();
      Files.writeString(from.toPath(), "content");
      File dest = tempDir.resolve("archive.zip").toFile();

      new HousekeepFilesBlf().execute(
          form(zipRecord(from.getAbsolutePath(), dest.getAbsolutePath(), "FALSE", "IGNORE")));

      assertThat(dest).exists();
    }

    @Test
    @DisplayName("existing destination zip, actionForDestFileExists=ERROR: MSG_ERR_DEST_PATH_EXISTS")
    void existingDestinationError() throws Exception {
      File from = tempDir.resolve("test.txt").toFile();
      Files.writeString(from.toPath(), "content");
      File dest = tempDir.resolve("archive.zip").toFile();
      Files.writeString(dest.toPath(), "pre-existing");

      assertSingleBusinessViolation(
          form(zipRecord(from.getAbsolutePath(), dest.getAbsolutePath(), "FALSE", "ERROR")),
          "MSG_ERR_DEST_PATH_EXISTS");
    }

    @Test
    @DisplayName("existing destination zip, actionForDestFileExists=WARN: warn mail sent, "
        + "existing zip left unchanged")
    void existingDestinationWarn() throws Exception {
      File from = tempDir.resolve("test.txt").toFile();
      Files.writeString(from.toPath(), "content");
      File dest = tempDir.resolve("archive.zip").toFile();
      Files.writeString(dest.toPath(), "pre-existing");

      AtomicBoolean warnMailSent = new AtomicBoolean();
      new HousekeepFilesBlf(warnMailDetectingBl(warnMailSent)).execute(
          form(zipRecord(from.getAbsolutePath(), dest.getAbsolutePath(), "FALSE", "WARN")));

      assertThat(warnMailSent).isTrue();
      assertThat(dest).hasContent("pre-existing");
    }

    @Test
    @DisplayName("existing destination zip, actionForDestFileExists=IGNORE: no violation, "
        + "existing zip left unchanged")
    void existingDestinationIgnore() throws Exception {
      File from = tempDir.resolve("test.txt").toFile();
      Files.writeString(from.toPath(), "content");
      File dest = tempDir.resolve("archive.zip").toFile();
      Files.writeString(dest.toPath(), "pre-existing");

      new HousekeepFilesBlf().execute(
          form(zipRecord(from.getAbsolutePath(), dest.getAbsolutePath(), "FALSE", "IGNORE")));

      assertThat(dest).hasContent("pre-existing");
    }
  }

  @Nested
  @DisplayName("zipping a directory")
  class ZippingDirectory {

    @Test
    @DisplayName("zips the directory's contents, keeping the original")
    void zipsDirectory() throws Exception {
      File srcDir = tempDir.resolve("srcDir").toFile();
      srcDir.mkdir();
      Files.writeString(srcDir.toPath().resolve("child.txt"), "content");

      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "ZIP_REMAIN_ORIG", null, srcDir.getAbsolutePath(), "TRUE", "0", "IGNORE", null, null,
          null, null);

      new HousekeepFilesBlf().execute(form(rec));

      assertThat(new File(srcDir.getAbsolutePath() + ".zip")).exists();
      assertThat(srcDir).exists();
    }
  }
}

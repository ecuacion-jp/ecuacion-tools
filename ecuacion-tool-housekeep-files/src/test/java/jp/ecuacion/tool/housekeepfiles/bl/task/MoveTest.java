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

/** Tests for {@link Move}, executed through {@link HousekeepFilesBlf}. */
@SuppressWarnings("null")
@DisplayName("Move")
class MoveTest {

  @TempDir
  Path tempDir;

  private HousekeepFilesForm form(HousekeepFilesTaskRecord taskRec) {
    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);

    return form;
  }

  @Nested
  @DisplayName("file move")
  class FileMove {

    private HousekeepFilesTaskRecord moveRecord(String srcPath, String destPath,
        String srcPathWaitDays) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE", null, srcPath, "FALSE",
          srcPathWaitDays, "IGNORE", destPath, "FALSE", "TRUE", "IGNORE");
    }

    @Test
    @DisplayName("moves a file whose wait-days condition (0 days) is already satisfied")
    void movesWhenWaitDaysIsZero() throws Exception {
      Path from = tempDir.resolve("from.txt");
      Files.writeString(from, "content");
      Path to = tempDir.resolve("to.txt");

      new HousekeepFilesBlf().execute(form(moveRecord(from.toString(), to.toString(), "0")));

      assertThat(from).doesNotExist();
      assertThat(to).exists().hasContent("content");
    }

    @Test
    @DisplayName("skips a file whose last-modified time hasn't passed the wait-days condition")
    void skipsWhenWaitDaysNotElapsed() throws Exception {
      Path from = tempDir.resolve("from.txt");
      Files.writeString(from, "content");
      Path to = tempDir.resolve("to.txt");

      // A large wait-days value can never be satisfied by a file created moments ago.
      new HousekeepFilesBlf().execute(form(moveRecord(from.toString(), to.toString(), "100")));

      assertThat(from).exists();
      assertThat(to).doesNotExist();
    }
  }

  @Nested
  @DisplayName("directory move")
  class DirectoryMove {

    @Test
    @DisplayName("moves a directory (with contents) under the destination directory, named "
        + "after the source directory")
    void movesDirectoryUnderDest() throws Exception {
      File srcDir = tempDir.resolve("srcDir").toFile();
      srcDir.mkdir();
      Files.writeString(srcDir.toPath().resolve("child.txt"), "child-content");
      File destDir = tempDir.resolve("destDir").toFile();
      destDir.mkdir();

      HousekeepFilesTaskRecord rec =
          new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE", null,
              srcDir.getAbsolutePath(), "TRUE", "0", "IGNORE", destDir.getAbsolutePath(), "TRUE",
              "FALSE", "IGNORE");

      new HousekeepFilesBlf().execute(form(rec));

      assertThat(srcDir).doesNotExist();
      assertThat(destDir.toPath().resolve("srcDir").resolve("child.txt")).hasContent(
          "child-content");
    }

    @Test
    @DisplayName("source directory contains a symbolic link: MSG_ERR_SRC_PATH_CONTAINS_SYMLINK, "
        + "source left in place")
    void srcDirContainsSymbolicLink() throws Exception {
      File srcDir = tempDir.resolve("srcDir").toFile();
      srcDir.mkdir();
      Path linkTarget = tempDir.resolve("outsideTarget.txt");
      Files.writeString(linkTarget, "outside-content");
      Files.createSymbolicLink(srcDir.toPath().resolve("link"), linkTarget);
      File destDir = tempDir.resolve("destDir").toFile();
      destDir.mkdir();

      HousekeepFilesTaskRecord rec =
          new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE", null,
              srcDir.getAbsolutePath(), "TRUE", "0", "IGNORE", destDir.getAbsolutePath(), "TRUE",
              "FALSE", "IGNORE");

      assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form(rec)))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId)
                  .containsExactly("MSG_ERR_SRC_PATH_CONTAINS_SYMLINK"));
      assertThat(srcDir).exists();
      assertThat(destDir.toPath().resolve("srcDir")).doesNotExist();
    }
  }
}

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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ZipDeleteOrig}, executed through {@link HousekeepFilesBlf}.
 *
 * <p>Only the behavior that differs from {@link ZipRemainOrig} is covered here (deleting the
 *     original after zipping); everything else is shared {@link AbstractTaskZip} behavior already
 *     covered by {@link ZipRemainOrigTest}.</p>
 */
@SuppressWarnings("null")
@DisplayName("ZipDeleteOrig ※共通の振る舞いは ZipRemainOrigTest 参照")
class ZipDeleteOrigTest {

  @TempDir
  Path tempDir;

  private HousekeepFilesForm form(HousekeepFilesTaskRecord taskRec) {
    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);

    return form;
  }

  @Nested
  @DisplayName("zip and delete original")
  class ZipAndDeleteOriginal {

    @Test
    @DisplayName("zips a file and deletes it")
    void zipsAndDeletesFile() throws Exception {
      File from = tempDir.resolve("test.txt").toFile();
      Files.writeString(from.toPath(), "content");
      File zipped = new File(from.getAbsolutePath() + ".zip");

      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "ZIP_DELETE_ORIG", null, from.getAbsolutePath(), "FALSE", "0", "IGNORE", null, null,
          null, null);

      new HousekeepFilesBlf().execute(form(rec));

      assertThat(zipped).exists();
      assertThat(from).doesNotExist();
    }

    @Test
    @DisplayName("zips a directory and deletes it")
    void zipsAndDeletesDirectory() throws Exception {
      File srcDir = tempDir.resolve("srcDir").toFile();
      srcDir.mkdir();
      Files.writeString(srcDir.toPath().resolve("child.txt"), "content");

      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "ZIP_DELETE_ORIG", null, srcDir.getAbsolutePath(), "TRUE", "0", "IGNORE", null, null,
          null, null);

      new HousekeepFilesBlf().execute(form(rec));

      assertThat(new File(srcDir.getAbsolutePath() + ".zip")).exists();
      assertThat(srcDir).doesNotExist();
    }
  }
}

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
import jp.ecuacion.tool.housekeepfiles.util.CompressUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link UnzipDeleteOrig}, executed through {@link HousekeepFilesBlf}.
 *
 * <p>Only the behavior that differs from {@link UnzipRemainOrig} is covered here (deleting the
 *     original after unzipping); everything else is shared {@link AbstractTaskUnzip} behavior
 *     already covered by {@link UnzipRemainOrigTest}.</p>
 */
@SuppressWarnings("null")
@DisplayName("UnzipDeleteOrig ※共通の振る舞いは UnzipRemainOrigTest 参照")
class UnzipDeleteOrigTest {

  @TempDir
  Path tempDir;

  private HousekeepFilesForm form(HousekeepFilesTaskRecord taskRec) {
    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);

    return form;
  }

  @Nested
  @DisplayName("unzip and delete original")
  class UnzipAndDeleteOriginal {

    @Test
    @DisplayName("unzips a file and deletes the zip file")
    void unzipsAndDeletesZipFile() throws Exception {
      File dataFile = tempDir.resolve("data.txt").toFile();
      Files.writeString(dataFile.toPath(), "content");
      File zipFile = tempDir.resolve("archive.zip").toFile();
      new CompressUtil().zipFile(dataFile.getAbsolutePath(), zipFile.getAbsolutePath());
      dataFile.delete();

      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "UNZIP_DELETE_ORIG", null, zipFile.getAbsolutePath(), "FALSE", "0", "IGNORE", null, null,
          null, null);

      new HousekeepFilesBlf().execute(form(rec));

      assertThat(tempDir.resolve("data.txt")).hasContent("content");
      assertThat(zipFile).doesNotExist();
    }
  }
}

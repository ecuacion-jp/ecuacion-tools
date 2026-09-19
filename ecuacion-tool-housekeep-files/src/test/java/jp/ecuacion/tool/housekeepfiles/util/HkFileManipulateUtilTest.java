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
package jp.ecuacion.tool.housekeepfiles.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link HkFileManipulateUtil}. */
@SuppressWarnings("null")
@DisplayName("HkFileManipulateUtil")
class HkFileManipulateUtilTest {

  private final HkFileManipulateUtil fmu = new HkFileManipulateUtil();

  private HousekeepFilesTaskRecord recordWithIsDestPathDir(String isDestPathDirEnumName) {
    return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE", null, "aPath", "FALSE",
        "0", "IGNORE", "aPath", isDestPathDirEnumName, "TRUE", "IGNORE");
  }

  @Nested
  @DisplayName("checkIfToOverwrittenFileOrDirExists(): destination is a file")
  class DestinationIsFile {

    @Test
    @DisplayName("returns true when the destination file exists")
    void destExists(@TempDir Path tempDir) throws Exception {
      File dest = tempDir.resolve("dest.txt").toFile();
      dest.createNewFile();

      assertThat(fmu.checkIfToOverwrittenFileOrDirExists(recordWithIsDestPathDir("FALSE"),
          "/any/from.txt", dest.getAbsolutePath())).isTrue();
    }

    @Test
    @DisplayName("returns false when the destination file does not exist")
    void destNotExist(@TempDir Path tempDir) {
      File dest = tempDir.resolve("dest.txt").toFile();

      assertThat(fmu.checkIfToOverwrittenFileOrDirExists(recordWithIsDestPathDir("FALSE"),
          "/any/from.txt", dest.getAbsolutePath())).isFalse();
    }
  }

  @Nested
  @DisplayName("checkIfToOverwrittenFileOrDirExists(): destination is a directory")
  class DestinationIsDirectory {

    @Test
    @DisplayName("returns true when a same-named entry exists under the destination directory")
    void sameNamedEntryExists(@TempDir Path tempDir) throws Exception {
      File destDir = tempDir.resolve("destDir").toFile();
      destDir.mkdir();
      new File(destDir, "from.txt").createNewFile();

      assertThat(fmu.checkIfToOverwrittenFileOrDirExists(recordWithIsDestPathDir("TRUE"),
          "/any/from.txt", destDir.getAbsolutePath())).isTrue();
    }

    @Test
    @DisplayName("returns false when no same-named entry exists under the destination directory")
    void noSameNamedEntry(@TempDir Path tempDir) {
      File destDir = tempDir.resolve("destDir").toFile();
      destDir.mkdir();

      assertThat(fmu.checkIfToOverwrittenFileOrDirExists(recordWithIsDestPathDir("TRUE"),
          "/any/from.txt", destDir.getAbsolutePath())).isFalse();
    }

    @Test
    @DisplayName("matches by the source's basename regardless of its own directory")
    void matchesBySourceBasename(@TempDir Path tempDir) throws Exception {
      File destDir = tempDir.resolve("destDir").toFile();
      destDir.mkdir();
      new File(destDir, "sameName.txt").createNewFile();

      assertThat(fmu.checkIfToOverwrittenFileOrDirExists(recordWithIsDestPathDir("TRUE"),
          "/some/other/dir/sameName.txt", destDir.getAbsolutePath())).isTrue();
    }
  }
}

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link CompressUtil}. */
@SuppressWarnings("null")
@DisplayName("CompressUtil")
class CompressUtilTest {

  private final CompressUtil cu = new CompressUtil();

  @Nested
  @DisplayName("zipFile() / unzip()")
  class ZipFileAndUnzip {

    @Test
    @DisplayName("a single file round-trips through zip and unzip")
    void roundTrip(@TempDir Path tempDir) throws IOException {
      Path src = tempDir.resolve("src.txt");
      Files.writeString(src, "hello");
      Path zip = tempDir.resolve("out.zip");
      Path unzipDir = tempDir.resolve("unzipped");
      Files.createDirectory(unzipDir);

      cu.zipFile(src.toString(), zip.toString());

      assertThat(zip).exists();

      cu.unzip(zip.toString(), unzipDir.toString());

      assertThat(unzipDir.resolve("src.txt")).exists().hasContent("hello");
    }
  }

  @Nested
  @DisplayName("zipFileList()")
  class ZipFileList {

    @Test
    @DisplayName("archives multiple files into a single zip")
    void multipleFiles(@TempDir Path tempDir) throws IOException {
      Path file1 = tempDir.resolve("a.txt");
      Path file2 = tempDir.resolve("b.txt");
      Files.writeString(file1, "aaa");
      Files.writeString(file2, "bbb");
      Path zip = tempDir.resolve("out.zip");
      Path unzipDir = tempDir.resolve("unzipped");
      Files.createDirectory(unzipDir);

      cu.zipFileList(List.of(file1.toString(), file2.toString()), zip.toString());
      cu.unzip(zip.toString(), unzipDir.toString());

      assertThat(unzipDir.resolve("a.txt")).hasContent("aaa");
      assertThat(unzipDir.resolve("b.txt")).hasContent("bbb");
    }
  }

  @Nested
  @DisplayName("zipDirectory() / unzip()")
  class ZipDirectoryAndUnzip {

    @Test
    @DisplayName("a directory with nested files and subdirectories round-trips, entries prefixed "
        + "with the source directory's own name")
    void roundTrip(@TempDir Path tempDir) throws IOException {
      // Entry names are computed relative to the zip file's own parent directory (see
      // CompressUtil#archive), so the zip file must live alongside srcDir (same parent) for the
      // resulting entries to be prefixed with "srcDir/" rather than an unrelated absolute path.
      Path srcDir = tempDir.resolve("srcDir");
      Files.createDirectory(srcDir);
      Files.writeString(srcDir.resolve("top.txt"), "top");
      Path subDir = srcDir.resolve("sub");
      Files.createDirectory(subDir);
      Files.writeString(subDir.resolve("nested.txt"), "nested");

      Path zip = tempDir.resolve("out.zip");
      Path unzipDir = tempDir.resolve("unzipped");
      Files.createDirectory(unzipDir);

      cu.zipDirectory(srcDir.toString(), zip.toString());
      cu.unzip(zip.toString(), unzipDir.toString());

      assertThat(unzipDir.resolve("srcDir").resolve("top.txt")).hasContent("top");
      assertThat(unzipDir.resolve("srcDir").resolve("sub").resolve("nested.txt"))
          .hasContent("nested");
    }
  }

  @Nested
  @DisplayName("unzip(): zip slip protection")
  class UnzipZipSlipProtection {

    @Test
    @DisplayName("an entry name escaping the target directory via '../' is rejected")
    void rejectsEntryOutsideTargetDir(@TempDir Path tempDir) throws IOException {
      Path zip = tempDir.resolve("evil.zip");
      // A file outside of "outDir" (a sibling directly under tempDir) that the malicious entry
      // targets by relative path.
      Path outsideMarker = tempDir.resolve("evil.txt");

      try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip.toFile()))) {
        ZipEntry entry = new ZipEntry("../evil.txt");
        out.putNextEntry(entry);
        out.write("pwned".getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
      }

      Path outDir = tempDir.resolve("outDir");
      Files.createDirectory(outDir);

      assertThatThrownBy(() -> cu.unzip(zip.toString(), outDir.toString()))
          .isInstanceOf(IOException.class).hasMessageContaining("zip slip");

      assertThat(outsideMarker).doesNotExist();
    }
  }
}

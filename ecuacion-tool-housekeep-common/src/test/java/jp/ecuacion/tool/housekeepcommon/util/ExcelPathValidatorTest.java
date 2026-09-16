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
package jp.ecuacion.tool.housekeepcommon.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.constraints.NotEmpty;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.validation.constraints.FileExists;
import jp.ecuacion.lib.validation.constraints.FileExtension;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ExcelPathValidator")
class ExcelPathValidatorTest {

  /** Mirrors the {@code excelPath} field every real caller (a tasklet) carries. */
  static class Caller {
    @NotEmpty
    @FileExists
    @FileExtension(".xlsx")
    private final @Nullable String excelPath;

    Caller(@Nullable String excelPath) {
      this.excelPath = excelPath;
    }
  }

  @Test
  @DisplayName("a null excelPath fails @NotEmpty validation")
  void nullExcelPathFails() {
    assertThatThrownBy(() -> ExcelPathValidator.validate(new Caller(null), null))
        .isInstanceOf(ViolationException.class);
  }

  @Test
  @DisplayName("a path pointing to a non-existent file fails @FileExists validation")
  void nonExistentFileFails() {
    String path = "/no/such/file.xlsx";

    assertThatThrownBy(() -> ExcelPathValidator.validate(new Caller(path), path))
        .isInstanceOf(ViolationException.class);
  }

  @Test
  @DisplayName("a non-.xlsx extension fails @FileExtension validation")
  void wrongExtensionFails(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("settings.txt");
    Files.writeString(file, "not an excel file");
    String path = file.toString();

    assertThatThrownBy(() -> ExcelPathValidator.validate(new Caller(path), path))
        .isInstanceOf(ViolationException.class);
  }

  @Test
  @DisplayName("a .xlsx file that isn't a real workbook raises MSG_ERR_EXCEL_PATH_CANNOT_OPEN")
  void unopenableFileFails(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("corrupt.xlsx");
    Files.writeString(file, "not actually an xlsx file");
    String path = file.toString();

    assertThatExceptionOfType(ViolationException.class)
        .isThrownBy(() -> ExcelPathValidator.validate(new Caller(path), path))
        .satisfies(ex -> assertThat(ex.getViolations().getBusinessViolations())
            .extracting(BusinessViolation::getMessageId)
            .containsExactly("MSG_ERR_EXCEL_PATH_CANNOT_OPEN"));
  }

  @Test
  @DisplayName("a genuinely password-encrypted .xlsx file also raises "
      + "MSG_ERR_EXCEL_PATH_CANNOT_OPEN, via EncryptedDocumentException rather than the plain "
      + "corrupt-file IOException path exercised above")
  void encryptedFileFails(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("encrypted.xlsx");

    byte[] plainXlsxBytes;
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      wb.createSheet("Sheet1");
      wb.write(baos);
      plainXlsxBytes = baos.toByteArray();
    }

    try (POIFSFileSystem fs = new POIFSFileSystem()) {
      EncryptionInfo info = new EncryptionInfo(EncryptionMode.agile);
      Encryptor encryptor = info.getEncryptor();
      encryptor.confirmPassword("test-password");

      try (OutputStream os = encryptor.getDataStream(fs)) {
        os.write(plainXlsxBytes);
      }

      try (OutputStream fos = Files.newOutputStream(file)) {
        fs.writeFilesystem(fos);
      }
    }

    String path = file.toString();

    assertThatExceptionOfType(ViolationException.class)
        .isThrownBy(() -> ExcelPathValidator.validate(new Caller(path), path))
        .satisfies(ex -> assertThat(ex.getViolations().getBusinessViolations())
            .extracting(BusinessViolation::getMessageId)
            .containsExactly("MSG_ERR_EXCEL_PATH_CANNOT_OPEN"));
  }
}

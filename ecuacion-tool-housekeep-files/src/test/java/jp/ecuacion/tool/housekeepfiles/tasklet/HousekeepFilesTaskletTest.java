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
package jp.ecuacion.tool.housekeepfiles.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;

/** Tests for {@link HousekeepFilesTasklet}. */
@DisplayName("HousekeepFilesTasklet")
class HousekeepFilesTaskletTest {

  @Nested
  @DisplayName("execute(): excel path validation")
  class ExcelPathValidation {

    @SuppressWarnings("null")
    @Test
    @DisplayName("a null excelPath fails @NotEmpty validation")
    void nullExcelPathFails() {
      assertThatThrownBy(() -> new HousekeepFilesTasklet(null).execute(
          mock(StepContribution.class), mock(ChunkContext.class)))
              .isInstanceOf(ViolationException.class);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("an empty excelPath fails @NotEmpty validation")
    void emptyExcelPathFails() {
      assertThatThrownBy(() -> new HousekeepFilesTasklet("").execute(
          mock(StepContribution.class), mock(ChunkContext.class)))
              .isInstanceOf(ViolationException.class);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("a path pointing to a non-existent file fails @FileExists validation")
    void nonExistentFileFails() {
      assertThatThrownBy(() -> new HousekeepFilesTasklet("/no/such/file.xlsx")
          .execute(mock(StepContribution.class), mock(ChunkContext.class)))
              .isInstanceOf(ViolationException.class);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("a non-.xlsx extension fails @FileExtension validation")
    void wrongExtensionFails(@TempDir Path tempDir) throws IOException {
      Path file = tempDir.resolve("settings.txt");
      Files.writeString(file, "not an excel file");

      assertThatThrownBy(() -> new HousekeepFilesTasklet(file.toString())
          .execute(mock(StepContribution.class), mock(ChunkContext.class)))
              .isInstanceOf(ViolationException.class);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("a .xlsx file that isn't a real workbook raises MSG_ERR_EXCEL_PATH_CANNOT_OPEN")
    void unopenableFileFails(@TempDir Path tempDir) throws IOException {
      Path file = tempDir.resolve("corrupt.xlsx");
      Files.writeString(file, "not actually an xlsx file");

      assertThatExceptionOfType(ViolationException.class)
          .isThrownBy(() -> new HousekeepFilesTasklet(file.toString())
              .execute(mock(StepContribution.class), mock(ChunkContext.class)))
          .satisfies(ex -> assertThat(ex.getViolations().getBusinessViolations())
              .extracting(BusinessViolation::getMessageId)
              .containsExactly("MSG_ERR_EXCEL_PATH_CANNOT_OPEN"));
    }
  }
}

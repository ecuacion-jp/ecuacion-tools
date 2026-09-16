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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.tool.housekeepfiles.constant.Constants;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.util.LangExcelUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.mock.env.MockEnvironment;

/** Tests for {@link HousekeepFilesTasklet}. */
@SuppressWarnings("null")
@DisplayName("HousekeepFilesTasklet")
class HousekeepFilesTaskletTest {

  /**
   * Writes a settings excel file whose sheet names and header labels are localized to "en", the
   * same way {@link HousekeepFilesTasklet} (via
   * {@link jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm}) localizes them when
   * reading it back from the "locale" row of the "Info" sheet. Exercises the real Excel-reading
   * path end to end (unlike the other tests here, which only cover pre-read validation),
   * including {@link jp.ecuacion.tool.housekeepfiles.reader.ExcelInfoListReader}'s ja-then-en
   * header-label fallback for the "Info" sheet.
   */
  private static Path buildExcelFile(List<String @Nullable []> taskRows) throws IOException {
    LangExcelUtil lang = new LangExcelUtil(java.util.Locale.of("en"));

    try (XSSFWorkbook wb = new XSSFWorkbook()) {
      writeSheet(wb, "Info", new String[] {"item", "value"},
          List.<String @Nullable []>of(new String[] {"locale", "en"},
              new String[] {"format-version", "2.0.0"}));
      writeSheet(wb, lang.get(LangExcelUtil.TASK_SETTINGS),
          lang.getHeaderLabels(HousekeepFilesTaskRecord.HEADER_LABEL_KEYS), taskRows);
      writeSheet(wb, lang.get(LangExcelUtil.SERVER_AUTH_SETTINGS),
          lang.getHeaderLabels(HousekeepFilesAuthRecord.HEADER_LABEL_KEYS), List.of());

      Path path = Files.createTempFile("housekeep-files-test-", ".xlsx");
      try (OutputStream os = Files.newOutputStream(path)) {
        wb.write(os);
      }
      return path;
    }
  }

  private static void writeSheet(XSSFWorkbook wb, String sheetName, String[] headers,
      List<String @Nullable []> rows) {
    Sheet sheet = wb.createSheet(sheetName);

    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      headerRow.createCell(i).setCellValue(headers[i]);
    }

    int rowNum = 1;
    for (String[] row : rows) {
      Row excelRow = sheet.createRow(rowNum++);
      for (int i = 0; i < row.length; i++) {
        @Nullable String value = row[i];
        if (value != null) {
          excelRow.createCell(i).setCellValue(value);
        }
      }
    }
  }

  @Nested
  @DisplayName("execute(): a real settings excel")
  class ExecuteWithRealExcel {

    @Test
    @DisplayName("reads the excel end to end and runs the task it describes")
    void readsAndRunsTask(@TempDir Path tempDir) throws Exception {
      Path destDir = tempDir.resolve("created-dir");

      // Columns, in order: taskId, taskName, taskPtnDisplay, taskPtn, remoteServer, srcPath,
      // isSrcPathDir, srcPathWaitDays, actionForNoSrcPath, destPath, isDestPathDir,
      // overwriteDestPath, actionForDestPathExists. srcPath-related columns are left blank since
      // CREATE_DIR prohibits them.
      String @Nullable [] taskRow = new String[] {"01", "task01", "Create Directory",
          "CREATE_DIR", null, null, null, null, null, destDir.toString(), "TRUE", "FALSE",
          "IGNORE"};
      Path excelFile = buildExcelFile(java.util.Collections.singletonList(taskRow));

      RepeatStatus status = new HousekeepFilesTasklet(excelFile.toString())
          .execute(mock(StepContribution.class), mock(ChunkContext.class));

      assertThat(status).isEqualTo(RepeatStatus.FINISHED);
      assertThat(destDir).isDirectory();
    }
  }

  /** The ROOT logger's level as found before {@link #attachLogCapture()} last changed it. */
  private static ch.qos.logback.classic.@Nullable Level levelBeforeCapture;

  /**
   * Attaches a started {@link ListAppender} to the ROOT logger, capturing every log event
   * emitted anywhere during the test. Also lowers the ROOT logger's level to {@code DEBUG} for
   * the duration of the capture - the previous level is restored by {@link #detachLogCapture}.
   */
  private static ListAppender<ILoggingEvent> attachLogCapture() {
    ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
            ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

    levelBeforeCapture = rootLogger.getLevel();
    rootLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);

    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.setContext(rootLogger.getLoggerContext());
    appender.start();
    rootLogger.addAppender(appender);

    return appender;
  }

  /**
   * Detaches and stops an appender previously returned by {@link #attachLogCapture()}, and
   * restores the ROOT logger's level to what it was before that call.
   */
  private static void detachLogCapture(ListAppender<ILoggingEvent> appender) {
    ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
            ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    rootLogger.detachAppender(appender);
    appender.stop();
    rootLogger.setLevel(levelBeforeCapture);
  }

  @Nested
  @DisplayName("execute(): target system name")
  class TargetSystemName {

    @SuppressWarnings("null")
    @Test
    @DisplayName("when set, is logged as an additional startup info line, alongside the "
        + "unmodified start/finish messages")
    void loggedAsStartupInfoLineWhenSet(@TempDir Path tempDir) throws Exception {
      Path destDir = tempDir.resolve("created-dir");
      String @Nullable [] taskRow = new String[] {"01", "task01", "Create Directory",
          "CREATE_DIR", null, null, null, null, null, destDir.toString(), "TRUE", "FALSE",
          "IGNORE"};
      Path excelFile = buildExcelFile(java.util.Collections.singletonList(taskRow));

      MockEnvironment env = new MockEnvironment();
      env.setProperty(Constants.PROP_TARGET_SYSTEM_NAME, "my-system");
      HousekeepFilesTasklet tasklet = new HousekeepFilesTasklet(excelFile.toString());
      tasklet.env = env;

      ListAppender<ILoggingEvent> appender = attachLogCapture();
      try {
        tasklet.execute(mock(StepContribution.class), mock(ChunkContext.class));

        List<String> messages =
            appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).anyMatch(msg -> msg.equals("housekeep-files started."));
        assertThat(messages)
            .anyMatch(msg -> msg.equals("- Target System Name  : my-system"));
        assertThat(messages)
            .anyMatch(msg -> msg.equals("housekeep-files finished successfully."));
      } finally {
        detachLogCapture(appender);
      }
    }

    @Test
    @DisplayName("when unset, the startup info line is omitted")
    void omittedFromStartupInfoWhenUnset(@TempDir Path tempDir) throws Exception {
      Path destDir = tempDir.resolve("created-dir");
      String @Nullable [] taskRow = new String[] {"01", "task01", "Create Directory",
          "CREATE_DIR", null, null, null, null, null, destDir.toString(), "TRUE", "FALSE",
          "IGNORE"};
      Path excelFile = buildExcelFile(java.util.Collections.singletonList(taskRow));

      ListAppender<ILoggingEvent> appender = attachLogCapture();
      try {
        new HousekeepFilesTasklet(excelFile.toString())
            .execute(mock(StepContribution.class), mock(ChunkContext.class));

        List<String> messages =
            appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).anyMatch(msg -> msg.equals("housekeep-files started."));
        assertThat(messages).noneMatch(msg -> msg.contains("Target System Name"));
        assertThat(messages)
            .anyMatch(msg -> msg.equals("housekeep-files finished successfully."));
      } finally {
        detachLogCapture(appender);
      }
    }
  }

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

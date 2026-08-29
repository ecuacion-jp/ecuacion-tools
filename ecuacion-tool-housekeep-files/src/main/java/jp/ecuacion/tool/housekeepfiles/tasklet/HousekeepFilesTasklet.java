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

import jakarta.validation.Validation;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.Objects;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.validation.constraints.FileExists;
import jp.ecuacion.lib.validation.constraints.FileExtension;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.constant.Constants;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.util.excel.util.ExcelReadUtil;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Housekeeps files.
 */
@Component
public class HousekeepFilesTasklet implements Tasklet {

  public static final String PROP_EXCEL_PATH = "jp.ecuacion.tool.housekeep-files.excel-path";

  HousekeepFilesBlf blf = new HousekeepFilesBlf();
  @Nullable
  HousekeepFilesForm form;

  @NotEmpty
  @FileExists
  @FileExtension(".xlsx")
  private final @Nullable String excelPath;

  // Not set when this tasklet is instantiated directly (e.g. in tests) instead of through Spring.
  @Autowired(required = false)
  @Nullable
  Environment env;

  /**
   * Creates the tasklet, reading the excel file path from the {@link #PROP_EXCEL_PATH} property.
   *
   * @param excelPath the excel file path, or {@code null} if unset
   */
  public HousekeepFilesTasklet(
      @Value("${" + PROP_EXCEL_PATH + ":#{null}}") @Nullable String excelPath) {
    this.excelPath = excelPath;
  }

  /**
   * Executes housekeeping files.
   */
  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {

    String excelPath = validateExcelPath();

    // AbstractTaskSftp is instantiated by reflection outside of Spring's DI, so it cannot read
    // this property from the Environment directly. Bridge it through a JVM system property here,
    // which also makes values set in application.properties / application_profile.properties
    // effective, not only "-D" arguments. Left untouched when this tasklet is instantiated
    // directly without Spring (e.g. in tests), in which case only "-D" is honored.
    if (env != null && Objects.requireNonNull(env)
        .containsProperty(Constants.PROP_SFTP_STRICT_HOST_KEY_CHECKING)) {
      System.setProperty(Constants.PROP_SFTP_STRICT_HOST_KEY_CHECKING, Objects.requireNonNull(
          Objects.requireNonNull(env).getProperty(Constants.PROP_SFTP_STRICT_HOST_KEY_CHECKING)));
    }

    form = getFormFromExcel(excelPath);

    blf.execute(Objects.requireNonNull(form));

    return RepeatStatus.FINISHED;
  }

  private String validateExcelPath() {
    new Violations().addAll(Validation.buildDefaultValidatorFactory().getValidator().validate(this))
        .messageParameters(Violations.newMessageParameters().isMessageWithItemName(true))
        .throwIfAny();

    String nonnullExcelPath = Objects.requireNonNull(excelPath);

    try (Workbook workbook = ExcelReadUtil.openForRead(nonnullExcelPath)) {
      // Only verifying the file can be opened as an excel file here.
      // Its content is read later.
    } catch (EncryptedDocumentException | IOException e) {
      new Violations()
          .add(new BusinessViolation("MSG_ERR_EXCEL_PATH_CANNOT_OPEN", nonnullExcelPath))
          .throwIfAny();
    }

    return nonnullExcelPath;
  }

  /**
   * It's package scope for unit-test.
   */
  HousekeepFilesForm getFormFromExcel(String excelPath) {
    return new HousekeepFilesForm(excelPath);
  }
}

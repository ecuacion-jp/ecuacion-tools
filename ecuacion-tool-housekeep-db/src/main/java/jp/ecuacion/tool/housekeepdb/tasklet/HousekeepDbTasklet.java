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
package jp.ecuacion.tool.housekeepdb.tasklet;

import jakarta.validation.Validation;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.lib.validation.constraints.FileExists;
import jp.ecuacion.lib.validation.constraints.FileExtension;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.DbConnectionInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import jp.ecuacion.tool.housekeepdb.bl.HousekeepConfigLoader;
import jp.ecuacion.tool.housekeepdb.bl.HousekeepMainTableDeleter;
import jp.ecuacion.util.excel.util.ExcelReadUtil;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Executes housekeeping DB.
 *
 * <p>Owns the excel path / property validation and the per-task loop; reading and linking the
 *     excel settings is delegated to {@link HousekeepConfigLoader}, and deleting the records of
 *     one task is delegated to {@link HousekeepMainTableDeleter}.</p>
 */
@Component
public class HousekeepDbTasklet implements Tasklet {

  public static final String PROP_EXCEL_PATH = "jp.ecuacion.tool.housekeep-db.excel-path";
  public static final String PROP_MAX_SELECT_LINES =
      "jp.ecuacion.tool.housekeep-db.max-select-lines";

  private DetailLogger detailLogger = new DetailLogger(this);
  @NotEmpty
  @FileExists
  @FileExtension(".xlsx")
  private final @Nullable String excelPath;
  private final int maxSelectLines;

  /**
   * Creates the tasklet, reading the excel file path and the per-commit row limit from the
   * {@link #PROP_EXCEL_PATH} / {@link #PROP_MAX_SELECT_LINES} properties.
   *
   * @param excelPath the excel file path, or {@code null} if unset
   * @param maxSelectLines the number of rows selected and committed per loop iteration
   */
  public HousekeepDbTasklet(@Value("${" + PROP_EXCEL_PATH + ":#{null}}") @Nullable String excelPath,
      @Value("${" + PROP_MAX_SELECT_LINES + ":1000}") int maxSelectLines) {
    this.excelPath = excelPath;
    this.maxSelectLines = maxSelectLines;
  }

  /**
   * Executes the procedure.
   */
  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {

    String excelPath = validateExcelPath();

    detailLogger.info("===============");
    detailLogger.info("housekeep-db start.");
    detailLogger.info("Excel File Path     : " + excelPath);

    HousekeepConfigLoader configLoader = new HousekeepConfigLoader();
    configLoader.load(excelPath);

    Map<String, String> infoMap = configLoader.getInfoMap();
    detailLogger.info("Format Excel Version: " + infoMap.get("format-version"));
    detailLogger.info("Locale              : " + infoMap.get("locale"));

    Map<String, DbConnectionInfoBean> dbConnectionInfoMap = configLoader.getDbConnectionInfoMap();
    List<HousekeepInfoBean> housekeepInfoList = configLoader.getHousekeepInfoList();

    if (housekeepInfoList.isEmpty()) {
      detailLogger.warn("\"Housekeep DB Settings\" sheet has no data rows. Nothing to do.");
    }

    HousekeepMainTableDeleter mainTableDeleter =
        new HousekeepMainTableDeleter(detailLogger, maxSelectLines);

    for (HousekeepInfoBean info : housekeepInfoList) {
      detailLogger.info("-----");
      detailLogger.info("task start : " + info.getTaskId());

      mainTableDeleter.execute(dbConnectionInfoMap, info);

      detailLogger.info("task finish: " + info.getTaskId());
    }

    detailLogger.info("-----");
    detailLogger.info("housekeep-db finished successfully.");

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
}

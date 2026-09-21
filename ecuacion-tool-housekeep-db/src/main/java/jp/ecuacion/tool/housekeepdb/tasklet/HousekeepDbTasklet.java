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

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.validation.constraints.FileExists;
import jp.ecuacion.lib.validation.constraints.FileExtension;
import jp.ecuacion.splib.core.util.SplibLogUtil;
import jp.ecuacion.tool.housekeepcommon.util.ExcelPathValidator;
import jp.ecuacion.tool.housekeepcommon.util.HousekeepLogUtil;
import jp.ecuacion.tool.housekeepcommon.util.HousekeepPropKeys;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.DbConnectionInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import jp.ecuacion.tool.housekeepdb.bl.HousekeepConfigLoader;
import jp.ecuacion.tool.housekeepdb.bl.HousekeepMainTableDeleter;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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

  private static final String TOOL_NAME = "housekeep-db";

  public static final String PROP_EXCEL_PATH =
      HousekeepPropKeys.PREFIX + TOOL_NAME + HousekeepPropKeys.SUFFIX_EXCEL_PATH;
  public static final String PROP_MAX_SELECT_LINES =
      "jp.ecuacion.tool.housekeep-db.max-select-lines";

  /**
   * Optional name of the system whose DB records this housekeeping instance manages, shown in
   * the startup log. When unset, that part of the log is simply omitted.
   */
  public static final String PROP_TARGET_SYSTEM_NAME =
      HousekeepPropKeys.PREFIX + TOOL_NAME + HousekeepPropKeys.SUFFIX_TARGET_SYSTEM_NAME;

  private DetailLogger detailLogger = new DetailLogger(this);
  @NotEmpty
  @FileExists
  @FileExtension(".xlsx")
  private final @Nullable String excelPath;
  private final int maxSelectLines;

  private final Environment env;

  /**
   * Creates the tasklet, reading the excel file path and the per-commit row limit from the
   * {@link #PROP_EXCEL_PATH} / {@link #PROP_MAX_SELECT_LINES} properties.
   *
   * @param excelPath the excel file path, or {@code null} if unset
   * @param maxSelectLines the number of rows selected and committed per loop iteration
   * @param env the Spring {@link Environment}, used to resolve the optional target system name
   *     and any {@code ${VAR}} references in DB connection passwords
   */
  public HousekeepDbTasklet(@Value("${" + PROP_EXCEL_PATH + ":#{null}}") @Nullable String excelPath,
      @Value("${" + PROP_MAX_SELECT_LINES + ":1000}") int maxSelectLines, Environment env) {
    this.excelPath = excelPath;
    this.maxSelectLines = maxSelectLines;
    this.env = env;
  }

  /**
   * Executes the procedure.
   */
  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {

    String excelPath = validateExcelPath();

    @Nullable String targetSystemName = env.getProperty(PROP_TARGET_SYSTEM_NAME);

    HousekeepLogUtil.logStarted(detailLogger, TOOL_NAME, excelPath, targetSystemName);

    HousekeepConfigLoader configLoader = new HousekeepConfigLoader();
    configLoader.load(excelPath);

    Map<String, String> infoMap = configLoader.getInfoMap();
    HousekeepLogUtil.logExcelFormatInfo(detailLogger, infoMap.get("format-version"),
        infoMap.get("locale"));

    Map<String, DbConnectionInfoBean> dbConnectionInfoMap = configLoader.getDbConnectionInfoMap();
    List<HousekeepInfoBean> housekeepInfoList = configLoader.getHousekeepInfoList();

    // Resolve ${VAR} references in every DB connection's password up front (fails fast), so the
    // actual secret can be kept out of the settings Excel file and supplied via environment
    // variable instead (e.g. "${DB_PASSWORD}").
    Function<String, String> envVarValueGetter = createEnvVarValueGetter();
    for (DbConnectionInfoBean dbInfo : dbConnectionInfoMap.values()) {
      dbInfo.setEnvVarValueGetter(envVarValueGetter);
    }

    if (housekeepInfoList.isEmpty()) {
      detailLogger.warn("\"Housekeep DB Settings\" sheet has no data rows. Nothing to do.");
    }

    HousekeepMainTableDeleter mainTableDeleter =
        new HousekeepMainTableDeleter(detailLogger, maxSelectLines);

    detailLogger.info("Per-task procedure started.");

    for (HousekeepInfoBean info : housekeepInfoList) {
      SplibLogUtil.info(detailLogger, "Task started  : " + info.getTaskId(), 1);

      mainTableDeleter.execute(dbConnectionInfoMap, info);

      SplibLogUtil.info(detailLogger, "Task finished : " + info.getTaskId(), 1);
    }

    HousekeepLogUtil.logFinishedSuccessfully(detailLogger, TOOL_NAME);

    return RepeatStatus.FINISHED;
  }

  /**
   * Builds the ${VAR} value resolver used to expand DB connection passwords: resolves via
   * {@code env} (application.properties, OS environment variables, JVM system properties,
   * command-line arguments - anything Spring Boot's Environment can resolve). An empty-string
   * property value resolves to {@code null} (i.e. "not found") rather than silently expanding to
   * an empty password.
   *
   * <p>Package-private for unit testing.
   */
  Function<String, String> createEnvVarValueGetter() {
    return key -> {
      String value = env.getProperty(key);
      return StringUtils.isEmpty(value) ? null : value;
    };
  }

  private String validateExcelPath() {
    return ExcelPathValidator.validate(this, excelPath);
  }
}

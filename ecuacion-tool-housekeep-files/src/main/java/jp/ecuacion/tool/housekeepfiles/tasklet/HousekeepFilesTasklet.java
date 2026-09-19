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

import jakarta.validation.constraints.NotEmpty;
import java.util.Objects;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.validation.constraints.FileExists;
import jp.ecuacion.lib.validation.constraints.FileExtension;
import jp.ecuacion.tool.housekeepcommon.util.ExcelPathValidator;
import jp.ecuacion.tool.housekeepcommon.util.HousekeepLogUtil;
import jp.ecuacion.tool.housekeepcommon.util.HousekeepPropKeys;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.constant.Constants;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
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

  public static final String PROP_EXCEL_PATH = HousekeepPropKeys.PREFIX + Constants.TOOL_NAME
      + HousekeepPropKeys.SUFFIX_EXCEL_PATH;

  private DetailLogger detailLogger = new DetailLogger(this);

  HousekeepFilesBlf blf = new HousekeepFilesBlf();

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

    @Nullable String targetSystemName = env == null ? null
        : Objects.requireNonNull(env).getProperty(Constants.PROP_TARGET_SYSTEM_NAME);

    HousekeepLogUtil.logStarted(detailLogger, Constants.TOOL_NAME, excelPath, targetSystemName);

    // AbstractTaskSftp and CompressUtil are instantiated outside of Spring's DI (by reflection /
    // plain "new"), so they cannot read these properties from the Environment directly. Bridge
    // them through JVM system properties here, which also makes values set in
    // application.properties / application_profile.properties effective, not only "-D"
    // arguments. Left untouched when this tasklet is instantiated directly without Spring (e.g.
    // in tests), in which case only "-D" is honored.
    bridgeEnvPropertyToSystemProperty(Constants.PROP_SFTP_STRICT_HOST_KEY_CHECKING);
    bridgeEnvPropertyToSystemProperty(Constants.PROP_SFTP_CONNECT_TIMEOUT_MILLIS);
    bridgeEnvPropertyToSystemProperty(Constants.PROP_UNZIP_MAX_TOTAL_BYTES);

    HousekeepFilesForm nonnullForm = getFormFromExcel(excelPath);

    HousekeepLogUtil.logExcelFormatInfo(detailLogger, nonnullForm.getFormatVersion(),
        nonnullForm.getLocale());

    blf.execute(nonnullForm, env);

    return RepeatStatus.FINISHED;
  }

  private String validateExcelPath() {
    return ExcelPathValidator.validate(this, excelPath);
  }

  /**
   * Copies a property from the Spring {@code Environment} to a JVM system property of the same
   * key, if present. See the call site in {@link #execute} for why this bridging is needed.
   */
  private void bridgeEnvPropertyToSystemProperty(String key) {
    if (env != null && Objects.requireNonNull(env).containsProperty(key)) {
      System.setProperty(key, Objects.requireNonNull(Objects.requireNonNull(env).getProperty(key)));
    }
  }

  /**
   * It's package scope for unit-test.
   */
  HousekeepFilesForm getFormFromExcel(String excelPath) {
    return new HousekeepFilesForm(excelPath);
  }
}

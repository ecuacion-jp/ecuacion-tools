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

import java.util.Map;
import java.util.Objects;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.constant.Constants;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Housekeeps files.
 */
@Component
public class HousekeepFilesTasklet implements Tasklet {
  HousekeepFilesBlf blf = new HousekeepFilesBlf();
  @Nullable
  HousekeepFilesForm form;

  // Not set when this tasklet is instantiated directly (e.g. in tests) instead of through Spring.
  @Autowired(required = false)
  @Nullable
  Environment env;

  /**
   * Executes housekeeping files.
   */
  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    Map<String, Object> paramMap = chunkContext.getStepContext().getJobParameters();

    String excelPath = (String) paramMap.get("excelPath");

    execute(Objects.requireNonNull(excelPath));

    return RepeatStatus.FINISHED;
  }

  /**
   * Housekeeps files.
   */
  @SuppressWarnings("unused")
  public void execute(String excelFilePath) throws Exception {

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

    // Check the first argument.
    if (excelFilePath == null || excelFilePath.equals("")) {
      new Violations()
          .add(new BusinessViolation("MSG_ERR_PARAM_NULL_OR_EMPTY", "1st argument(excelFilePath)"))
          .throwIfAny();

    } else if (!excelFilePath.contains(".")) {
      // No file extension found.
      new Violations().add(new BusinessViolation("MSG_ERR_1ST_ARG_HAS_NO_EXTENSION", excelFilePath))
          .throwIfAny();
    }

    Objects.requireNonNull(excelFilePath);

    // Determine the number of parameters based on the file extension in the first argument path.
    String extension = excelFilePath.substring(excelFilePath.lastIndexOf("."));

    if (extension.equals(".xlsx")) {
      form = getFormFromExcel(excelFilePath);

    } else {
      new Violations().add(new BusinessViolation("MSG_ERR_EXTENSION_NOT_EXPECTED", extension))
          .throwIfAny();
    }

    blf.execute(Objects.requireNonNull(form));
  }

  /**
   * It's package scope for unit-test.
   */
  HousekeepFilesForm getFormFromExcel(String excelPath) {
    return new HousekeepFilesForm(excelPath);
  }
}

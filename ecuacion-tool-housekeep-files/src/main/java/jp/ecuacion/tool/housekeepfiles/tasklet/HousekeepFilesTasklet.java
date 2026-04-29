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
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Housekeeps files.
 */
@Component
public class HousekeepFilesTasklet implements Tasklet {
  HousekeepFilesBlf blf = new HousekeepFilesBlf();
  @Nullable HousekeepFilesForm form;

  /**
   * Executes housekeeping files.
   */
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
  public void execute(String excelFilePath) throws Exception {

    // 第一引数をチェック
    if (excelFilePath == null || excelFilePath.equals("")) {
      new Violations()
          .add(new BusinessViolation("MSG_ERR_PARAM_NULL_OR_EMPTY", "1st argument(excelFilePath)"))
          .throwIfAny();

    } else if (!excelFilePath.contains(".")) {
      // 拡張子が存在しない
      new Violations().add(new BusinessViolation("MSG_ERR_1ST_ARG_HAS_NO_EXTENSION", excelFilePath))
          .throwIfAny();
    }

    Objects.requireNonNull(excelFilePath);

    // 第一引数のパスに含まれるファイル名の拡張子によりパラメータの数を判断
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

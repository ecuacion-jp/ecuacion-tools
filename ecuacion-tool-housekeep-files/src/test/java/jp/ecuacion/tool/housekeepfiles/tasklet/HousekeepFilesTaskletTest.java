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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileNotFoundException;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link HousekeepFilesTasklet}. */
@DisplayName("HousekeepFilesTasklet")
class HousekeepFilesTaskletTest {

  /**
   * Tasklet whose Excel reading and business logic are stubbed out, to test only the launch
   * parameter check.
   */
  private HousekeepFilesTasklet taskletWithStubbedExcelAndBlf() {
    HousekeepFilesTasklet tasklet = new HousekeepFilesTasklet() {
      @SuppressWarnings({"NullAway", "null"})
      @Override
      protected HousekeepFilesForm getFormFromExcel(String excelFilePath) {
        return null;
      }
    };

    tasklet.blf = new HousekeepFilesBlf() {
      @Override
      public void execute(HousekeepFilesForm form) throws Exception {
        // Do nothing and return.
      }
    };

    return tasklet;
  }

  @Nested
  @DisplayName("execute(): launch parameter check")
  class ExecuteLaunchParameterCheck {

    @SuppressWarnings({"NullAway", "null"})
    @Test
    @DisplayName("null excelPath throws ViolationException with MSG_ERR_PARAM_NULL_OR_EMPTY")
    void nullArgument() {
      assertThatThrownBy(() -> taskletWithStubbedExcelAndBlf().execute(null))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId)
                  .containsExactly("MSG_ERR_PARAM_NULL_OR_EMPTY"));
    }

    @Test
    @DisplayName("empty excelPath throws ViolationException with MSG_ERR_PARAM_NULL_OR_EMPTY")
    void emptyArgument() {
      assertThatThrownBy(() -> taskletWithStubbedExcelAndBlf().execute(""))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId)
                  .containsExactly("MSG_ERR_PARAM_NULL_OR_EMPTY"));
    }
  }

  @Nested
  @DisplayName("execute(): Excel config file check")
  class ExecuteExcelConfigFileCheck {

    @Test
    @DisplayName("nonexistent Excel file throws RuntimeException caused by FileNotFoundException")
    void fileDoesNotExist() {
      assertThatThrownBy(() -> new HousekeepFilesTasklet().execute("./testpath/test.xlsx"))
          .isInstanceOf(RuntimeException.class)
          .hasCauseInstanceOf(FileNotFoundException.class);
    }
  }
}

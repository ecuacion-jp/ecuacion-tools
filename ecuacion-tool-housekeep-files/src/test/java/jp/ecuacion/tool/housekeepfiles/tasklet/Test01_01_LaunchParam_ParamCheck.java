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

import java.io.IOException;
import java.util.Objects;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Test01_01_LaunchParam_ParamCheck extends TaskletTestTool {

  @BeforeEach
  public void before() throws IOException {
    super.before();
    // Modified for testing.
    action = new HousekeepFilesTasklet() {
      @SuppressWarnings({"NullAway", "null"})
      @Override
      protected HousekeepFilesForm getFormFromExcel(String excelFilePath) {
        // Do nothing.
        return null;
      }
    };

    // Replace blf with a stub here.
    action.blf = new HousekeepFilesBlf() {
      @Override
      public void execute(HousekeepFilesForm form) throws Exception {
        // Do nothing and return.
      }
    };
  }

  @SuppressWarnings({"NullAway", "null"})
  @Test
  public void test11_whenArgIsNull() {
    try {
      action.execute(null);
      fail();

    } catch (Exception e) {
      assertTrue(e instanceof ViolationException);
      BusinessViolation bv =Objects.requireNonNull(
          ((ViolationException) e).getViolations().getBusinessViolations().iterator().next());
      assertEquals("MSG_ERR_PARAM_NULL_OR_EMPTY", bv.getMessageId());
    }
  }

  @Test
  public void test12_whenArgIsEmpty() {
    try {
      action.execute("");
      fail();

    } catch (Exception e) {
      assertTrue(e instanceof ViolationException);
      BusinessViolation bv = Objects.requireNonNull(
          ((ViolationException) e).getViolations().getBusinessViolations().iterator().next());
      assertEquals("MSG_ERR_PARAM_NULL_OR_EMPTY", bv.getMessageId());
    }
  }
}

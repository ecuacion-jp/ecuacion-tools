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
package jp.ecuacion.tool.housekeepfiles.blf;

import jakarta.validation.ConstraintViolation;
import java.util.List;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("null")
public class Test11_01_ExcelDataValidation_taskList_Check_Common_systemName extends BlfTestTool {

  /**
   * When the system field in taskList is empty. Treat as an error.
   * A spot check from the Action perspective, with detailed tests elsewhere.
   * system is attached as an attribute, so handle it separately for safety.
   */
  @Test
  public void test01_systemNameIsEmpty() throws Exception {
    HousekeepFilesForm form = new HousekeepFilesForm();

    try {
      blf.execute(form);
      fail();
    } catch (ViolationException e) {
      List<ConstraintViolation<?>> arr = e.getViolations().getConstraintViolations();
      Assertions.assertEquals(1, arr.size());
      ConstraintViolation<?> cv = arr.get(0);
      Assertions.assertEquals("jakarta.validation.constraints.NotEmpty", cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      Assertions.assertEquals("sysName", cv.getPropertyPath().toString());
    }
  }
}

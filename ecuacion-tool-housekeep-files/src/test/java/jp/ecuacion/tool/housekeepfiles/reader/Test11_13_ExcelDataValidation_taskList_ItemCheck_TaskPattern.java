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
package jp.ecuacion.tool.housekeepfiles.reader;

import jakarta.validation.ConstraintViolation;
import java.util.ArrayList;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.Test;

@SuppressWarnings("null")
public class Test11_13_ExcelDataValidation_taskList_ItemCheck_TaskPattern extends TestTool {

  @Test
  public void test01_taskPattern_invalid_null()
      throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", null, null,
        null, null, null, null, null, null, null, null, null, null);

    try {
      new Violations().validate(rec).throwIfAny();
      fail();

    } catch (ViolationException ex) {
      ConstraintViolation<?> cv =
          new ArrayList<>(ex.getViolations().getConstraintViolations()).get(0);
      assertEquals("jakarta.validation.constraints.NotEmpty",
          cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("taskPtnEnumName", cv.getPropertyPath().toString());
    }
  }

  @Test
  public void test02_taskPattern_invalid_unexpectedString()
      throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "AAA", null,
        null, null, null, null, null, null, null, null, null, null);

    try {
      new Violations().validate(rec).throwIfAny();
      fail();

    } catch (ViolationException ex) {
      ConstraintViolation<?> cv =
          new ArrayList<>(ex.getViolations().getConstraintViolations()).get(0);
      assertEquals("jp.ecuacion.lib.validation.constraints.EnumElement",
          cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("taskPtnEnumName", cv.getPropertyPath().toString());
    }
  }
}

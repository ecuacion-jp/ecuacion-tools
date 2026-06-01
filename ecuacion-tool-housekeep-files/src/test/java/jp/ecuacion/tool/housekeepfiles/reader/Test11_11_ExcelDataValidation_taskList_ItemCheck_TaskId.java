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
import java.util.HashSet;
import java.util.Set;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.Test;

@SuppressWarnings("null")
public class Test11_11_ExcelDataValidation_taskList_ItemCheck_TaskId extends TestTool {

  @Test
  public void test01_taskId_invalid_null() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord(null, "aTaskName", "CREATE_DIR",
        null, null, null, null, null, null, null, null, null, null, null);

    try {

      new Violations().validate(rec).throwIfAny();
      fail();

    } catch (ViolationException ex) {
      ConstraintViolation<?> cv =
          new ArrayList<>(ex.getViolations().getConstraintViolations()).get(0);
      assertEquals("jakarta.validation.constraints.NotEmpty",
          cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("taskId", cv.getPropertyPath().toString());
    }
  }

  @Test
  public void test02_taskId_invalid_emptyString() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("", "aTaskName", "CREATE_DIR", null,
        null, null, null, null, null, null, null, null, null, null);

    try {
      new Violations().validate(rec).throwIfAny();
      fail();

    } catch (ViolationException ex) {
      // NotEmpty and Size
      assertEquals(2, ex.getViolations().getConstraintViolations().size());
      // Verify that both are failing with an error on taskId.
      Set<String> set = new HashSet<>();
      for (ConstraintViolation<?> cv : ex.getViolations().getConstraintViolations()) {
        assertEquals("taskId", cv.getPropertyPath().toString());
        set.add(cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      }

      assertTrue(set.contains("jakarta.validation.constraints.NotEmpty"));
      assertTrue(set.contains("jakarta.validation.constraints.Size"));
    }
  }

  @Test
  public void test03_taskId_invalid_tooLong() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("12345678901", "aTaskName",
        "CREATE_DIR", null, null, null, null, null, null, null, null, null, null, null);

    try {
      new Violations().validate(rec).throwIfAny();
      fail();

    } catch (ViolationException ex) {
      ConstraintViolation<?> cv =
          new ArrayList<>(ex.getViolations().getConstraintViolations()).get(0);
      assertEquals("jakarta.validation.constraints.Size",
          cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("taskId", cv.getPropertyPath().toString());
    }
  }

  @Test
  public void test04_taskId_invalid_illegalChar_hash()
      throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("task#", "aTaskName", "CREATE_DIR",
        null, null, null, null, null, null, null, null, null, null, null);

    try {
      new Violations().validate(rec).throwIfAny();
      fail();

    } catch (ViolationException ex) {
      ConstraintViolation<?> cv =
          new ArrayList<>(ex.getViolations().getConstraintViolations()).get(0);
      assertEquals("jakarta.validation.constraints.Pattern",
          cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("taskId", cv.getPropertyPath().toString());
    }
  }
}

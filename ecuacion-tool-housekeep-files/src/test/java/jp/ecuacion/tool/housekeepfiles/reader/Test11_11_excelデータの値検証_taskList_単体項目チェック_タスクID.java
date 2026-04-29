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
import jp.ecuacion.lib.core.util.ViolationUtil;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.Test;

public class Test11_11_excelデータの値検証_taskList_単体項目チェック_タスクID extends TestTool {

  @Test
  public void test01_タスクID_異常系_null() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord(null, "aTaskName", "CREATE_DIR",
        null, null, null, null, null, null, null, null, null, null, null);

    try {

      ViolationUtil.validate(rec).throwIfAny();
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
  public void test02_タスクID_異常系_空文字() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("", "aTaskName", "CREATE_DIR", null,
        null, null, null, null, null, null, null, null, null, null);

    try {
      ViolationUtil.validate(rec).throwIfAny();
      fail();

    } catch (ViolationException ex) {
      // NotEmptyとSize
      assertEquals(2, ex.getViolations().getConstraintViolations().size());
      // 両方ともtaskIdでエラーになっていることを確認
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
  public void test03_タスクID_異常系_長さ超過() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("12345678901", "aTaskName",
        "CREATE_DIR", null, null, null, null, null, null, null, null, null, null, null);

    try {
      ViolationUtil.validate(rec).throwIfAny();
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
  public void test04_タスクID_異常系_不正文字使用_hash()
      throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("task#", "aTaskName", "CREATE_DIR",
        null, null, null, null, null, null, null, null, null, null, null);

    try {
      ViolationUtil.validate(rec).throwIfAny();
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

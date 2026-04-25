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
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import jp.ecuacion.lib.core.util.ValidationUtil;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.apache.poi.EncryptedDocumentException;
import org.junit.jupiter.api.Test;

public class Test11_12_excelデータの値検証_taskList_単体項目チェック_タスク名 extends TestTool {

  @Test
  public void test01_タスク名_異常系_null() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", null, "CREATE_DIR", null,
        null, null, null, null, null, null, null, null, null, null);

    try {
      ValidationUtil.validateThenThrow(rec);
      fail();

    } catch (ConstraintViolationException ex) {
      ConstraintViolation<?> cv = new ArrayList<>(ex.getConstraintViolations()).get(0);
      assertEquals("jakarta.validation.constraints.NotEmpty", cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("taskName", cv.getPropertyPath().toString());
    }
  }

  @Test
  public void test02_タスク名_異常系_空文字() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "", "CREATE_DIR", null,
        null, null, null, null, null, null, null, null, null, null);

    try {
      ValidationUtil.validateThenThrow(rec);
      fail();

    } catch (ConstraintViolationException ex) {
      // NotEmptyとSize
      assertEquals(2, ex.getConstraintViolations().size());
      // 両方ともtaskNameでエラーになっていることを確認
      Set<String> set = new HashSet<>();
      for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
        assertEquals("taskName", cv.getPropertyPath().toString());
        set.add(cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      }

      assertTrue(set.contains("jakarta.validation.constraints.NotEmpty"));
      assertTrue(set.contains("jakarta.validation.constraints.Size"));
    }
  }

  @Test
  public void test03_タスク名_異常系_長さ超過() throws Exception {
    HousekeepFilesTaskRecord rec =
        new HousekeepFilesTaskRecord("aTaskId", "12345678901234567890123456789012345678901",
            "CREATE_DIR", null, null, null, null, null, null, null, null, null, null, null);

    try {
      ValidationUtil.validateThenThrow(rec);
      fail();

    } catch (ConstraintViolationException ex) {
      ConstraintViolation<?> cv = new ArrayList<>(ex.getConstraintViolations()).get(0);
      assertEquals("jakarta.validation.constraints.Size", cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("taskName", cv.getPropertyPath().toString());
    }
  }

  @Test
  public void test04_タスク名_異常系_不正文字使用_hash()
      throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName#",
        "CREATE_DIR", null, null, null, null, null, null, null, null, null, null, null);

    try {
      ValidationUtil.validateThenThrow(rec);
      fail();

    } catch (ConstraintViolationException ex) {
      ConstraintViolation<?> cv = new ArrayList<>(ex.getConstraintViolations()).get(0);
      assertEquals("jakarta.validation.constraints.Pattern", cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("taskName", cv.getPropertyPath().toString());
    }
  }
}

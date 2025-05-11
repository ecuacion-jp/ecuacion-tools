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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.exception.checked.ValidationAppException;
import jp.ecuacion.lib.core.jakartavalidation.bean.ConstraintViolationBean;
import jp.ecuacion.lib.core.util.ValidationUtil;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import org.apache.poi.EncryptedDocumentException;
import org.junit.jupiter.api.Test;

public class Test11_11_excelデータの値検証_taskList_単体項目チェック_タスクID extends Test11_1x_common {

  @Test
  public void test01_タスクID_異常系_null() throws EncryptedDocumentException, IOException, AppException {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord(null, "aTaskName", "CREATE_DIR",
        null, null, null, null, null, null, null, null, null, null, null);

    try {
      
      ValidationUtil.builder().build().validateThenThrow(rec);
      fail();

    } catch (MultipleAppException ex) {
      ValidationAppException bv = (ValidationAppException) ex.getList().get(0);
      ConstraintViolationBean bean = bv.getConstraintViolationBean();
      assertEquals("jakarta.validation.constraints.NotEmpty", bean.getAnnotation());
      assertEquals("taskId", bean.getPropertyPath());
    }
  }

  @Test
  public void test02_タスクID_異常系_空文字() throws EncryptedDocumentException, IOException, AppException {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("", "aTaskName", "CREATE_DIR", null,
        null, null, null, null, null, null, null, null, null, null);

    try {
      ValidationUtil.builder().build().validateThenThrow(rec);
      fail();

    } catch (MultipleAppException ex) {
      // NotEmptyとSize
      assertEquals(2, ex.getList().size());
      // 両方ともtaskIdでエラーになっていることを確認
      Set<String> set = new HashSet<>();
      for (AppException ae : ex.getList()) {
        ValidationAppException bv = (ValidationAppException) ae;
        ConstraintViolationBean bean = bv.getConstraintViolationBean();
        assertEquals("taskId", bean.getPropertyPath());
        set.add(bean.getAnnotation());
      }

      assertTrue(set.contains("jakarta.validation.constraints.NotEmpty"));
      assertTrue(set.contains("jakarta.validation.constraints.Size"));
    }
  }

  @Test
  public void test03_タスクID_異常系_長さ超過() throws EncryptedDocumentException, IOException, AppException {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("12345678901", "aTaskName",
        "CREATE_DIR", null, null, null, null, null, null, null, null, null, null, null);

    try {
      ValidationUtil.builder().build().validateThenThrow(rec);
      fail();

    } catch (MultipleAppException ex) {
      ValidationAppException bv = (ValidationAppException) ex.getList().get(0);
      ConstraintViolationBean bean = bv.getConstraintViolationBean();
      assertEquals("jakarta.validation.constraints.Size", bean.getAnnotation());
      assertEquals("taskId", bean.getPropertyPath());
    }
  }

  @Test
  public void test04_タスクID_異常系_不正文字使用_hash()
      throws EncryptedDocumentException, IOException, AppException {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("task#", "aTaskName", "CREATE_DIR",
        null, null, null, null, null, null, null, null, null, null, null);

    try {
      ValidationUtil.builder().build().validateThenThrow(rec);
      fail();

    } catch (MultipleAppException ex) {
      ValidationAppException bv = (ValidationAppException) ex.getList().get(0);
      ConstraintViolationBean bean = bv.getConstraintViolationBean();
      assertEquals("jakarta.validation.constraints.Pattern", bean.getAnnotation());
      assertEquals("taskId", bean.getPropertyPath());
    }
  }
}

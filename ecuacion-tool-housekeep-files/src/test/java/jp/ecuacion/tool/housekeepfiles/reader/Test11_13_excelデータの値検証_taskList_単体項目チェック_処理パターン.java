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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.exception.checked.ValidationAppException;
import jp.ecuacion.lib.core.jakartavalidation.bean.ConstraintViolationBean;
import jp.ecuacion.lib.core.util.ValidationUtil;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.apache.poi.EncryptedDocumentException;
import org.junit.jupiter.api.Test;

public class Test11_13_excelデータの値検証_taskList_単体項目チェック_処理パターン extends TestTool {

  @Test
  public void test01_処理パターン_異常系_null()
      throws EncryptedDocumentException, IOException, AppException {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", null, null,
        null, null, null, null, null, null, null, null, null, null);

    try {
      ValidationUtil.validateThenThrow(rec);
      fail();

    } catch (MultipleAppException ex) {
      ValidationAppException bv = (ValidationAppException) ex.getList().get(0);
      ConstraintViolationBean bean = bv.getConstraintViolationBean();
      assertEquals("jakarta.validation.constraints.NotEmpty", bean.getAnnotation());
      assertEquals("taskPtnEnumName", bean.getPropertyPath());
    }
  }

  @Test
  public void test02_処理パターン_異常系_想定外文字列()
      throws EncryptedDocumentException, IOException, AppException {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "AAA", null,
        null, null, null, null, null, null, null, null, null, null);

    try {
      ValidationUtil.validateThenThrow(rec);
      fail();

    } catch (MultipleAppException ex) {
      ValidationAppException bv = (ValidationAppException) ex.getList().get(0);
      ConstraintViolationBean bean = bv.getConstraintViolationBean();
      assertEquals("jp.ecuacion.lib.core.jakartavalidation.validator.EnumElement", bean.getAnnotation());
      assertEquals("taskPtnEnumName", bean.getPropertyPath());
    }
  }
}

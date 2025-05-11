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

import java.util.List;
import jp.ecuacion.lib.core.exception.checked.ValidationAppException;
import jp.ecuacion.lib.core.jakartavalidation.bean.ConstraintViolationBean;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Test11_01_excelデータの値検証_taskList_チェック_共通_systemName extends BlfTestTool {

  /**
   * taskListのsystemが空欄の場合。 エラーとする。 別途テストするが、Actionから見てちゃんと動いているかの抜粋テスト。
   * systemだけ、属性にくっついてるので念のため別扱いにしておく
   */
  @Test
  public void test01_system名が空欄() {
    HousekeepFilesForm form = new HousekeepFilesForm();
    
    try {
      blf.execute(form);
      fail();
    } catch (Exception e) {
      List<Throwable> arr = ExceptionUtil.getExceptionListWithMessages(e);
      Assertions.assertEquals(arr.size(), 1);
      Assertions.assertTrue(arr.get(0) instanceof ValidationAppException);
      ValidationAppException ae = (ValidationAppException) arr.get(0);
      ConstraintViolationBean bean = ae.getConstraintViolationBean();
      Assertions.assertEquals("jakarta.validation.constraints.NotEmpty", bean.getAnnotation());
      Assertions.assertEquals("sysName", bean.getPropertyPath());
    }
  }
}

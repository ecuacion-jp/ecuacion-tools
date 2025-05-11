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
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.util.ExceptionUtil;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author 庸介
 *
 */
public class Test01_01_起動パラメータ関連_パラメータチェック extends TaskletTestTool {

  @BeforeEach
  public void before() throws IOException {
    super.before();
    // テスト用に変更
    action = new HousekeepFilesTasklet() {
      @Override
      protected HousekeepFilesForm getFormFromExcel(String excelFilePath) throws AppException {
        // 何もしない
        return null;
      }
    };

    // ここではblfをスタブにしたいので置き換え
    action.blf = new HousekeepFilesBlf() {
      @Override
      public void execute(HousekeepFilesForm form) throws AppException {
        // 何もしないで終了
      }
    };
  }

  @Test
  public void test11_引数がnullの場合() {
    try {
      action.execute(null);
      fail();
      
    } catch (Exception e) {
      assertTrue(e instanceof BizLogicAppException);

      if (e instanceof BizLogicAppException) {
        BizLogicAppException cfe = (BizLogicAppException) ExceptionUtil.getExceptionListWithMessages(e).get(0);
        assertEquals("MSG_ERR_PARAM_NULL_OR_EMPTY", cfe.getMessageId());
        assertEquals(1, cfe.getMessageArgs().length);
        assertEquals("1st argument(excelFilePath)", cfe.getMessageArgs()[0].getArgString());
      }
    }
  }

  @Test
  public void test12_引数が空文字の場合() {
    try {
      action.execute("");
      fail();
      
    } catch (Exception e) {
      assertTrue(e instanceof BizLogicAppException);
      if (e instanceof BizLogicAppException) {
        BizLogicAppException cfe = (BizLogicAppException) ExceptionUtil.getExceptionListWithMessages(e).get(0);
        assertEquals("MSG_ERR_PARAM_NULL_OR_EMPTY", cfe.getMessageId());
        assertEquals(1, cfe.getMessageArgs().length);
        assertEquals("1st argument(excelFilePath)", cfe.getMessageArgs()[0].getArgString());
      }
    }
  }
}

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
package jp.ecuacion.tool.housekeepfiles.bl.task;

import java.io.IOException;
import java.util.ArrayList;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.tool.housekeepfiles.dto.form.DoNothingInConstructorForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * HousekeepFilesXmlDataChecker#checkTaskItem のテスト。
 */
public class Test31_12_excelデータの値検証_taskList_チェック_各項目が共通処理を呼んでいるか確認 extends TestTool {

  DoNothingInConstructorForm form = null;

  @BeforeAll
  public static void beforeClass() throws IOException {}

  @BeforeEach
  public void before() throws IOException {
    try {
      form = new DoNothingInConstructorForm();
      form.getTaskInfoHdRec().recList = new ArrayList<>();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void test01_各項目がcheckTaskItemメソッドを呼んでいるかの確認() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE",
        "aHost", "aPath", "TRUE", "DAY", "7", "IGNORE", "aPath", "TRUE", "FALSE", "IGNORE", null);
    form.getTaskInfoHdRec().recList.add(rec);

    // stub
    final CounterForStub counterObj = new CounterForStub();
    // checkerを変更
    Move move = new Move() {
      @Override
      public void checkTaskItem(String taskId, TaskPtnEnum taskPtn,
          TaskAttrCheckPtnEnum checkPtn, String itemTitle, Object itemValue)
          throws BizLogicAppException {
        counterObj.counter++;
      }
    };

    try {
      move.check(rec);
      assertEquals(3, counterObj.counter);

    } catch (MultipleAppException ex) {
      fail();
    }
  }

  class CounterForStub {
    public int counter = 0;
  }
}

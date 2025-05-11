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
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.util.ExceptionUtil;
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
public class Test31_11_excelデータの値検証_taskList_チェック_共通処理 extends TestTool {

  DoNothingInConstructorForm form = null;
  HousekeepFilesTaskRecord taskRec = null;

  @BeforeAll
  public static void beforeClass() throws IOException {
  }

  @BeforeEach
  public void before() throws IOException {
  }

  @Test
  public void test01_checkTaskItem_required_null() {
    try {
      new Move().checkTaskItem("aTaskId", TaskPtnEnum.MOVE, TaskAttrCheckPtnEnum.REQUIRED, "unit",
          null);
      fail();
    } catch (Exception e) {
      List<Throwable> thList = ExceptionUtil.getExceptionListWithMessages((AppException) e);
      assertEquals(1, thList.size());
      BizLogicAppException ace = (BizLogicAppException) thList.get(0);
      assertEquals("MSG_ERR_TASK_REQUIRED_CHECK", ace.getMessageId());
      assertEquals(3, ace.getMessageArgs().length);
      assertEquals("aTaskId", ace.getMessageArgs()[0].getArgString());
      assertEquals("MOVE", ace.getMessageArgs()[1].getArgString());
      assertEquals("処理対象（単位）", ace.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test02_checkTaskItem_required_空文字() {
    try {
      new Move().checkTaskItem("aTaskId", TaskPtnEnum.MOVE, TaskAttrCheckPtnEnum.REQUIRED, "unit",
          "");
      fail();
    } catch (Exception e) {
      List<Throwable> thList = ExceptionUtil.getExceptionListWithMessages((AppException) e);
      assertEquals(1, thList.size());
      BizLogicAppException ace = (BizLogicAppException) thList.get(0);
      assertEquals("MSG_ERR_TASK_REQUIRED_CHECK", ace.getMessageId());
      assertEquals(3, ace.getMessageArgs().length);
      assertEquals("aTaskId", ace.getMessageArgs()[0].getArgString());
      assertEquals("MOVE", ace.getMessageArgs()[1].getArgString());
      assertEquals("処理対象（単位）", ace.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test03_checkTaskItem_required_通常文字列() {
    try {
      new Move().checkTaskItem("aTaskId", TaskPtnEnum.MOVE, TaskAttrCheckPtnEnum.REQUIRED, "anItem",
          "aValue");
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void test11_checkTaskItem_prohibited_null() {
    try {
      new Move().checkTaskItem("aTaskId", TaskPtnEnum.MOVE, TaskAttrCheckPtnEnum.PROHIBITED, "anItem",
          null);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void test12_checkTaskItem_prohibited_空文字() {
    try {
      new Move().checkTaskItem("aTaskId", TaskPtnEnum.MOVE, TaskAttrCheckPtnEnum.PROHIBITED, "anItem",
          "");
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void test13_checkTaskItem_prohibited_通常文字列() {
    try {
      new Move().checkTaskItem("aTaskId", TaskPtnEnum.MOVE, TaskAttrCheckPtnEnum.PROHIBITED, "unit",
          "aValue");
      fail();
    } catch (Exception e) {
      List<Throwable> thList = ExceptionUtil.getExceptionListWithMessages((AppException) e);
      assertEquals(1, thList.size());
      BizLogicAppException ace = (BizLogicAppException) thList.get(0);
      assertEquals("MSG_ERR_TASK_PROHIBITED_CHECK", ace.getMessageId());
      assertEquals(3, ace.getMessageArgs().length);
      assertEquals("aTaskId", ace.getMessageArgs()[0].getArgString());
      assertEquals("MOVE", ace.getMessageArgs()[1].getArgString());
      assertEquals("処理対象（単位）", ace.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test21_checkTaskItem_arbitrary_null() {
    try {
      new Move().checkTaskItem("aTaskId", TaskPtnEnum.MOVE, TaskAttrCheckPtnEnum.ARBITRARY, "anItem",
          null);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void test22_checkTaskItem_arbitrary_空文字() {
    try {
      new Move().checkTaskItem("aTaskId", TaskPtnEnum.MOVE, TaskAttrCheckPtnEnum.ARBITRARY, "anItem",
          "");
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void test23_checkTaskItem_arbitrary_通常文字列() {
    try {
      new Move().checkTaskItem("aTaskId", TaskPtnEnum.MOVE, TaskAttrCheckPtnEnum.ARBITRARY, "anItem",
          "aValue");
    } catch (Exception e) {
      fail();
    }
  }
}

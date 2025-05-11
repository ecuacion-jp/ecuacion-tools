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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.tool.housekeepfiles.bl.HousekeepFilesBl;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesHdRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.Test;

public class Test81_102_単体動作確認_task_ファイル作成 extends TestTool {

  public static boolean procCalledOnWarnListIsNotEmpty = false;

  private HousekeepFilesForm getForm(HousekeepFilesTaskRecord taskRec) {

    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);
    form.getTaskInfoHdRec().setSysName("test-system");

    return form;
  }

  @Test
  public void test01_異常系_input_validation_remoteServer_notEmpty() throws Exception {
    HousekeepFilesTaskRecord rec =
        new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "CREATE_FILE", "aHost", null, null,
            null, null, null, "aPath", "FALSE", "FALSE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (MultipleAppException ex) {
      assertEquals(1, ex.getList().size());
      BizLogicAppException ae = (BizLogicAppException) ex.getList().get(0);
      assertEquals("MSG_ERR_TASK_PROHIBITED_CHECK", ae.getMessageId());
      assertEquals("リモートサーバ", ae.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test02_異常系_input_validation_srcPath_notEmpty() throws Exception {
    HousekeepFilesTaskRecord rec =
        new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "CREATE_FILE", null, "a", "TRUE", "5",
            "0", "IGNORE", "aPath", "FALSE", "FALSE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (MultipleAppException ex) {
      assertEquals(1, ex.getList().size());
      BizLogicAppException ae = (BizLogicAppException) ex.getList().get(0);
      assertEquals("MSG_ERR_TASK_PROHIBITED_CHECK", ae.getMessageId());
      assertEquals("元パス", ae.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test03_異常系_input_validation_destPath_empty() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "CREATE_FILE", null, null, null, null, null, null, null, null, null, null, null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (MultipleAppException ex) {
      assertEquals(1, ex.getList().size());
      BizLogicAppException ae = (BizLogicAppException) ex.getList().get(0);
      assertEquals("MSG_ERR_TASK_REQUIRED_CHECK", ae.getMessageId());
      assertEquals("先パス", ae.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test11_異常系_input_validation_taskDependent_isDestPathDir_true() throws Exception {
    HousekeepFilesTaskRecord rec =
        new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "CREATE_FILE", null, null, null, null,
            null, null, "aPath", "TRUE", "FALSE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);

    } catch (MultipleAppException ex) {
      assertEquals(1, ex.getList().size());
      BizLogicAppException ae = (BizLogicAppException) ex.getList().get(0);
      assertEquals("MSG_ERR_TASK_CANNOT_SET_IS_DEST_PATH_DIR_TO_VALUE", ae.getMessageId());
      assertEquals("TRUE", ae.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test12_異常系_input_validation_taskDependent_DoesOverwriteDestPath_true()
      throws Exception {
    HousekeepFilesTaskRecord rec =
        new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "CREATE_FILE", null, null, null, null,
            null, null, "aPath", "FALSE", "TRUE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);

    } catch (MultipleAppException ex) {
      assertEquals(1, ex.getList().size());
      BizLogicAppException ae = (BizLogicAppException) ex.getList().get(0);
      assertEquals("MSG_ERR_TASK_CANNOT_SET_OVERWRITE_TO_VALUE", ae.getMessageId());
    }
  }

  @Test
  public void test21_異常系_ディレクトリ存在_先パス存在時処理_IGNORE() throws Exception {
    String path = TEST_HOME_PATH + "/test-dir";
    new File(path).mkdirs();

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "CREATE_FILE", null, null, null, null, null, null, path, "FALSE", "FALSE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (BizLogicAppException ex) {
      assertEquals("MSG_ERR_DEST_PATH_IS_DIR", ex.getMessageId());
    }
  }

  @Test
  public void test22_異常系_ディレクトリ存在_先パス存在時処理_WARN() throws Exception {
    String path = TEST_HOME_PATH + "/test-dir";
    new File(path).mkdirs();

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "CREATE_FILE", null, null, null, null, null, null, path, "FALSE", "FALSE", "WARN", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (BizLogicAppException ex) {
      assertEquals("MSG_ERR_DEST_PATH_IS_DIR", ex.getMessageId());
    }
  }

  @Test
  public void test23_異常系_ディレクトリ存在_先パス存在時処理_ERROR() throws Exception {
    String path = TEST_HOME_PATH + "/test-dir";
    new File(path).mkdirs();

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "CREATE_FILE", null, null, null, null, null, null, path, "FALSE", "FALSE", "ERROR", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (BizLogicAppException ex) {
      assertEquals("MSG_ERR_DEST_PATH_IS_DIR", ex.getMessageId());
    }
  }

  @Test
  public void test24_異常系_ファイル存在_先パス存在時処理_IGNORE() throws Exception {
    String path = TEST_HOME_PATH + "/test-dir";
    new File(TEST_HOME_PATH).mkdirs();
    new File(path).createNewFile();

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "CREATE_FILE", null, null, null, null, null, null, path, "FALSE", "FALSE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    new HousekeepFilesBlf().execute(form);
  }

  @Test
  public void test25_異常系_ファイル存在_先パス存在時処理_WARN() throws Exception {
    String path = TEST_HOME_PATH + "/test-dir";
    new File(TEST_HOME_PATH).mkdirs();
    new File(path).createNewFile();

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "CREATE_FILE", null, null, null, null, null, null, path, "FALSE", "FALSE", "WARN", null);
    HousekeepFilesForm form = getForm(rec);

    // HousekeepFilesBl#sendWarnMail を呼ばれたことがわかるよう置き換え
    HousekeepFilesBl bl = new HousekeepFilesBl() {
      @Override
      public void sendWarnMail(List<AppException> warnList, HousekeepFilesHdRecord hdE)
          throws Exception {
        procCalledOnWarnListIsNotEmpty = true;
      }
    };

    new HousekeepFilesBlf(bl).execute(form);

    assertTrue(procCalledOnWarnListIsNotEmpty);
  }

  @Test
  public void test26_異常系_ファイル存在_先パス存在時処理_ERROR() throws Exception {
    String path = TEST_HOME_PATH + "/test-dir";
    new File(TEST_HOME_PATH).mkdirs();
    new File(path).createNewFile();

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "CREATE_FILE", null, null, null, null, null, null, path, "FALSE", "FALSE", "ERROR", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (BizLogicAppException ex) {
      assertEquals("MSG_ERR_DEST_PATH_EXISTS", ex.getMessageId());
    }
  }

  @Test
  public void test27_異常系_親ディレクトリなし() throws Exception {
    String path = TEST_HOME_PATH + "/test-dir/test-file.txt";

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "CREATE_FILE", null, null, null, null, null, null, path, "FALSE", "FALSE", "ERROR", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (BizLogicAppException ex) {
      assertEquals("MSG_ERR_PARENT_DIR_NOT_EXIST", ex.getMessageId());
    }
  }

  @Test
  public void test31_正常系() throws Exception {
    String path = TEST_HOME_PATH + "/test";
    new File(TEST_HOME_PATH).mkdirs();

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "CREATE_FILE", null, null, null, null, null, null, path, "FALSE", "FALSE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    File file = new File(path);
    assertFalse(file.exists());

    new HousekeepFilesBlf().execute(form);

    assertTrue(file.exists());

    String content = Files.readString(Path.of(file.getAbsolutePath()));
    assertEquals("", content);
  }
}

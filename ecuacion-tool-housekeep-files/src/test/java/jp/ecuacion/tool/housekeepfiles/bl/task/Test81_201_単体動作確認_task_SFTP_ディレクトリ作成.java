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

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.tool.housekeepfiles.bl.HousekeepFilesBl;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesHdRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Test81_201_単体動作確認_task_SFTP_ディレクトリ作成 extends TestTool {

  public static boolean procCalledOnWarnListIsNotEmpty = false;

  @BeforeAll
  private static void beforeAll() throws JSchException {
    beforeAllOnSftpTest();
  }

  @BeforeEach
  private void beforeEach() throws Exception {
    beforeEachOnSftpTest();
  }

  @AfterEach
  private void afterach() throws Exception {
    afterEachOnSftpTest();
  }

  @AfterAll
  protected static void afterAll() {
    afterAllOnSftpTest();
  }

  private HousekeepFilesForm getForm(HousekeepFilesTaskRecord taskRec) {
    HousekeepFilesAuthRecord authRec = new HousekeepFilesAuthRecord("resources.ecuacion.jp", "SFTP",
        "20022", "PASSWORD", "test_user", "pass", null);

    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);
    form.getAuthInfoRecList().add(authRec);
    form.getTaskInfoHdRec().setSysName("test-system");

    return form;
  }

  @Test
  public void test01_異常系_input_validation_remoteServer_empty() throws Exception {
    HousekeepFilesTaskRecord rec =
        new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "SFTP_CREATE_DIR", null, null, null,
            null, null, null, "aPath", "TRUE", "FALSE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (MultipleAppException ex) {
      assertEquals(1, ex.getList().size());
      BizLogicAppException bl = (BizLogicAppException) ex.getList().get(0);
      assertEquals("MSG_ERR_TASK_REQUIRED_CHECK", bl.getMessageId());
      assertEquals("リモートサーバ", bl.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test02_異常系_input_validation_srcPath_notEmpty() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_CREATE_DIR", "resources.ecuacion.jp", "aPath", "TRUE", "7", "DAY", "7", "aPath",
        "TRUE", "FALSE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (MultipleAppException ex) {
      assertEquals(1, ex.getList().size());
      BizLogicAppException bl = (BizLogicAppException) ex.getList().get(0);
      assertEquals("MSG_ERR_TASK_PROHIBITED_CHECK", bl.getMessageId());
      assertEquals("元パス", bl.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test03_異常系_input_validation_destPath_empty() throws Exception {
    HousekeepFilesTaskRecord rec =
        new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "SFTP_CREATE_DIR",
            "resources.ecuacion.jp", null, null, null, null, null, null, null, null, null, null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (MultipleAppException ex) {
      assertEquals(1, ex.getList().size());
      BizLogicAppException bl = (BizLogicAppException) ex.getList().get(0);
      assertEquals("MSG_ERR_TASK_REQUIRED_CHECK", bl.getMessageId());
      assertEquals("先パス", bl.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test11_異常系_input_validation_taskDependent_isDestPathDir_false() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_CREATE_DIR", "resources.ecuacion.jp", null, null, null, null, null,
        SFTP_ROOT_PATH + "/destPath", "FALSE", "FALSE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (MultipleAppException ex) {
      assertEquals(1, ex.getList().size());
      BizLogicAppException bl = (BizLogicAppException) ex.getList().get(0);
      assertEquals("MSG_ERR_TASK_CANNOT_SET_IS_DEST_PATH_DIR_TO_VALUE", bl.getMessageId());
      assertEquals("FALSE", bl.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test12_異常系_input_validation_taskDependent_DoesOverwriteDestPath_true()
      throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_CREATE_DIR", "resources.ecuacion.jp", null, null, null, null, null,
        FileUtil.concatFilePaths(SFTP_ROOT_PATH, "test-dir"), "TRUE", "TRUE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (MultipleAppException ex) {
      assertEquals(1, ex.getList().size());
      BizLogicAppException bl = (BizLogicAppException) ex.getList().get(0);
      assertEquals("MSG_ERR_TASK_CANNOT_SET_OVERWRITE_TO_VALUE", bl.getMessageId());
      assertEquals("TRUE", bl.getMessageArgs()[2].getArgString());
    }
  }

  @Test
  public void test21_異常系_ディレクトリ存在_先パス存在時処理_IGNORE() throws Exception {
    String dir = SFTP_ROOT_PATH + "/test-dir";

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_CREATE_DIR", "resources.ecuacion.jp", null, null, null, null, null, dir, "TRUE",
        "FALSE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    // HousekeepFilesBl#sendWarnMail を呼ばれたことがわかるよう置き換え
    HousekeepFilesBl bl = new HousekeepFilesBl() {
      @Override
      public void sendWarnMail(List<AppException> warnList, HousekeepFilesHdRecord hdE)
          throws Exception {
        procCalledOnWarnListIsNotEmpty = true;
      }
    };

    String testFilePath = dir + "/test.txt";
    sftpCreateDir(channel, dir);
    sftpCreateFile(channel, testFilePath);
    LsEntry self = sftpLsSelfDetail(channel, dir);
    String timeBefore = self.getAttrs().getAtimeString();

    new HousekeepFilesBlf(bl).execute(form);

    self = sftpLsSelfDetail(channel, dir);
    String timeAfter = self.getAttrs().getAtimeString();
    assertEquals(timeBefore, timeAfter);
    assertTrue(sftpExists(channel, testFilePath));

    assertFalse(procCalledOnWarnListIsNotEmpty);
  }

  @Test
  public void test22_異常系_ディレクトリ存在_先パス存在時処理_WARN() throws Exception {
    String dir = SFTP_ROOT_PATH + "/test-dir";

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_CREATE_DIR", "resources.ecuacion.jp", null, null, null, null, null, dir, "TRUE",
        "FALSE", "WARN", null);
    HousekeepFilesForm form = getForm(rec);

    // HousekeepFilesBl#sendWarnMail を呼ばれたことがわかるよう置き換え
    HousekeepFilesBl bl = new HousekeepFilesBl() {
      @Override
      public void sendWarnMail(List<AppException> warnList, HousekeepFilesHdRecord hdE)
          throws Exception {
        procCalledOnWarnListIsNotEmpty = true;
      }
    };

    String testFilePath = dir + "/test.txt";
    sftpCreateDir(channel, dir);
    sftpCreateFile(channel, testFilePath);
    LsEntry self = sftpLsSelfDetail(channel, dir);
    String timeBefore = self.getAttrs().getAtimeString();

    new HousekeepFilesBlf(bl).execute(form);

    self = sftpLsSelfDetail(channel, dir);
    String timeAfter = self.getAttrs().getAtimeString();
    assertEquals(timeBefore, timeAfter);
    assertTrue(sftpExists(channel, testFilePath));

    assertTrue(procCalledOnWarnListIsNotEmpty);
  }

  @Test
  public void test23_異常系_ディレクトリ存在_先パス存在時処理_ERROR() throws Exception {
    String dir = SFTP_ROOT_PATH + "/test-dir";

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_CREATE_DIR", "resources.ecuacion.jp", null, null, null, null, null, dir, "TRUE",
        "FALSE", "ERROR", null);
    HousekeepFilesForm form = getForm(rec);

    sftpCreateDir(channel, dir);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (RuntimeException ex) {
      BizLogicAppException blEx = (BizLogicAppException) ex.getCause();
      assertEquals("MSG_ERR_DEST_PATH_EXISTS", blEx.getMessageId());
    }
  }

  @Test
  public void test24_異常系_ファイル存在_先パス存在時処理_IGNORE() throws Exception {
    String filePath = SFTP_ROOT_PATH + "/testfile.txt";

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_CREATE_DIR", "resources.ecuacion.jp", null, null, null, null, null, filePath, "TRUE",
        "FALSE", "IGNORE", null);
    HousekeepFilesForm form = getForm(rec);

    sftpCreateDir(channel, SFTP_ROOT_PATH);
    sftpCreateFile(channel, filePath);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();
    
    } catch (RuntimeException ex) {
      BizLogicAppException blEx = (BizLogicAppException) ex.getCause();
      assertEquals("MSG_ERR_DEST_PATH_IS_FILE", blEx.getMessageId());
    }
  }

  @Test
  public void test25_異常系_ファイル存在_先パス存在時処理_WARN() throws Exception {
    String filePath = SFTP_ROOT_PATH + "/testfile.txt";

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_CREATE_DIR", "resources.ecuacion.jp", null, null, null, null, null, filePath, "TRUE",
        "FALSE", "WARN", null);
    HousekeepFilesForm form = getForm(rec);

    sftpCreateDir(channel, SFTP_ROOT_PATH);
    sftpCreateFile(channel, filePath);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (RuntimeException ex) {
      BizLogicAppException blEx = (BizLogicAppException) ex.getCause();
      assertEquals("MSG_ERR_DEST_PATH_IS_FILE", blEx.getMessageId());
    }
  }

  @Test
  public void test26_異常系_ファイル存在_先パス存在時処理_ERROR() throws Exception {
    String filePath = SFTP_ROOT_PATH + "/testfile.txt";

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_CREATE_DIR", "resources.ecuacion.jp", null, null, null, null, null, filePath, "TRUE",
        "FALSE", "ERROR", null);
    HousekeepFilesForm form = getForm(rec);

    sftpCreateDir(channel, SFTP_ROOT_PATH);
    sftpCreateFile(channel, filePath);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();

    } catch (RuntimeException ex) {
      BizLogicAppException blEx = (BizLogicAppException) ex.getCause();
      assertEquals("MSG_ERR_DEST_PATH_IS_FILE", blEx.getMessageId());
    }
  }

  @Test
  public void test31_正常系_1階層() throws Exception {
    String dir = SFTP_ROOT_PATH + "/test-dir";

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_CREATE_DIR", "resources.ecuacion.jp", null, null, null, null, null, dir, "TRUE",
        "FALSE", "ERROR", null);
    HousekeepFilesForm form = getForm(rec);

    new HousekeepFilesBlf().execute(form);

    sftpExists(channel, dir);
  }

  @Test
  public void test32_正常系_複数階層() throws Exception {
    String dir = SFTP_ROOT_PATH + "/test-dir/1/2/3";

    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
        "SFTP_CREATE_DIR", "resources.ecuacion.jp", null, null, null, null, null, dir, "TRUE",
        "FALSE", "ERROR", null);
    HousekeepFilesForm form = getForm(rec);

    new HousekeepFilesBlf().execute(form);

    sftpExists(channel, dir);
  }
}

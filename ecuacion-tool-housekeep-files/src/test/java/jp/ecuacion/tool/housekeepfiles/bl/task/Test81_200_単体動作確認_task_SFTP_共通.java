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
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Test81_200_単体動作確認_task_SFTP_共通 extends TestTool {

  private AbstractTaskSftp task = new AbstractTaskSftp() {
    @Override
    protected void doSpecificTask(ConnectionToRemoteServer connection,
        HousekeepFilesTaskRecord taskRec, String fromPath, String toPath,
        List<AppException> warnList) throws Exception {}

    @Override
    public void taskDependentCheck(HousekeepFilesTaskRecord taskRec,
        List<SingleAppException> exList) {}

    @Override
    public TaskActionKindEnum getTaskActionKind() {
      return null;
    }

    @Override
    public Boolean isSrcPathLocal() {
      return null;
    }

    @Override
    public Boolean isDestPathLocal() {
      return null;
    }
  };

  /** パスに乱数を使用し存在しないパス文字列を作成 */
  private String getTestRootPath() {
    return SFTP_ROOT_PATH + "/" + new Random().nextLong();
  }

  @BeforeAll
  private static void beforeAll() throws JSchException {
    beforeAllOnSftpTest();
  }

  @BeforeEach
  private void beforeEach() throws Exception {
    // testToolのbeforeEachOnSftpTestはここでテストするメソッドを使用した処理が入っており、それだとテストの意義がわからなくなるので使用しない。
    channel = connectChannelSftp(session);
  }

  @AfterEach
  private void afterEach() {
    afterEachOnSftpTest();
  }

  @AfterAll
  protected static void afterAll() {
    afterAllOnSftpTest();
  }

  private List<LsEntry> checkExistenceOfDotAndDoubleDotThenReturnElse(List<LsEntry> list) {
    List<LsEntry> rtnList = new ArrayList<>(list);

    // "." の存在を確認の上listから除去
    List<LsEntry> targetList = list.stream().filter(e -> e.getFilename().equals(".")).toList();
    assertEquals(1, targetList.size());
    rtnList.remove(targetList.get(0));

    // ".." の存在を確認の上listから除去
    targetList = list.stream().filter(e -> e.getFilename().equals("..")).toList();
    assertEquals(1, targetList.size());
    rtnList.remove(targetList.get(0));

    // のこりをreturn
    return rtnList;
  }

  private void createTestRootDir(String testRootPath) {
    // ディレクトリがなければ作成、あればスキップ
    try {
      channel.mkdir(SFTP_ROOT_PATH);
    } catch (Exception ex) {

    }

    // ディレクトリがなければ作成、あればスキップ
    try {
      channel.mkdir(testRootPath);
    } catch (Exception ex) {

    }
  }

  // @Test
  // public void test01_session_異常系_リモート接続エラー() throws Exception {
  // try {
  // sftpWrongConnectSession();
  // fail();
  //
  // } catch (JSchException ex) {
  // // ここを通ればOK。
  //
  // } finally {
  // session = null;
  // sftpConnectSession();
  // }
  // }

  @Test
  public void testa1_getRemoteAll_異常系_存在なし() throws Exception {
    String testRootpath = getTestRootPath();
    List<LsEntry> list = task.getRemoteAll(channel, testRootpath);
    assertEquals(0, list.size());
  }

  @Test
  public void testa2_getRemoteAll_正常系_ファイル存在() throws Exception {
    String testRootpath = getTestRootPath();
    String testFilePath = testRootpath + "/testfile";
    createTestRootDir(testRootpath);
    channel.put(new ByteArrayInputStream("".getBytes()), testFilePath);

    try {
      List<LsEntry> list = task.getRemoteAll(channel, testRootpath);
      assertEquals(3, list.size());
      list = checkExistenceOfDotAndDoubleDotThenReturnElse(list);
      LsEntry entry = list.get(0);
      assertEquals("testfile", entry.getFilename());
      assertEquals(false, entry.getAttrs().isDir());

    } finally {
      channel.rm(testFilePath);
      channel.rmdir(testRootpath);
    }
  }

  @Test
  public void testa3_getRemoteAll_正常系_ディレクトリ存在() throws Exception {
    String testRootPath = getTestRootPath();
    createTestRootDir(testRootPath);

    try {
      List<LsEntry> list = task.getRemoteAll(channel, testRootPath);
      assertEquals(2, list.size());
      list = checkExistenceOfDotAndDoubleDotThenReturnElse(list);
      assertEquals(0, list.size());

    } finally {
      channel.rmdir(testRootPath);
    }
  }

  @Test
  public void testa4_getRemoteAll_正常系_ディレクトリ存在で子ファイルとディレクトリ存在() throws Exception {
    String testRootPath = getTestRootPath();
    createTestRootDir(testRootPath);
    // 子を作成
    String childFilePath1 = testRootPath + "/childFile1.txt";
    String childFilePath2 = testRootPath + "/childFile2.txt";
    String childDirPath1 = testRootPath + "/childDir1";
    String childDirPath2 = testRootPath + "/childDir2";
    channel.put(new ByteArrayInputStream("".getBytes()), childFilePath1);
    channel.put(new ByteArrayInputStream("".getBytes()), childFilePath2);
    channel.mkdir(childDirPath1);
    channel.mkdir(childDirPath2);

    List<LsEntry> list = task.getRemoteAll(channel, testRootPath);

    assertEquals(6, list.size());
    list = checkExistenceOfDotAndDoubleDotThenReturnElse(list);
    List<String> fileNameList = list.stream().map(e -> e.getFilename()).toList();
    assertTrue(fileNameList.contains("childFile1.txt"));
    assertTrue(fileNameList.contains("childFile2.txt"));
    assertTrue(fileNameList.contains("childDir1"));
    assertTrue(fileNameList.contains("childDir2"));
  }

  @Test
  public void testb1_remoteExists_正常系_存在なし() throws Exception {
    String testRootPath = getTestRootPath();
    boolean bl = task.remoteExists(channel, testRootPath);
    assertEquals(false, bl);
  }

  @Test
  public void testb2_remoteExists_正常系_ファイル存在() throws Exception {
    String testRootPath = getTestRootPath();
    String testFilePath = testRootPath + "/testfile";
    createTestRootDir(testRootPath);
    channel.put(new ByteArrayInputStream("".getBytes()), testFilePath);

    boolean bl = task.remoteExists(channel, testFilePath);
    assertEquals(true, bl);

    channel.rm(testFilePath);
    channel.rmdir(testRootPath);
  }

  @Test
  public void testb3_remoteExists_正常系_ディレクトリ存在() throws Exception {
    String testRootPath = getTestRootPath();
    createTestRootDir(testRootPath);

    boolean bl = task.remoteExists(channel, testRootPath);
    assertEquals(true, bl);

    channel.rmdir(testRootPath);
  }

  @Test
  public void testc1_remoteDirExists_正常系_存在なし() throws Exception {
    String testRootPath = getTestRootPath();
    boolean bl = task.remoteDirExists(channel, testRootPath);
    assertEquals(false, bl);
  }

  @Test
  public void testc2_remoteDirExists_正常系_ファイル存在() throws Exception {
    String testRootPath = getTestRootPath();
    String testFilePath = testRootPath + "/testfile";
    createTestRootDir(testRootPath);
    channel.put(new ByteArrayInputStream("".getBytes()), testFilePath);

    boolean bl = task.remoteDirExists(channel, testFilePath);
    assertEquals(false, bl);

    channel.rm(testFilePath);
    channel.rmdir(testRootPath);
  }

  @Test
  public void testc3_remoteDirExists_正常系_ディレクトリ存在() throws Exception {
    String testRootPath = getTestRootPath();
    createTestRootDir(testRootPath);

    boolean bl = task.remoteDirExists(channel, testRootPath);
    assertEquals(true, bl);

    channel.rmdir(testRootPath);
  }

  @Test
  public void testd1_remoteFileExists_正常系_存在なし() throws Exception {
    String testRootPath = getTestRootPath();
    boolean bl = task.remoteFileExists(channel, testRootPath);
    assertEquals(false, bl);
  }

  @Test
  public void testd2_remoteFileExists_正常系_ファイル存在() throws Exception {
    String testRootPath = getTestRootPath();
    String testFilePath = testRootPath + "/testfile";
    createTestRootDir(testRootPath);
    channel.put(new ByteArrayInputStream("".getBytes()), testFilePath);

    boolean bl = task.remoteFileExists(channel, testFilePath);
    assertEquals(true, bl);

    channel.rm(testFilePath);
    channel.rmdir(testRootPath);
  }

  @Test
  public void testd3_remoteFileExists_正常系_ディレクトリ存在() throws Exception {
    String testRootPath = getTestRootPath();
    createTestRootDir(testRootPath);

    boolean bl = task.remoteFileExists(channel, testRootPath);
    assertEquals(false, bl);

    channel.rmdir(testRootPath);
  }

  @Test
  public void teste1_getRemoteDetail_正常系_存在なし() throws Exception {
    String testRootPath = getTestRootPath();
    LsEntry entry = task.getRemoteDetail(channel, testRootPath);
    assertEquals(null, entry);
  }

  @Test
  public void teste2_getRemoteDetail_正常系_ファイル存在() throws Exception {
    String testRootPath = getTestRootPath();
    String testFilePath = testRootPath + "/testfile";
    createTestRootDir(testRootPath);
    channel.put(new ByteArrayInputStream("".getBytes()), testFilePath);

    LsEntry entry = task.getRemoteDetail(channel, testFilePath);
    assertEquals("testfile", entry.getFilename());
    assertEquals(false, entry.getAttrs().isDir());

    channel.rm(testFilePath);
    channel.rmdir(testRootPath);
  }

  @Test
  public void teste3_getRemoteDetail_正常系_ディレクトリ存在() throws Exception {
    String testRootPath = getTestRootPath();
    createTestRootDir(testRootPath);

    LsEntry entry = task.getRemoteDetail(channel, testRootPath);
    assertEquals(".", entry.getFilename());
    assertEquals(true, entry.getAttrs().isDir());

    channel.rmdir(testRootPath);
  }

  @Test
  public void testf1_getRemoteDirChildrenList_正常系_存在なし() throws Exception {
    String testRootPath = getTestRootPath();
    try {
      task.getRemoteDirChildrenList(channel, testRootPath);
      fail();

    } catch (RuntimeException ex) {
      
    }
  }

  @Test
  public void testf2_getRemoteDirChildrenList_異常系_ファイル存在() throws Exception {
    String testRootPath = getTestRootPath();
    String testFilePath = testRootPath + "/testfile";
    createTestRootDir(testRootPath);
    channel.put(new ByteArrayInputStream("".getBytes()), testFilePath);

    try {
      task.getRemoteDirChildrenList(channel, testFilePath);
      fail();

    } catch (RuntimeException ex) {
      
    }

    channel.rm(testFilePath);
    channel.rmdir(testRootPath);
  }

  @Test
  public void testf3_getRemoteDirChildrenList_正常系_ディレクトリ存在子なし() throws Exception {
    String testRootPath = getTestRootPath();
    createTestRootDir(testRootPath);

    List<LsEntry> list = task.getRemoteDirChildrenList(channel, testRootPath);
    assertEquals(0, list.size());
    
    channel.rmdir(testRootPath);
  }

  @Test
  public void testf4_getRemoteDirChildrenList_正常系_ディレクトリ存在子あり() throws Exception {
    String testRootPath = getTestRootPath();
    createTestRootDir(testRootPath);
    // 子を作成
    String childFilePath1 = testRootPath + "/childFile1.txt";
    String childDirPath1 = testRootPath + "/childDir1";
    channel.put(new ByteArrayInputStream("".getBytes()), childFilePath1);
    channel.mkdir(childDirPath1);

    List<LsEntry> list = task.getRemoteDirChildrenList(channel, testRootPath);
    assertEquals(2, list.size());
    List<String> strList = list.stream().map(e -> e.getFilename()).toList();
    assertTrue(strList.contains("childFile1.txt"));
    assertTrue(strList.contains("childDir1"));
    
    channel.rm(childFilePath1);
    channel.rmdir(childDirPath1);
    channel.rmdir(testRootPath);
  }

  @Test
  public void testg1_getRemoteDirChildrenNameList_異常系_存在なし() throws Exception {
    String testRootPath = getTestRootPath();
    try {
      task.getRemoteDirChildrenNameList(channel, testRootPath);
      fail();

    } catch (RuntimeException ex) {
      
    }
  }

  @Test
  public void testg2_getRemoteDirChildrenNameList_異常系_ファイル存在() throws Exception {
    String testRootPath = getTestRootPath();
    String testFilePath = testRootPath + "/testfile";
    createTestRootDir(testRootPath);
    channel.put(new ByteArrayInputStream("".getBytes()), testFilePath);

    try {
      task.getRemoteDirChildrenNameList(channel, testFilePath);
      fail();

    } catch (RuntimeException ex) {
      
    }

    channel.rm(testFilePath);
    channel.rmdir(testRootPath);
  }

  @Test
  public void testg3_getRemoteDirChildrenNameList_正常系_ディレクトリ存在子なし() throws Exception {
    String testRootPath = getTestRootPath();
    createTestRootDir(testRootPath);

    List<String> list = task.getRemoteDirChildrenNameList(channel, testRootPath);
    assertEquals(0, list.size());
    
    channel.rmdir(testRootPath);
  }

  @Test
  public void testg4_getRemoteDirChildrenNameList_正常系_ディレクトリ存在子あり() throws Exception {
    String testRootPath = getTestRootPath();
    createTestRootDir(testRootPath);
    // 子を作成
    String childFilePath1 = testRootPath + "/childFile1.txt";
    String childDirPath1 = testRootPath + "/childDir1";
    channel.put(new ByteArrayInputStream("".getBytes()), childFilePath1);
    channel.mkdir(childDirPath1);

    List<String> list = task.getRemoteDirChildrenNameList(channel, testRootPath);
    assertEquals(2, list.size());
    assertTrue(list.contains("childFile1.txt"));
    assertTrue(list.contains("childDir1"));
    
    channel.rm(childFilePath1);
    channel.rmdir(childDirPath1);
    channel.rmdir(testRootPath);
  }

}

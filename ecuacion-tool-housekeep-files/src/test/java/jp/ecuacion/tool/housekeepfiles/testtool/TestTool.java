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
package jp.ecuacion.tool.housekeepfiles.testtool;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.tool.housekeepfiles.TestTools;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTaskSftp;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class TestTool extends TestTools {

  private static String TEST_MSG = "abc";

  protected static String TEST_HOME_PATH = "target/test-home";
  protected static File TEST_HOME = new File(TEST_HOME_PATH);

  // 個別テストクラスで使用しがちなのでここで定義

  protected String getCurDirPath() {
    return Paths.get("").toAbsolutePath().toString();
  }

  //
  // befure / after関連
  //

  @BeforeEach
  protected void beforeEachInTestTool() throws IOException {
    initTestDir();
  }

  @AfterEach
  protected void afterEachInTestTool() throws IOException {
    deleteTestDir();
  }

  //
  // TEST_HOMEディレクトリの管理
  //

  protected void initTestDir() throws IOException {
    deleteTestDir();
    TEST_HOME.mkdirs();
  }

  private static void deleteTestDir() throws IOException {
    if (TEST_HOME.exists()) {
      FileUtils.deleteDirectory(TEST_HOME);
    }
  }

  //
  // textファイルに文字列を記載し、それが処理後に変更されていないことを確認するためのmethod群。
  //

  /** 指定のfileに書き込みをして新規作成。 */
  public void writeTestMsgToFile(File file) throws IOException {
    FileWriter fw = null;
    BufferedWriter bw = null;
    try {
      fw = new FileWriter(file);
      bw = new BufferedWriter(fw);
      bw.write(TEST_MSG);
    } finally {
      if (bw != null) {
        bw.close();
      }

      if (fw != null) {
        fw.close();
      }
    }
  }

  /** ファイルの中身をチェック。 */
  public void checkFileContentIfTestMsgExists(File file) throws IOException {
    // ファイルの内容を確認
    FileReader fr = null;
    BufferedReader br = null;
    try {
      fr = new FileReader(file);
      br = new BufferedReader(fr);
      String str = br.readLine();
      assertTrue(str.equals("abc"));
    } catch (Exception e) {
      fail();
    } finally {
      if (br != null) {
        br.close();
      }

      if (fr != null) {
        fr.close();
      }
    }
  }

  //
  // sftp関連
  //

  protected static String SFTP_ROOT_PATH = "/share/ecuacion-util-housekeep-files-test";

  /** task内に持っているsftp処理を借りるため、taskを作成しておく。 */
  protected AbstractTaskSftp sftpTask = new AbstractTaskSftp() {

    @Override
    protected void doSpecificTask(ConnectionToRemoteServer connection,
        HousekeepFilesTaskRecord taskRec, String fromPath, String toPath,
        List<AppException> warnList) throws Exception {}

    @Override
    public void taskDependentCheck(HousekeepFilesTaskRecord taskRec,
        List<SingleAppException> exList) {}

    @Override
    public TaskActionKindEnum getTaskActionKind() {
      return TaskActionKindEnum.change;
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

  protected static Session session;
  protected ChannelSftp channel;

  protected static void beforeAllOnSftpTest() throws JSchException {
    sftpConnectSession();
  }

  protected void beforeEachOnSftpTest() throws Exception {
    channel = connectChannelSftp(session);

    sftpRmAll(channel, SFTP_ROOT_PATH);
    sftpCreateDir(channel, SFTP_ROOT_PATH);
  }

  protected void afterEachOnSftpTest() {
    if (channel != null && !channel.isClosed()) {
      channel.disconnect();
    }
  }

  protected static void afterAllOnSftpTest() {
//    if (session != null) {
//      session.disconnect();
//    }
  }

  protected static void sftpConnectSession() throws JSchException {

    String hostname = "resources.ecuacion.jp";
    int port = 20022;
    String userId = "test_user";
    String password = "pass";

    if (session != null) {
      return;
    }

    final JSch jsch = new JSch();
    // Session設定
    session = jsch.getSession(userId, hostname, port);
    session.setPassword(password);

    Properties config = new java.util.Properties();
    config.put("StrictHostKeyChecking", "no");
    session.setConfig(config);

    session.connect();
  }

  /**
   * test用に、接続エラーとなるsesion取得処理。後続のテストで問題とならないよう、 テストの最後にsession = null; sftpConnectSession();
   * を代入すること。
   */
  protected static void sftpWrongConnectSession() throws JSchException {

    String hostname = "resources.ecuacion.jp";
    int port = 20022;
    String userId = "test_user";
    String password = "wrongPass";

    final JSch jsch = new JSch();
    // Session設定
    session = jsch.getSession(userId, hostname, port);
    session.setPassword(password);

    Properties config = new java.util.Properties();
    config.put("StrictHostKeyChecking", "no");
    session.setConfig(config);

    session.connect();
  }

  /**
   * SFTPのChannelを開始
   *
   * @param session 開始されたSession情報
   */
  protected ChannelSftp connectChannelSftp(final Session session) throws JSchException {
    final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
    channel.connect();

    return channel;
  }

  public boolean sftpExists(ChannelSftp channel, String dirPath) throws SftpException {
    return sftpTask.remoteExists(channel, dirPath);
  }
  
  public LsEntry sftpLsSelfDetail(ChannelSftp channel, String dirPath) throws SftpException {
    return sftpTask.getRemoteDetail(channel, dirPath);
  }

  public List<String> sftpLsChildren(ChannelSftp channel, String dirPath) throws SftpException {
    return sftpTask.getRemoteDirChildrenNameList(channel, dirPath);
  }

  public List<LsEntry> sftpLsChildrenDetail(ChannelSftp channel, String dirPath)
      throws SftpException {
    return sftpTask.getRemoteDirChildrenList(channel, dirPath);
  }

  public void sftpCreateDir(ChannelSftp channel, String dirPath)
      throws JSchException, SftpException {

    if (!sftpExists(channel, dirPath)) {
      channel.mkdir(dirPath);
    }
  }

  protected void sftpPutFile(ChannelSftp channel, final String sourceFilePath,
      final String destFilePath) throws JSchException, SftpException {
    try {
      channel.put(sourceFilePath, destFilePath);

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  protected void sftpCreateFile(ChannelSftp channel, String filePath) {
    try {
      channel.put(new ByteArrayInputStream("".getBytes()), filePath);

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }


  public void sftpRmFile(ChannelSftp channel, String filePath) {
    try {
      if (sftpExists(channel, filePath)) {
        channel.rm(filePath);
      }

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void sftpRmDir(ChannelSftp channel, String dirPath) {
    try {
      if (sftpExists(channel, dirPath)) {
        channel.rmdir(dirPath);
      }

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void sftpRmAll(ChannelSftp channel, String path) throws Exception {
    if (!sftpExists(channel, path)) {
      return;
    }

    List<String> fileList = sftpLsChildren(channel, path);
    if (fileList.size() == 0) {
      return;
    }

    try {
      if (!sftpExists(channel, path)) {
        return;
      }

      rmAllRecursively(channel, path);

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private void rmAllRecursively(ChannelSftp channel, String path) throws SftpException {
    List<LsEntry> lsEntries = sftpLsChildrenDetail(channel, path);
    for (LsEntry entry : lsEntries) {
      String childPath = FileUtil.concatFilePaths(path, entry.getFilename());
      if (entry.getAttrs().isDir()) {
        rmAllRecursively(channel, childPath);

      } else {
        channel.rm(childPath);
      }
    }

    LsEntry me = sftpLsSelfDetail(channel, path);
    if (me.getAttrs().isDir()) {
      sftpRmDir(channel, path);
    } else {
      sftpRmFile(channel, path);
    }
  }
}

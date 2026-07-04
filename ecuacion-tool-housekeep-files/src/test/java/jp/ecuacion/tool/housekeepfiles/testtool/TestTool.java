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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.TestTools;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTaskSftp;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@SuppressWarnings("null")
public class TestTool extends TestTools {

  private static String TEST_MSG = "abc";

  protected static String TEST_HOME_PATH = "target/test-home";
  protected static File TEST_HOME = new File(TEST_HOME_PATH);

  // Defined here because it is frequently used across individual test classes.

  protected String getCurDirPath() {
    return Paths.get("").toAbsolutePath().toString();
  }

  //
  // Before/After related.
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
  // Management of the TEST_HOME directory.
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
  // Methods for writing strings to text files and verifying they are unchanged after processing.
  //

  /** Write to the specified file and create it. */
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

  /** Check the file contents. */
  public void checkFileContentIfTestMsgExists(File file) throws IOException {
    // Verify file contents.
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
  // SFTP related.
  //

  protected static String SFTP_ROOT_PATH = "/share/ecuacion-util-housekeep-files-test";

  /** Create a task to borrow SFTP processing from it. */
  protected AbstractTaskSftp sftpTask = new AbstractTaskSftp() {

    @Override
    protected void doSpecificTask(ConnectionToRemoteServer connection,
        HousekeepFilesTaskRecord taskRec, String fromPath, String toPath,
        List<BusinessViolation> warnList) throws Exception {}

    @Override
    public void taskDependentCheck(HousekeepFilesTaskRecord taskRec, Violations violations) {}

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

  /** Hostname of the embedded SFTP server used in tests. */
  protected static String SFTP_HOST = "localhost";

  /** Port of the embedded SFTP server; assigned dynamically at startup. */
  protected static int SFTP_PORT;

  private static SshServer embeddedSftpServer;
  protected static Session session;
  protected ChannelSftp channel;

  protected static void beforeAllOnSftpTest() throws JSchException {
    startEmbeddedSftpServer();
    sftpConnectSession();
  }

  private static void startEmbeddedSftpServer() {
    if (embeddedSftpServer != null) {
      return;
    }

    try {
      Path sftpRoot = Paths.get("target/sftp-root").toAbsolutePath();
      // Pre-create parent directories so SFTP_ROOT_PATH mkdir succeeds.
      Files.createDirectories(sftpRoot.resolve("share"));

      embeddedSftpServer = SshServer.setUpDefaultServer();
      embeddedSftpServer.setPort(0);
      embeddedSftpServer.setKeyPairProvider(
          new SimpleGeneratorHostKeyProvider(Paths.get("target/sftp-hostkey.ser")));
      embeddedSftpServer.setPasswordAuthenticator(
          (username, password, sess) ->
              "test_user".equals(username) && "pass".equals(password));
      embeddedSftpServer.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
      embeddedSftpServer.setFileSystemFactory(new VirtualFileSystemFactory(sftpRoot));
      embeddedSftpServer.start();

      SFTP_PORT = embeddedSftpServer.getPort();

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          embeddedSftpServer.stop(true);
        } catch (IOException e) {
          // ignore on shutdown
        }
      }));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void beforeEachOnSftpTest() throws Exception {
    if (session == null || !session.isConnected()) {
      session = null;
      sftpConnectSession();
    }
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
    if (session != null) {
      return;
    }

    final JSch jsch = new JSch();
    session = jsch.getSession("test_user", SFTP_HOST, SFTP_PORT);
    session.setPassword("pass".getBytes(StandardCharsets.UTF_8));

    Properties config = new java.util.Properties();
    config.put("StrictHostKeyChecking", "no");
    session.setConfig(config);

    session.connect();
  }

  /**
   * Session retrieval that intentionally fails for testing. To avoid affecting subsequent tests,
   * assign session = null; sftpConnectSession(); at the end of the test.
   */
  protected static void sftpWrongConnectSession() throws JSchException {
    final JSch jsch = new JSch();
    session = jsch.getSession("test_user", SFTP_HOST, SFTP_PORT);
    session.setPassword("wrongPass".getBytes(StandardCharsets.UTF_8));

    Properties config = new java.util.Properties();
    config.put("StrictHostKeyChecking", "no");
    session.setConfig(config);

    session.connect();
  }

  /**
   * Start the SFTP channel.
   *
   * @param session the started Session
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
    if (me == null) {
      return;
    }
    if (me.getAttrs().isDir()) {
      sftpRmDir(channel, path);
    } else {
      sftpRmFile(channel, path);
    }
  }
}

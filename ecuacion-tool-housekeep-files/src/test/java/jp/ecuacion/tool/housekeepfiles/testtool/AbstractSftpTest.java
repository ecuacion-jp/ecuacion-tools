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
import java.io.ByteArrayInputStream;
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
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bl.task.AbstractTaskSftp;
import jp.ecuacion.tool.housekeepfiles.constant.Constants;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for tests which need an SFTP server.
 *
 * <p>Starts an embedded SFTP server (Apache MINA SSHD) once per test run, keeps a shared JSch
 * session to it, and opens a fresh {@link ChannelSftp} before each test. The directory
 * {@link #SFTP_ROOT_PATH} is emptied and recreated before each test so tests start from a clean
 * remote state; subclasses testing the SFTP helper methods themselves can override
 * {@link #prepareRemoteTestRoot()} since the cleanup internally relies on those methods.</p>
 */
@SuppressWarnings("null")
public abstract class AbstractSftpTest {

  /** Hostname of the embedded SFTP server used in tests. */
  protected static final String SFTP_HOST = "localhost";

  protected static final String SFTP_USER = "test_user";

  protected static final String SFTP_PASSWORD = "pass";

  /** Root directory on the embedded server under which each test creates its files. */
  protected static final String SFTP_ROOT_PATH = "/share/ecuacion-util-housekeep-files-test";

  /** Port of the embedded SFTP server; assigned dynamically at startup. */
  protected static int sftpPort;

  private static SshServer embeddedSftpServer;

  protected static Session session;

  protected ChannelSftp channel;

  /**
   * Task instance to borrow the SFTP access methods defined in {@link AbstractTaskSftp} from it.
   */
  private final AbstractTaskSftp sftpTask = new AbstractTaskSftp() {

    @Override
    protected void doSpecificTask(@Nullable ConnectionToRemoteServer connection,
        HousekeepFilesTaskRecord taskRec, @Nullable String fromPath, @Nullable String toPath,
        List<BusinessViolation> warnList) throws Exception {}

    @Override
    public void taskDependentCheck(HousekeepFilesTaskRecord taskRec, Violations violations) {}

    @Override
    public TaskActionKindEnum getTaskActionKind() {
      return TaskActionKindEnum.change;
    }

    @Override
    public @Nullable Boolean isSrcPathLocal() {
      return null;
    }

    @Override
    public @Nullable Boolean isDestPathLocal() {
      return null;
    }
  };

  @BeforeAll
  static void startSftpServerAndSession() throws JSchException {
    // The embedded server below is a throwaway instance freshly started per test run, so its host
    // key can never be pre-registered in ~/.ssh/known_hosts. Disable the production code's strict
    // host key check, which is otherwise on by default for exactly this kind of accidental
    // weakening.
    System.setProperty(Constants.PROP_SFTP_STRICT_HOST_KEY_CHECKING, "false");
    startEmbeddedSftpServer();
    connectSession();
  }

  @AfterAll
  static void restoreStrictHostKeyChecking() {
    System.clearProperty(Constants.PROP_SFTP_STRICT_HOST_KEY_CHECKING);
  }

  @BeforeEach
  void openChannelAndPrepareRemoteTestRoot() throws Exception {
    if (session == null || !session.isConnected()) {
      session = null;
      connectSession();
    }

    channel = connectChannelSftp(session);
    prepareRemoteTestRoot();
  }

  @AfterEach
  void closeChannel() {
    if (channel != null && !channel.isClosed()) {
      channel.disconnect();
    }
  }

  /**
   * Empties and recreates {@link #SFTP_ROOT_PATH} before each test.
   *
   * <p>Relies on the SFTP access methods of {@link AbstractTaskSftp}, so tests targeting those
   * methods themselves must override this with an empty implementation.</p>
   */
  protected void prepareRemoteTestRoot() throws Exception {
    sftpRmAll(SFTP_ROOT_PATH);
    sftpCreateDir(channel, SFTP_ROOT_PATH);
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
          (username, password, sess) -> SFTP_USER.equals(username) && SFTP_PASSWORD.equals(password));
      embeddedSftpServer.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
      embeddedSftpServer.setFileSystemFactory(new VirtualFileSystemFactory(sftpRoot));
      embeddedSftpServer.start();

      sftpPort = embeddedSftpServer.getPort();

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

  private static void connectSession() throws JSchException {
    if (session != null) {
      return;
    }

    final JSch jsch = new JSch();
    session = jsch.getSession(SFTP_USER, SFTP_HOST, sftpPort);
    session.setPassword(SFTP_PASSWORD.getBytes(StandardCharsets.UTF_8));

    Properties config = new Properties();
    config.put("StrictHostKeyChecking", "no");
    session.setConfig(config);

    session.connect();
  }

  /** Opens an SFTP channel on the started session. */
  protected ChannelSftp connectChannelSftp(final Session session) throws JSchException {
    final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
    channel.connect();

    return channel;
  }

  protected boolean sftpExists(ChannelSftp channel, String path) throws SftpException {
    return sftpTask.remoteExists(channel, path);
  }

  protected LsEntry sftpLsSelfDetail(ChannelSftp channel, String path) throws SftpException {
    return sftpTask.getRemoteDetail(channel, path);
  }

  protected List<String> sftpLsChildren(ChannelSftp channel, String dirPath) throws SftpException {
    return sftpTask.getRemoteDirChildrenNameList(channel, dirPath);
  }

  protected List<LsEntry> sftpLsChildrenDetail(ChannelSftp channel, String dirPath)
      throws SftpException {
    return sftpTask.getRemoteDirChildrenList(channel, dirPath);
  }

  protected void sftpCreateDir(ChannelSftp channel, String dirPath)
      throws JSchException, SftpException {

    if (!sftpExists(channel, dirPath)) {
      channel.mkdir(dirPath);
    }
  }

  protected void sftpCreateFile(ChannelSftp channel, String filePath) {
    try {
      channel.put(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)), filePath);

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Recursively removes the file or directory at {@code path} if it exists. */
  protected void sftpRmAll(String path) throws SftpException {
    LsEntry me = sftpLsSelfDetail(channel, path);
    if (me == null) {
      return;
    }

    if (me.getAttrs().isDir()) {
      for (LsEntry entry : sftpLsChildrenDetail(channel, path)) {
        sftpRmAll(FileUtil.concatFilePaths(path, entry.getFilename()));
      }

      channel.rmdir(path);

    } else {
      channel.rm(path);
    }
  }
}

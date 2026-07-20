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


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Vector;
import java.util.stream.Collectors;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToSftpServer;
import jp.ecuacion.tool.housekeepfiles.constant.Constants;
import jp.ecuacion.tool.housekeepfiles.dto.other.FileInfo;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.AuthTypeEnum;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import org.jspecify.annotations.Nullable;

/**
 * Provides abstract sftp tasks.
 */
public abstract class AbstractTaskSftp extends AbstractTaskRemote {

  @Override
  public String getConnectionProtocol() {
    return "SFTP";
  }

  /**
   * Executes task.
   */
  protected abstract void doSpecificTask(@Nullable ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, @Nullable String fromPath, @Nullable String toPath,
      List<BusinessViolation> warnList) throws Exception;

  // private int depth = 0;

  @Override
  protected void doTaskInternal(@Nullable ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, @Nullable String fromPath, @Nullable String toPath,
      List<BusinessViolation> warnList) {
    try {
      doSpecificTask(connection, taskRec, fromPath, toPath, warnList);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unused")
  @Override
  public ConnectionToSftpServer getConnection(String remoteHost,
      Map<String, HousekeepFilesAuthRecord> authMap) throws Exception {

    HousekeepFilesAuthRecord auth =
        Objects.requireNonNull(authMap.get(remoteHost + "-SFTP"));

    final String username = auth.getUserName();
    final String password = auth.getPassword();
    final int port = auth.getPort();

    java.util.Properties config = new java.util.Properties();

    JSch ssh = new JSch();

    // Verify the server's host key against the standard OpenSSH known_hosts file to prevent
    // man-in-the-middle attacks. The host key must be registered beforehand (e.g. via a manual
    // "ssh" connection or "ssh-keyscan"); connections to unknown hosts will fail by design.
    // Set Constants.PROP_SFTP_STRICT_HOST_KEY_CHECKING=false to disable this check, e.g. for
    // quick local trials against a throwaway server. Never disable it against a production or
    // otherwise untrusted network.
    if ("false"
        .equalsIgnoreCase(System.getProperty(Constants.PROP_SFTP_STRICT_HOST_KEY_CHECKING))) {
      dlog.warn("'" + Constants.PROP_SFTP_STRICT_HOST_KEY_CHECKING + "=false' is set. SFTP host "
          + "key verification is disabled, which allows man-in-the-middle attacks to go "
          + "undetected. This should never be used other than for quick, throwaway local trials.");
      config.put("StrictHostKeyChecking", "no");

    } else {
      config.put("StrictHostKeyChecking", "yes");
      ssh.setKnownHosts(System.getProperty("user.home") + "/.ssh/known_hosts");
    }

    addAuthTypeToConfig(auth.getAuthType(), config);

    if (auth.getAuthType() == AuthTypeEnum.KEY) {
      if (auth.getPassword() == null) {
        ssh.addIdentity(auth.getKeyPath());

      } else {
        ssh.addIdentity(auth.getKeyPath(),
            auth.getPassword().getBytes(StandardCharsets.UTF_8));
      }
    }

    Session sftpSession = ssh.getSession(username, remoteHost, port);
    sftpSession.setConfig(config);
    if (password != null) {
      sftpSession.setPassword(password.getBytes(StandardCharsets.UTF_8));
    }
    sftpSession.connect();

    Channel channel = sftpSession.openChannel("sftp");
    channel.connect();
    ChannelSftp sftpChannel = (ChannelSftp) channel;

    return new ConnectionToSftpServer(sftpSession, sftpChannel);
  }

  private void addAuthTypeToConfig(AuthTypeEnum authTypeEnum, Properties config) {
    Map<AuthTypeEnum, String> authNameMap = new HashMap<>();
    authNameMap.put(AuthTypeEnum.PASSWORD, "password");
    authNameMap.put(AuthTypeEnum.KEY, "publickey");
    authNameMap.put(AuthTypeEnum.KERBEROS, "gssapi-with-mic");

    config.put("PreferredAuthentications", authNameMap.get(authTypeEnum));
  }

  @Override
  protected @Nullable FileInfo getRemoteFileInfo(AbstractTask task,
      @Nullable ConnectionToRemoteServer connection, boolean isPathDir, String path) {
    List<FileInfo> list = getRemoteFileInfoList(task, connection, isPathDir, path);

    if (list == null || list.size() == 0) {
      return null;
    }

    for (FileInfo fi : list) {
      if (fi.getFilePath().equals(path)) {
        return fi;
      }
    }

    throw new RuntimeException("This point should never be reached.");
  }

  /**
   * Returns specified file or directory if exists.
   */
  @Override
  protected List<FileInfo> getRemoteFileInfoList(AbstractTask task,
      @Nullable ConnectionToRemoteServer connection, boolean isPathDir, String path) {
    List<FileInfo> rtnList = new ArrayList<FileInfo>();

    // For file/directory creation tasks, non-existence is the expected state,
    // so skip checks and return.
    if (task.getTaskActionKind() == TaskActionKindEnum.create) {
      return rtnList;
    }

    try {
      ChannelSftp sftpChannel =
          ((ConnectionToSftpServer) Objects.requireNonNull(connection)).getSftpChannel();

      // Check whether the file/directory exists exactly as specified by the path.
      try {
        SftpATTRS attrs = sftpChannel.stat(path);

        // If it exists: return that file/directory.
        rtnList.add(new FileInfo(path, true, ((long) attrs.getMTime() * 1000L), false));
        return rtnList;

      } catch (Exception e) {
        // No action. Proceed to the next step.
      }

      // The file/directory does not exist as specified in path - either truly absent or a wildcard
      // was used.

      // Check that the remote directory exists. Error if it does not.
      Vector<ChannelSftp.LsEntry> files;
      try {
        files = (Vector<ChannelSftp.LsEntry>) sftpChannel.ls(path);

      } catch (SftpException sftpEx) {
        dlog.error("*** If not exist, CREATE DIRECTORY : " + path);
        throw sftpEx;
      }

      // @SuppressWarnings("unchecked")
      // Vector<ChannelSftp.LsEntry> files = (Vector<ChannelSftp.LsEntry>) sftpChannel.ls(path);

      // This operation, when given a directory, retrieves "..", ".", and entries under it.
      List<ChannelSftp.LsEntry> list = files.stream()
          .filter(bean -> bean.getAttrs().isDir() == isPathDir).collect(Collectors.toList());
      if (list == null || list.size() == 0) {
        return new ArrayList<>();
      }

      // // For a directory spec, if "." exists that is correct; for a file spec, if "." exists,
      // // the path is a directory so the judgment is "no file found". Make that judgment first.
      // for (ChannelSftp.LsEntry file : list) {
      // if (file.getFilename().equals(".")) {
      // FileInfo dir = new FileInfo(path, true); List<FileInfo> flist = new ArrayList<>();
      // flist.add(dir);
      // return (isPathDir)? flist : null;
      // }
      // }

      for (ChannelSftp.LsEntry file : list) {
        FileInfo fi = new FileInfo();
        fi.setDirectory(file.getAttrs().isDir());
        String fullPath = null;

        if (fi.isDirectory()) {
          // Ignore the parent directory entry.
          if (file.getFilename().equals("..")) {
            continue;
          }

          // If self-entry exists, set the original path as-is.
          if (file.getFilename().equals(".")) {
            fullPath = path;

          } else {
            fullPath = FileUtil.cleanPathStrWithSlash(
                FileUtil.concatFilePaths(FileUtil.getParentDirPath(path), file.getFilename()));
          }

        } else {
          fullPath = FileUtil.cleanPathStrWithSlash(
              FileUtil.concatFilePaths(FileUtil.getParentDirPath(path), file.getFilename()));
        }

        // Normally, for a directory the path could be used as-is, but because wildcards may appear,
        // always go up one level and come back.
        // It would be ideal if ChannelSftp.LsEntry held the path, but it does not...
        fi.setFilePath(fullPath);

        // Checking for locks appears difficult, so always return false.
        fi.setLocked(false);
        fi.setLastUpdTimeInMillis(((long) file.getAttrs().getMTime()) * 1000L);
        rtnList.add(fi);
      }

      return rtnList;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * To distinguish between "folder exists but has no contents" and "folder does not exist or path
   * is a file", treat the latter as an error.
   */
  public List<String> getRemoteDirChildrenNameList(ChannelSftp channel, String dirPath)
      throws SftpException {
    if (!remoteDirExists(channel, dirPath)) {
      throw new RuntimeException(
          "Path not found or file (not directory) exists. dirPath: " + dirPath);
    }

    try {
      List<LsEntry> list = getRemoteDirChildrenList(channel, dirPath);
      return list.stream().map(e -> e.getFilename()).toList();

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * To distinguish between "folder exists but has no contents" and "folder does not exist or path
   * is a file", treat the latter as an error.
   */
  public List<LsEntry> getRemoteDirChildrenList(ChannelSftp channel, String dirPath)
      throws SftpException {
    if (!remoteDirExists(channel, dirPath)) {
      throw new RuntimeException(
          "Path not found or file (not directory) exists. dirPath: " + dirPath);
    }

    try {
      List<LsEntry> list = getRemoteAll(channel, dirPath);
      return list.stream().filter(f -> !f.getFilename().equals("."))
          .filter(f -> !f.getFilename().equals("..")).toList();

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Returns true if the remote directory exists at the specified path. */
  public boolean remoteDirExists(ChannelSftp channel, String path) throws SftpException {
    List<LsEntry> list = getRemoteAll(channel, path);
    if (list.size() == 0) {
      return false;
    }

    List<LsEntry> filteredList = list.stream().filter(f -> f.getFilename().equals(".")).toList();

    return filteredList.size() == 1;
  }

  /** Returns true if the remote file exists at the specified path. */
  public boolean remoteFileExists(ChannelSftp channel, String path) throws SftpException {
    List<LsEntry> list = getRemoteAll(channel, path);
    if (list.size() == 0) {
      return false;
    }

    return !remoteDirExists(channel, path);
  }

  /** Returns true if a file or directory exists at the specified remote path. */
  public boolean remoteExists(ChannelSftp channel, String path) throws SftpException {
    return getRemoteAll(channel, path).size() > 0;
  }

  /**
   * Returns the specified path as an LsEntry. Note that getFilename() returns "." for directories.
   */
  public @Nullable LsEntry getRemoteDetail(ChannelSftp channel, String path) throws SftpException {
    List<LsEntry> list = getRemoteAll(channel, path);
    if (list.size() == 0) {
      return null;
    }

    if (remoteDirExists(channel, path)) {
      List<LsEntry> filteredList = list.stream().filter(f -> f.getFilename().equals(".")).toList();
      return filteredList.get(0);

    } else {
      List<LsEntry> filteredList =
          list.stream().filter(f -> f.getFilename().equals(new File(path).getName())).toList();
      return filteredList.get(0);
    }
  }

  /**
   * Returns ".", ".." and all entries under path when it is a directory; returns a single entry
   * when it is a file. Returns an empty list when path does not exist. Because this method returns
   * raw data including ".." and ".", it is not intended for external callers, but is not private
   * so it can be used in tests.
   */
  List<LsEntry> getRemoteAll(ChannelSftp channel, String path) throws SftpException {
    try {
      Vector<LsEntry> lsEntries = channel.ls(path);
      return lsEntries.stream().toList();

    } catch (SftpException e) {
      if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
        return new ArrayList<>();

      } else {
        throw e;
      }
    }
  }
}

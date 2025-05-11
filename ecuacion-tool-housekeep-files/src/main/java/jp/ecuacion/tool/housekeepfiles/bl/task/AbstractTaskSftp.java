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

import static jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum.create;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.stream.Collectors;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToSftpServer;
import jp.ecuacion.tool.housekeepfiles.dto.other.FileInfo;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.AuthTypeEnum;

public abstract class AbstractTaskSftp extends AbstractTaskRemote {

  @Override
  public String getConnectionProtocol() {
    return "SFTP";
  }

  protected abstract void doSpecificTask(ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, String fromPath, String toPath, List<AppException> warnList)
      throws Exception;

  // private int depth = 0;

  @Override
  protected void doTaskInternal(ConnectionToRemoteServer connection,
      HousekeepFilesTaskRecord taskRec, String fromPath, String toPath,
      List<AppException> warnList) {
    try {
      doSpecificTask(connection, taskRec, fromPath, toPath, warnList);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ConnectionToSftpServer getConnection(String remoteHost,
      Map<String, HousekeepFilesAuthRecord> authMap) throws Exception {

    HousekeepFilesAuthRecord auth = authMap.get(remoteHost + "-SFTP");

    final String username = auth.getUserName();
    final String password = auth.getPassword();
    final int port = auth.getPort();

    java.util.Properties config = new java.util.Properties();
    config.put("StrictHostKeyChecking", "no");

    addAuthTypeToConfig(auth.getAuthType(), config);

    JSch ssh = new JSch();
    if (auth.getAuthType() == AuthTypeEnum.KEY) {
      if (auth.getPassword() == null) {
        ssh.addIdentity(auth.getKeyPath());

      } else {
        ssh.addIdentity(auth.getKeyPath(), auth.getPassword());
      }
    }

    Session sftpSession = ssh.getSession(username, remoteHost, port);
    sftpSession.setConfig(config);
    sftpSession.setPassword(password);
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
  protected FileInfo getRemoteFileInfo(AbstractTask task, ConnectionToRemoteServer connection,
      boolean isPathDir, String path) {
    List<FileInfo> list = getRemoteFileInfoList(task, connection, isPathDir, path);

    if (list == null || list.size() == 0) {
      return null;
    }

    for (FileInfo fi : list) {
      if (fi.getFilePath().equals(path)) {
        return fi;
      }
    }

    throw new RuntimeException("ここに到達することはない");
  }

  /**
   * ディレクトリ指定の場合は、そのディレクトリ配下に存在するファイル・ディレクトリ一覧を取得、ファイル指定の場合は、存在すればそのファイルを取得。
   * →ディレクトリでもそのディレクトリ自体を取得するよう変更
   */
  protected List<FileInfo> getRemoteFileInfoList(AbstractTask task,
      ConnectionToRemoteServer connection, boolean isPathDir, String path) {
    List<FileInfo> rtnList = new ArrayList<FileInfo>();

    // そもそもファイル／ディレクトリ作成のタスクの場合は、存在しないのが正常なのでチェックなどはせず終了。
    if (task.getTaskActionKind() == create) {
      return rtnList;
    }

    try {
      ChannelSftp sftpChannel = ((ConnectionToSftpServer) connection).getSftpChannel();

      // pathの記載通りのファイル／ディレクトリが存在するか確認
      try {
        SftpATTRS attrs = sftpChannel.stat(path);

        // 存在する場合：そのファイル・ディレクトリを返す
        rtnList.add(new FileInfo(path, true, ((long) attrs.getMTime() * 1000L), false));
        return rtnList;

      } catch (Exception e) {
        // 処理なし。次へ進む
      }

      // pathの記載通りのファイル／ディレクトリがない＝本当にないか、ワイルドカードが使用された場合。

      // remoteのディレクトリの存在チェック。存在しない場合はエラー。
      Vector<ChannelSftp.LsEntry> files;
      try {
        files = (Vector<ChannelSftp.LsEntry>) sftpChannel.ls(path);

      } catch (SftpException sftpEx) {
        dlog.error("★★★ If not exist, CREATE DIRECTORY : " + path);
        throw sftpEx;
      }

      // @SuppressWarnings("unchecked")
      // Vector<ChannelSftp.LsEntry> files = (Vector<ChannelSftp.LsEntry>) sftpChannel.ls(path);

      // この処理は、ディレクトリを指定すると、「..」、「.」及びその配下を取ってくる処理。
      List<ChannelSftp.LsEntry> list = files.stream()
          .filter(bean -> bean.getAttrs().isDir() == isPathDir).collect(Collectors.toList());
      if (list == null || list.size() == 0) {
        return new ArrayList<>();
      }

      // //ディレクトリ指定の場合は、「.」があればそれが正解だし、ファイル指定の場合は「.」があれば、
      // //指定のパスはディレクトリなので「ファイルはなかった」という判断になる。その判断を先にしてしまう。
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
          // parent dirは無視
          if (file.getFilename().equals("..")) {
            continue;
          }

          // 自分が存在したら、元のpathをそのまま設定
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

        // 普通ならdirectoryの場合はpathをそのまま使えばいいのだが、wildcardが入ってくる場合があるため必ず一度上に行ってから戻ってくる。
        // ChannelSftp.LsEntryにpathを持っていれば良いのだが、持っていないようだ・・・
        fi.setFilePath(fullPath);

        // ロックの確認は難しそうなので、常にfalseとしておく
        fi.setLocked(false);
        fi.setLastUpdTimeInMillis(((long) file.getAttrs().getMTime()) * 1000L);
        rtnList.add(fi);
      }

      return rtnList;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** フォルダはあるが配下のファイル／フォルダがないのと、フォルダがない、またはpathがファイルの場合を区別するため、後者はエラーとする。 */
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

  /** フォルダはあるが配下のファイル／フォルダがないのと、フォルダがない、またはpathがファイルの場合を区別するため、後者はエラーとする。 */
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

  public boolean remoteDirExists(ChannelSftp channel, String path) throws SftpException {
    List<LsEntry> list = getRemoteAll(channel, path);
    if (list.size() == 0) {
      return false;
    }

    List<LsEntry> filteredList = list.stream().filter(f -> f.getFilename().equals(".")).toList();

    return filteredList.size() == 1;
  }

  public boolean remoteFileExists(ChannelSftp channel, String path) throws SftpException {
    List<LsEntry> list = getRemoteAll(channel, path);
    if (list.size() == 0) {
      return false;
    }

    return !remoteDirExists(channel, path);
  }

  public boolean remoteExists(ChannelSftp channel, String path) throws SftpException {
    return getRemoteAll(channel, path).size() > 0;
  }

  /** 指定したパスをLsEntry形式で返す。 ディレクトリの場合はgetFilenam()が"."となるので注意。 */
  public LsEntry getRemoteDetail(ChannelSftp channel, String path) throws SftpException {
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
   * pathがディレクトリの場合は".", ".." 及び配下のファイル・ディレクトリを返す、ファイルの場合は1つのみを返す。 pathが存在しない場合はsizeゼロのlistを返す。 ".",
   * ".." を含めた生データを返すので使用しにくいことから外から呼べない形としている。ただしテストで使用するためprivateにはしない。
   */
  List<LsEntry> getRemoteAll(ChannelSftp channel, String path) throws SftpException {
    try {
      Vector<LsEntry> lsEntries = channel.ls(path);
      return lsEntries.stream().toList();

    } catch (SftpException e) {
      if ("No such file".equals(e.getMessage())) {
        return new ArrayList<>();

      } else {
        throw e;
      }
    }
  }
}

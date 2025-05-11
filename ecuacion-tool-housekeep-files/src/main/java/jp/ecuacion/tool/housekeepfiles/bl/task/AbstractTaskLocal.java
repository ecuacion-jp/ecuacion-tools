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

import java.util.List;
import java.util.Map;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.dto.other.FileInfo;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;

public abstract class AbstractTaskLocal extends AbstractTask {

  @Override
  public ConnectionToRemoteServer getConnection(String remoteServer,
      Map<String, HousekeepFilesAuthRecord> authMap) throws Exception {
    return null;
  }

  @Override
  public String getConnectionProtocol() {
    return null;
  }

  // @Override
  // public boolean makeToDir(ConnectionToRemoteServer connection, String dirPath) {
  // return makeLocalDirs(dirPath);
  // }
  //
  // /** localなのでremoteはない。呼ばれることはないが、実装しないと怒られるので常にfalseを返す処理として作成しておく。 */
  // @Override
  // public boolean makeRemoteDirs(ConnectionToRemoteServer connection, String path)
  // throws AppException {
  // return false;
  // }

  /** 呼ばれることはないので常にnullを返す。 */
  @Override
  protected FileInfo getRemoteFileInfo(AbstractTask task, ConnectionToRemoteServer connection,
      boolean isPathDir, String path) {
    return null;
  }

  /** 呼ばれることはないので常にnullを返す。 */
  @Override
  protected List<FileInfo> getRemoteFileInfoList(AbstractTask task,
      ConnectionToRemoteServer connection, boolean isPathDir, String path) {
    return null;
  }

  @Override
  public Boolean isSrcPathLocal() {
    return true;
  }

  /** toPath側は、設定の不要なtaskもあるので必須とはしない。default falseにしておく。 */
  public Boolean isDestPathLocal() {
    return true;
  }
}

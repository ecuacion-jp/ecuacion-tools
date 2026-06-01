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
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.BeforeEach;

public class Test81_301_UnitTest_task_Sftp_CopyFromServer extends TestTool {
  @SuppressWarnings("null")
  SftpCopyFromServer task;

  @BeforeEach
  public void before() throws IOException {
    initTestDir();

    task = new SftpCopyFromServer();
  }

  // private HousekeepFilesTaskRecord getTaskRecord(String remoteServer, String
  // isSrcPathDirEnumName,
  // String pathFrom, String isDestPathDirEnumName, String pathTo) {
  // return new HousekeepFilesTaskRecord("1", "aName", TaskPtnEnum.SFTP_COPY_FROM_SERVER.getName(),
  // remoteServer, isSrcPathDirEnumName, pathFrom, isDestPathDirEnumName, pathTo,
  // Integer.valueOf(Calendar.DAY_OF_MONTH).toString(), "0",
  // IncidentTreatedAsEnum.IGNORE.getName(), IncidentTreatedAsEnum.IGNORE.getName(), "FALSE");
  // }
  //
  // @Test
  // public void test01_valid_fromFile_toFolder() throws IOException {
  // String fromFilePath = "/var/www/html/tanpinkatsuyou/background.jpg";
  // String remoteServer = "dev.ecuacion.jp";
  //
  // new File(fu.concatFilePaths(TEST_HOME_PATH, "toDir")).mkdirs();
  // String toFilePath = fu.concatFilePaths(TEST_HOME_PATH, "toDir", "test.jpg");
  //
  // dtE = getTaskRecord(remoteServer, "FALSE", fromFilePath, "FALSE", toFilePath);
  //
  // HousekeepFilesAuthRecord auth = new HousekeepFilesAuthRecord(dtE.getRemoteServer(), "SFTP",
  // "5306", AuthTypeEnum.PASSWORD.getName(), "tomcat_user", "HtyT#tomc!Aster1s", null);
  // HashMap<String, HousekeepFilesAuthRecord> map = new HashMap<>();
  // map.put(auth.getRemoteServer() + "-SFTP", auth);
  //
  // try {
  // task.doTask(task.getConnection(remoteServer, map), dtE, fromFilePath, toFilePath);
  // } catch (Exception e) {
  // fail();
  // }
  // }
  //
  // @Test
  // public void testXX_valid_withKey_fromFile_toFolder() throws IOException {
  // String remoteServer = "resources.ecuacion.jp";
  //
  // String fromFilePath = "/var/log/yum-debug.log";
  // new File(fu.concatFilePaths(TEST_HOME_PATH, "toDir")).mkdirs();
  // String toFilePath = fu.concatFilePaths(TEST_HOME_PATH, "toDir", "yum-debug.log");
  //
  // dtE = getTaskRecord(remoteServer, "FALSE", fromFilePath, "FALSE", toFilePath);
  //
  // HousekeepFilesAuthRecord auth = new HousekeepFilesAuthRecord(remoteServer, "SFTP", "5306",
  // AuthTypeEnum.KEY.getName(), "file_share_user", "HtyT#flsr!Aster1s",
  // "C:/Users/yosuk_000/Dropbox/Asterisk/400_dev/81_server/resources-server/"
  // + "ssh-key/file_share_user-private-key-for-resouces-ssh-for-openssh.ppk");
  //
  // HashMap<String, HousekeepFilesAuthRecord> map = new HashMap<>();
  // map.put(auth.getRemoteServer() + "-SFTP", auth);
  //
  // try {
  // task.doTask(task.getConnection(remoteServer, map), dtE, fromFilePath, toFilePath);
  // } catch (Exception e) {
  // fail();
  // }
  // }
}

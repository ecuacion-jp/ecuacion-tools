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
// package jp.ecuacion.util.housekeepfiles.bl;
//
// import java.io.IOException;
// import java.util.Calendar;
// import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
// import jp.ecuacion.lib.core.util.FileUtil;
// import jp.ecuacion.util.housekeepfiles.bl.task.SftpDeleteFromServer;
// import jp.ecuacion.util.housekeepfiles.entity.HousekeepFilesTask;
// import jp.ecuacion.util.housekeepfiles.enums.IncidentTreatedAsEnum;
// import jp.ecuacion.util.housekeepfiles.enums.TaskPtnEnum;
// import jp.ecuacion.util.housekeepfiles.testtool.TestTool;
// import org.junit.jupiter.api.BeforeEach;
//
// public class Test21_313_単体動作確認_taskList_Sftp_サーバから削除 extends TestTool {
// SftpDeleteFromServer task = null;
// HousekeepFilesTask dtE = null;
//
// FileUtil fu = new FileUtil();
//
// @BeforeEach
// public void before() throws MultipleAppException, IOException {
// initTestDir();
//
// task = new SftpDeleteFromServer();
//
// // デフォルト値を設定しておく。必要があればここのテストケースの中で変更する
// dtE = new HousekeepFilesTask();
// dtE.getPk().setTaskId("1");
// dtE.setTaskName("aName");
// dtE.setTaskPtn(TaskPtnEnum.SFTP_DELETE_FROM_SERVER);
// dtE.setUnit(Calendar.DAY_OF_MONTH);
// dtE.setValue(0);
// dtE.setActionForNoSrcPath(IncidentTreatedAsEnum.IGNORE);
// }
//
// @Test
// public void test01_正常系_ファイルを削除() throws IOException, AppExceptionListCarrier {
// dtE.setIsSrcPathDir(false);
//
// //ここは早く変更したいが・・・
// dtE.setRemoteServer("dev.ecuacion.jp");
// HousekeepFilesAuth auth = new HousekeepFilesAuth();
// auth.setRemoteServer(dtE.getRemoteServer());
// auth.setPort(5306);
// auth.setAuthType(AuthTypeEnum.PASSWORD);
// auth.setUserName("tomcat_user");
// auth.setPassword("HtyT#tomc!Aster1s");
// HashMap<String, HousekeepFilesAuth> map = new HashMap<String, HousekeepFilesAuth>();
// map.put(auth.getRemoteServer() + "-SFTP", auth);
//
// new File(fu.concatFilePaths(TEST_HOME_PATH, "toDir")).mkdirs();
// String toFilePath = fu.concatFilePaths(TEST_HOME_PATH, "toDir", "test.jpg");
// try {
// task.doTask(dtE, "/var/www/html/tanpinkatsuyou/background.jpg", toFilePath, map);
// } catch(Exception e) {
// fail();
// }
// }
//
// @Test
// public void testXX_正常系_鍵使用_fromファイル_toフォルダ() throws IOException, AppExceptionListCarrier {
// dtE.setIsSrcPathDir(false);
// dtE.setIsDestPathDir(false);
//
// //ここは早く変更したいが・・・
// dtE.setRemoteServer("resources.ecuacion.jp");
// HousekeepFilesAuth auth = new HousekeepFilesAuth();
// auth.setRemoteServer(dtE.getRemoteServer());
// auth.setPort(5306);
// auth.setAuthType(AuthTypeEnum.KEY);
// auth.setUserName("file_share_user");
// auth.setPassword("HtyT#flsr!Aster1s");
// auth.setKeyPath("C:/Users/yosuk_000/Dropbox/アスタリス/400_開発/81_サーバ関連/resourcesサーバ/
// ssh-key/file_share_user-private-key-for-resouces-ssh-for-openssh.ppk");
// HashMap<String, HousekeepFilesAuth> map = new HashMap<String, HousekeepFilesAuth>();
// map.put(auth.getRemoteServer() + "-SFTP", auth);
//
// new File(fu.concatFilePaths(TEST_HOME_PATH, "toDir")).mkdirs();
// String toFilePath = fu.concatFilePaths(TEST_HOME_PATH, "toDir", "yum-debug.log");
// try {
// task.doTask(dtE, "/var/log/yum-debug.log", toFilePath, map);
// } catch(Exception e) {
// fail();
// }
// }
// }


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
//package jp.ecuacion.util.housekeepfiles.tasklet;
//
//import static org.hamcrest.core.Is.is;
//import static org.junit.Assert.assertThat;
//import static org.junit.Assert.fail;
//import java.io.File;
//import java.io.IOException;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
///**
// * moveの場合のテスト。
// *
// * @author 庸介
// *
// */
//public class Test21_101_単体動作確認_taskList_移動 extends AbstractTestTool {
//
//  @BeforeClass
//  public static void beforeClass() throws IOException {
//    beforeClassCommon();
//  }
//
//  @Before
//  public void before() throws IOException {
//    super.before();
//  }
//
//  @After
//  public void after() throws IOException {
//    super.after();
//  }
//
//  @AfterClass
//  public static void afterClass() throws IOException {
//    afterClassCommon();
//  }
//
//  /**
//   * 基本パターン。from=ファイル、to=ファイル。
//   */
//  @Test
//  public void test01_正常系_fromファイル_toファイル() throws IOException {
//    new File(fu.concatFilePaths(TEST_HOME_PATH, "fromDir")).mkdirs();
//    File fromFile = (new File(fu.concatFilePaths(TEST_HOME_PATH, "fromDir", "test.txt")));
//    fromFile.createNewFile();
//    File toFile = (new File(fu.concatFilePaths(TEST_HOME_PATH, "toDir", "test2.txt")));
//    // ファイルに内容を書き込んでおく
//    writeTestMsgToFile(fromFile);
//
//    // テスト
//    String taskListXml = "0311-normalPtnFileToFile.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(fromFile.exists(), isEqualTo(true);
//      assertThat(toFile.exists()).isEqualTo(false));
//      execute(taskListXml, pathListXml, null, "21", "101", "01");
//      assertThat(fromFile.exists()).isEqualTo(false));
//      assertThat(toFile.exists(), isEqualTo(true);
//      checkFileContentIfTestMsgExists(toFile);
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail();
//    }
//  }
//
//  /**
//   * 基本パターン。from=ファイル、to=ディレクトリ。
//   */
//  @Test
//  public void test02_正常系_fromファイル_toディレクトリ() throws IOException {
//    new File(fu.concatFilePaths(TEST_HOME_PATH, "fromDir")).mkdirs();
//    File fromFile = (new File(fu.concatFilePaths(TEST_HOME_PATH, "fromDir", "test.txt")));
//    fromFile.createNewFile();
//    File toFile = (new File(fu.concatFilePaths(TEST_HOME_PATH, "toDir", "test.txt")));
//    // ファイルに内容を書き込んでおく
//    writeTestMsgToFile(fromFile);
//
//    // テスト
//    String taskListXml = "0311-normalPtnFileToDir.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(fromFile.exists(), isEqualTo(true);
//      assertThat(toFile.exists()).isEqualTo(false));
//      execute(taskListXml, pathListXml, null, "21", "101", "02");
//      assertThat(fromFile.exists()).isEqualTo(false));
//      assertThat(toFile.exists(), isEqualTo(true);
//      checkFileContentIfTestMsgExists(toFile);
//    } catch (Exception e) {
//      fail();
//    }
//  }
//
//  /**
//   * 基本パターン。from=ディレクトリ、to=ディレクトリ。
//   */
//  @Test
//  public void test03_正常系_fromディレクトリ_toディレクトリ() throws IOException {
//    new File(fu.concatFilePaths(TEST_HOME_PATH, "fromDir")).mkdirs();
//    File fromFile = (new File(fu.concatFilePaths(TEST_HOME_PATH, "fromDir")));
//    File toFile = (new File(fu.concatFilePaths(TEST_HOME_PATH, "toDir", "fromDir")));
//
//    // テスト
//    String taskListXml = "0311-normalPtnDirToDir.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(fromFile.exists(), isEqualTo(true);
//      assertThat(toFile.exists()).isEqualTo(false));
//      execute(taskListXml, pathListXml, null, "21", "101", "03");
//      assertThat(fromFile.exists()).isEqualTo(false));
//      assertThat(toFile.exists(), isEqualTo(true);
//    } catch (Exception e) {
//      fail();
//    }
//  }
//}
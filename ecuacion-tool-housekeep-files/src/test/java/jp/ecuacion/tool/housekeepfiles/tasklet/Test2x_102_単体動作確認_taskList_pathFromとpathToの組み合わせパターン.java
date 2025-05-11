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
//import org.apache.commons.io.FileUtils;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
///**
// * FromとToを両方指定するtaskPtnの試験を実施。<br>
// * 複数あるが、最後に行うファイル操作が異なるだけで、その直前までは同一処理を通ってくるので、 代表してcopyでテストを行う
// *
// * @author 庸介
// *
// */
//public class Test2x_102_単体動作確認_taskList_pathFromとpathToの組み合わせパターン extends AbstractTestTool {
//
//  String fromDir = fu.concatFilePaths(TEST_HOME_PATH, "fromDir");
//  String toDir = fu.concatFilePaths(TEST_HOME_PATH, "toDir");
//
//  @BeforeClass
//  public static void beforeClass() throws IOException {
//    beforeClassCommon();
//  }
//
//  @Before
//  public void before() throws IOException {
//    super.before();
//    (new File(fromDir)).mkdirs();
//    (new File(toDir)).mkdirs();
//  }
//
//  @After
//  public void after() throws IOException {
//    FileUtils.deleteDirectory(new File(fromDir));
//    FileUtils.deleteDirectory(new File(toDir));
//  }
//
//  /**
//   * from=1ファイル、to=1ファイル。
//   */
//  @Test
//  public void test01_copyFrom1FileTo1File() throws IOException {
//    File fromFile = (new File(fu.concatFilePaths(fromDir, "test.txt")));
//    fromFile.createNewFile();
//    File toFile = (new File(fu.concatFilePaths(toDir, "test_copied.txt")));
//
//    String taskListXml = "taskPtnFromAndToTest-taskList-oneFileToOneFile.xml";
//    String pathListXml = "common/normalPathList.xml";
//
//    try {
//      assertThat(toFile.exists()).isEqualTo(false));
//      execute(taskListXml, pathListXml, null, "21", "102", "01");
//      assertThat(toFile.exists(), isEqualTo(true);
//    } catch (Exception e) {
//      fail();
//    } finally {
//      fromFile.delete();
//      toFile.delete();
//    }
//  }
//
//  /**
//   * from=1ファイル、to=ディレクトリ。
//   */
//  @Test
//  public void test02_copyFrom1FileToDir() throws IOException {
//    File fromFile = (new File(fu.concatFilePaths(fromDir, "test.txt")));
//    fromFile.createNewFile();
//    File toFile = (new File(fu.concatFilePaths(toDir, "test.txt")));
//
//    String taskListXml = "taskPtnFromAndToTest-taskList-oneFileToDir.xml";
//    String pathListXml = "common/normalPathList.xml";
//
//    try {
//      assertThat(toFile.exists()).isEqualTo(false));
//      execute(taskListXml, pathListXml, null, "21", "102", "02");
//      assertThat(toFile.exists(), isEqualTo(true);
//    } catch (Exception e) {
//      fail();
//    } finally {
//      fromFile.delete();
//      toFile.delete();
//    }
//  }
//
//  /**
//   * from=複数ファイル、to=ディレクトリ。
//   */
//  @Test
//  public void test03_copyFrom2FilesToDir() throws IOException {
//    File fromFile1 = (new File(fu.concatFilePaths(fromDir, "test1.txt")));
//    fromFile1.createNewFile();
//    File fromFile2 = (new File(fu.concatFilePaths(fromDir, "test2.txt")));
//    fromFile2.createNewFile();
//    File fromFile03 = (new File(fu.concatFilePaths(fromDir, "test03.txt")));
//    fromFile03.createNewFile();
//
//    File toFile1 = (new File(fu.concatFilePaths(toDir, "test1.txt")));
//    File toFile2 = (new File(fu.concatFilePaths(toDir, "test2.txt")));
//    File toFile03 = (new File(fu.concatFilePaths(toDir, "test03.txt")));
//
//    String taskListXml = "taskPtnFromAndToTest-taskList-2FilesToDir.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(toFile1.exists()).isEqualTo(false));
//      assertThat(toFile2.exists()).isEqualTo(false));
//      assertThat(toFile03.exists()).isEqualTo(false));
//      execute(taskListXml, pathListXml, null, "21", "102", "03");
//      assertThat(toFile1.exists(), isEqualTo(true);
//      assertThat(toFile2.exists(), isEqualTo(true);
//      assertThat(toFile03.exists()).isEqualTo(false));
//    } catch (Exception e) {
//      fail();
//    }
//  }
//
//  /**
//   * from=ディレクトリ、to=ディレクトリ。
//   */
//  @Test
//  public void test04_copyFromDirToDir() throws IOException {
//    File fromFile1 = (new File(fu.concatFilePaths(fromDir, "test1.txt")));
//    fromFile1.createNewFile();
//    File fromFile2 = (new File(fu.concatFilePaths(fromDir, "test2.txt")));
//    fromFile2.createNewFile();
//
//    File toFile1 = (new File(fu.concatFilePaths(toDir, "fromDir", "test1.txt")));
//    File toFile2 = (new File(fu.concatFilePaths(toDir, "fromDir", "test2.txt")));
//
//    String taskListXml = "taskPtnFromAndToTest-taskList-dirToDir.xml";
//    String pathListXml = "common/normalPathList.xml";
//
//    try {
//      assertThat(toFile1.exists()).isEqualTo(false));
//      assertThat(toFile2.exists()).isEqualTo(false));
//      execute(taskListXml, pathListXml, null, "21", "102", "04");
//      assertThat(toFile1.exists(), isEqualTo(true);
//      assertThat(toFile2.exists(), isEqualTo(true);
//    } catch (Exception e) {
//      fail();
//    }
//  }
//}
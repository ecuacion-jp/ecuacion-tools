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
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
///**
// * Tests for pathFrom.<br>
// * The pathFrom check logic does not depend on taskPtn, so one taskPtn is used for testing.<br>
// * To avoid interference from pathTo logic, delete (which does not look at pathTo) is used.
// */
//public class Test31_01_DetailedVerification_path_pathFromSettings extends AbstractTestTool {
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
//  /**
//   * Pattern where pathFrom specifies a file and the file actually exists.
//   */
//  @Test
//  public void test01_pathFromDeclaringFileActuallyFile() throws IOException {
//    File file = (new File(fu.concatFilePaths(TEST_HOME_PATH, "test.txt")));
//    file.createNewFile();
//
//    String taskListXml = "common/task_pathFromIsFile.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file.exists(), isEqualTo(true);
//      execute(taskListXml, pathListXml, null, "31", "01", "01");
//      assertThat(file.exists()).isEqualTo(false));
//    } catch (Exception e) {
//      fail();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a file but a directory actually exists.
//   * Somewhat ambiguous, but since directories will not appear when wildcards expand files,
//   * and there is a separate setting for "when pathFrom does not exist", this is treated as
//   * ignorable.
//   */
//  @Test
//  public void test02_pathFromDeclaringFileActuallyDir() throws IOException {
//    File file = (new File(fu.concatFilePaths(TEST_HOME_PATH, "test.txt")));
//    file.mkdirs();
//
//    String taskListXml = "common/HK_Test31Common_pathFromTest-taskList-declaringFile.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file.exists(), isEqualTo(true);
//      execute(taskListXml, pathListXml, null, "31", "01", "02");
//    } catch (Exception e) {
//      fail();
//    } finally {
//      file.delete();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a file but the file does not exist.<br>
//   * Since taskList.xml sets "ignore when pathFrom does not exist", no error occurs.<br>
//   * This is acceptable here as pathFrom behavior is tested elsewhere.
//   */
//  @Test
//  public void test03_pathFromDeclaringFileActuallyNotExist() throws IOException {
//    File file = (new File(fu.concatFilePaths(TEST_HOME_PATH, "test.txt")));
//
//    String taskListXml = "common/HK_Test31Common_pathFromTest-taskList-declaringFile.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file.exists()).isEqualTo(false));
//      execute(taskListXml, pathListXml, null, "31", "01", "03");
//    } catch (Exception e) {
//      fail();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a directory but a file actually exists.
//   * Somewhat ambiguous, but treated as ignorable for the same reasons as test02.
//   */
//  @Test
//  public void test11_pathFromDeclaringDirActuallyFile() throws IOException {
//    File file = (new File(fu.concatFilePaths(TEST_HOME_PATH, "test.txt")));
//    file.createNewFile();
//
//    String taskListXml = "common/HK_Test31Common_pathFromTest-taskList-declaringDir.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file.exists(), isEqualTo(true);
//      execute(taskListXml, pathListXml, null, "31", "01", "11");
//    } catch (Exception e) {
//      fail();
//    } finally {
//      file.delete();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a directory and a directory actually exists.
//   */
//  @Test
//  public void test12_pathFromDeclaringDirActuallyDir() throws IOException {
//    File file = (new File(fu.concatFilePaths(TEST_HOME_PATH, "test.txt")));
//    file.mkdirs();
//
//    String taskListXml = "common/HK_Test31Common_pathFromTest-taskList-declaringDir.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file.exists(), isEqualTo(true);
//      execute(taskListXml, pathListXml, null, "31", "01", "12");
//      assertThat(file.exists()).isEqualTo(false));
//    } catch (Exception e) {
//      fail();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a directory but the path does not exist.<br>
//   * Since taskList.xml sets "ignore when pathFrom does not exist", no error occurs.<br>
//   * This is acceptable here as pathFrom behavior is tested elsewhere.
//   */
//  @Test
//  public void test13_pathFromDeclaringDirActuallyNotExist() throws IOException {
//    File file = (new File(fu.concatFilePaths(TEST_HOME_PATH, "test.txt")));
//
//    String taskListXml = "common/HK_Test31Common_pathFromTest-taskList-declaringDir.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file.exists()).isEqualTo(false));
//      execute(taskListXml, pathListXml, null, "31", "01", "13");
//    } catch (Exception e) {
//      fail();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a file and the file exists (with wildcard).<br>
//   */
//  @Test
//  public void test21_pathFromDeclaringFileActuallyFileWithWildCard() throws IOException {
//    File file1 = (new File(fu.concatFilePaths(TEST_HOME_PATH, "test.txt")));
//    file1.createNewFile();
//    File file2 = (new File(fu.concatFilePaths(TEST_HOME_PATH, "tesu.txt")));
//    file2.createNewFile();
//
//    String taskListXml =
//        "common/HK_Test31Common_pathFromTest-taskList-declaringFileWithWildCard.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file1.exists(), isEqualTo(true);
//      assertThat(file2.exists(), isEqualTo(true);
//      execute(taskListXml, pathListXml, null, "31", "01", "21");
//      assertThat(file1.exists()).isEqualTo(false));
//      assertThat(file2.exists()).isEqualTo(false));
//    } catch (Exception e) {
//      fail();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a file but a directory exists (with wildcard).<br>
//   * Treated as ignorable for the same reasons as test02.
//   */
//  @Test
//  public void test22_pathFromDeclaringFileActuallyDirWithWildCard() throws IOException {
//    File file = (new File(fu.concatFilePaths(TEST_HOME_PATH, "test.txt")));
//    file.mkdirs();
//
//    String taskListXml =
//        "common/HK_Test31Common_pathFromTest-taskList-declaringFileWithWildCard.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file.exists(), isEqualTo(true);
//      execute(taskListXml, pathListXml, null, "31", "01", "22");
//    } catch (Exception e) {
//      fail();
//    } finally {
//      file.delete();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a file and both files and directories exist (with wildcard).
//   */
//  @Test
//  public void test23_pathFromDeclaringFileActuallyMixedWithWildCard() throws IOException {
//    File file = (new File(fu.concatFilePaths(TEST_HOME_PATH, "tesF.txt")));
//    file.createNewFile();
//    File dir = (new File(fu.concatFilePaths(TEST_HOME_PATH, "tesD.txt")));
//    dir.mkdirs();
//
//    String taskListXml =
//        "common/HK_Test31Common_pathFromTest-taskList-declaringFileWithWildCard.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file.exists(), isEqualTo(true);
//      assertThat(dir.exists(), isEqualTo(true);
//      execute(taskListXml, pathListXml, null, "31", "01", "23");
//      assertThat(file.exists()).isEqualTo(false));
//      assertThat(dir.exists(), isEqualTo(true);
//    } catch (Exception e) {
//      fail();
//    } finally {
//      dir.delete();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a file but nothing exists (with wildcard).<br>
//   * Since taskList.xml sets "ignore when pathFrom does not exist", no error occurs.<br>
//   * This is acceptable here as pathFrom behavior is tested elsewhere.
//   */
//  @Test
//  public void test24_pathFromDeclaringFileActuallyNotExistWithWildCard() throws IOException {
//
//    String taskListXml =
//        "common/HK_Test31Common_pathFromTest-taskList-declaringFileWithWildCard.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      execute(taskListXml, pathListXml, null, "31", "01", "24");
//    } catch (Exception e) {
//      fail();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a directory but files actually exist (with wildcard).<br>
//   * Treated as ignorable for the same reasons as test11.
//   */
//  @Test
//  public void test31_pathFromDeclaringDirActuallyFileWithWildCard() throws IOException {
//    File file1 = (new File(fu.concatFilePaths(TEST_HOME_PATH, "tes1.txt")));
//    file1.createNewFile();
//    File file2 = (new File(fu.concatFilePaths(TEST_HOME_PATH, "tes2.txt")));
//    file2.createNewFile();
//
//    String taskListXml =
//        "common/HK_Test31Common_pathFromTest-taskList-declaringDirWithWildCard.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file1.exists(), isEqualTo(true);
//      assertThat(file2.exists(), isEqualTo(true);
//      execute(taskListXml, pathListXml, null, "31", "01", "31");
//      assertThat(file1.exists(), isEqualTo(true);
//      assertThat(file2.exists(), isEqualTo(true);
//    } catch (Exception e) {
//      fail();
//    } finally {
//      file1.delete();
//      file2.delete();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a directory and directories exist (with wildcard).
//   */
//  @Test
//  public void test32_pathFromDeclaringDirActuallyDirWithWildCard() throws IOException {
//    File file1 = (new File(fu.concatFilePaths(TEST_HOME_PATH, "tes1.txt")));
//    file1.mkdirs();
//    File file2 = (new File(fu.concatFilePaths(TEST_HOME_PATH, "tes2.txt")));
//    file2.mkdirs();
//
//    String taskListXml =
//        "common/HK_Test31Common_pathFromTest-taskList-declaringDirWithWildCard.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file1.exists(), isEqualTo(true);
//      assertThat(file2.exists(), isEqualTo(true);
//      execute(taskListXml, pathListXml, null, "31", "01", "32");
//      assertThat(file1.exists()).isEqualTo(false));
//      assertThat(file2.exists()).isEqualTo(false));
//    } catch (Exception e) {
//      fail();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a directory and mixed content exists (with wildcard).
//   */
//  @Test
//  public void test33_pathFromDeclaringDirActuallyMixedWithWildCard() throws IOException {
//    File file = (new File(fu.concatFilePaths(TEST_HOME_PATH, "tesF.txt")));
//    file.createNewFile();
//    File dir = (new File(fu.concatFilePaths(TEST_HOME_PATH, "tesD.txt")));
//    dir.mkdirs();
//
//    String taskListXml =
//        "common/HK_Test31Common_pathFromTest-taskList-declaringDirWithWildCard.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      assertThat(file.exists(), isEqualTo(true);
//      assertThat(dir.exists(), isEqualTo(true);
//      execute(taskListXml, pathListXml, null, "31", "01", "33");
//      assertThat(file.exists(), isEqualTo(true);
//      assertThat(dir.exists()).isEqualTo(false));
//    } catch (Exception e) {
//      fail();
//    } finally {
//      file.delete();
//    }
//  }
//
//  /**
//   * Pattern where pathFrom specifies a directory but nothing exists (with wildcard).<br>
//   * Since taskList.xml sets "ignore when pathFrom does not exist", no error occurs.<br>
//   * This is acceptable here as pathFrom behavior is tested elsewhere.
//   */
//  @Test
//  public void test34_pathFromDeclaringDirActuallyNotExistWithWildCard() throws IOException {
//    String taskListXml =
//        "common/HK_Test31Common_pathFromTest-taskList-declaringDirWithWildCard.xml";
//    String pathListXml = "common/normalPathList.xml";
//    try {
//      execute(taskListXml, pathListXml, null, "31", "01", "34");
//    } catch (Exception e) {
//      fail();
//    }
//  }
//}


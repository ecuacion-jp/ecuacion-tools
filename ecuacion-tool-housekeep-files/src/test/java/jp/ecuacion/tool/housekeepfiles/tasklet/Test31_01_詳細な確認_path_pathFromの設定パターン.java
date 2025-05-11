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
// * pathFromのテスト。<br>
// * pathFromのチェック処理は、taskPtnに依存しない。そのためひとつのtaskPtn似てテストを行う。<br>
// * pathFromのテストの中にpathToが入ってくると面倒なので、pathToを見ないdeleteを使ってテストを行う。
// *
// * @author 庸介
// *
// */
//public class Test31_01_詳細な確認_path_pathFromの設定パターン extends AbstractTestTool {
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
//   * pathFromがファイル指定でファイルが存在するパターン。
//   */
//  @Test
//  public void test01_pathFromがファイルと宣言し実際にファイルだった場合() throws IOException {
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
//   * pathFromがファイル指定でディレクトリが存在するパターン。 若干微妙だが、ワイルドカードが入る場合にどっちに倒すべきかますます悩む。
//   * 結局、ファイルだと宣言しているならディレクトリはワイルドカードを展開しても入ってこず、 かつ、別途「pathFromが存在しない場合」の挙動についても定義できるようにしているので、
//   * これは無視してよいこととする。
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
//   * pathFromのファイル指定が存在しないパターン。<br>
//   * taskList.xml上、pathFromが存在しない場合は無視になっているのでエラーは発生せず素通り。<br>
//   * ここではpathFromのテストなのでこれでよいものとし、pathFromが存在しない場合の挙動は別のテストで行う。
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
//   * pathFromがディレクトリ指定でファイルが存在するパターン。 若干微妙だが、ワイルドカードが入る場合にどっちに倒すべきかますます悩むのと、
//   * 別途「pathFromが存在しない場合」の挙動についても定義できるようにしているので、 これは無視してよいこととする。
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
//   * pathFromがディレクトリ指定でディレクトリが存在するパターン。
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
//   * pathFromのファイル指定が存在しないパターン。<br>
//   * taskList.xml上、pathFromが存在しない場合は無視になっているのでエラーは発生せず素通り。<br>
//   * ここではpathFromのテストなのでこれでよいものとし、pathFromが存在しない場合の挙動は別のテストで行う。
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
//   * pathFromがファイル指定でファイルが存在する場合（ワイルドカード付）。<br>
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
//   * pathFromがファイル指定でディレクトリが存在するパターン。（ワイルドカード付）<br>
//   * 若干微妙だが、ワイルドカードが入る場合にどっちに倒すべきかますます悩むのと、 別途「pathFromが存在しない場合」の挙動についても定義できるようにしているので、
//   * これは無視してよいこととする。
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
//   * pathFromのファイル指定で、ワイルドカード付でファイル・ディレクトリとも存在するパターン。<br>
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
//   * pathFromのファイル指定が存在しないパターン。（ワイルドカード付）<br>
//   * taskList.xml上、pathFromが存在しない場合は無視になっているのでエラーは発生せず素通り。<br>
//   * ここではpathFromのテストなのでこれでよいものとし、pathFromが存在しない場合の挙動は別のテストで行う。
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
//   * pathFromがディレクトリ指定でファイルが存在するパターン。（ワイルドカード付）<br>
//   * 若干微妙だが、ワイルドカードが入る場合にどっちに倒すべきかますます悩むのと、 別途「pathFromが存在しない場合」の挙動についても定義できるようにしているので、
//   * これは無視してよいこととする。
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
//   * pathFromがディレクトリ指定でディレクトリが存在するパターン。（ワイルドカード付）。<br>
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
//   * pathFromがディレクトリ指定で混在しているパターン。（ワイルドカード付）。<br>
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
//   * pathFromのディレクトリ指定が存在しないパターン。（ワイルドカード付）<br>
//   * taskList.xml上、pathFromが存在しない場合は無視になっているのでエラーは発生せず素通り。<br>
//   * ここではpathFromのテストなのでこれでよいものとし、pathFromが存在しない場合の挙動は別のテストで行う。
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


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
//import java.io.IOException;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//public class Test11_101_xmlデータの値検証_taskList_チェック_移動 extends AbstractTestTool {
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
//   * taskListの各項目が空欄の場合にエラーになることの確認。 別途テストするが、Actionから見てちゃんと動いているかの抜粋テスト。
//   */
//  @Test
//  public void test01_taskListの中身が空欄() {
//
//  }
//
//
//  // <isSrcPathDir>FALSE</isSrcPathDir>
//  // <pathFrom>${TEST_HOME}/test-test-test.test</pathFrom>
//  // <isDestPathDir>TRUE</isDestPathDir>
//  // <pathTo>${TEST_HOME}/tmp</pathTo>
//  // <unit>DAY</unit>
//  // <value>1000</value>
//  // <actionForNoSrcPath>IGNORE</actionForNoSrcPath>
//  // <doesOverwriteDestPath>FALSE</doesOverwriteDestPath>
//  // <actionForFileToExists>IGNORE</actionForFileToExists>
//
//  // } catch (Exception e) {
//  // ArrayList<Throwable> arr = eu.serializeAndRemoveExceptionWithoutMessage(e);
//  // assertThat(arr.size()).isEqualTo(1));
//  // assertThat(arr.get(0)).isEqualTo(instanceOf(BizLogicAppException.class)));
//  // BizLogicAppException ae = (BizLogicAppException)arr.get(0);
//  // assertThat(ae.getMessageId()).isEqualTo("MSG_ERR_REQUIRED_CHECK")));
//  // assertThat(ae.getMessageArgs()).isEqualTo(null)));
//  // assertThat(ae.getAttrInfoArr().length).isEqualTo(1));
//  // assertThat(ae.getAttrInfoArr()[0].getId()).isEqualTo("taskId")));
//
//}
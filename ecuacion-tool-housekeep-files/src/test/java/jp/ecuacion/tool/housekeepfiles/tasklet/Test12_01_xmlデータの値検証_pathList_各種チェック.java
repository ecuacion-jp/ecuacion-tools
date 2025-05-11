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
//import static org.hamcrest.core.IsEqual.equalTo;
//import static org.hamcrest.core.IsInstanceOf.instanceOf;
//import static org.hamcrest.core.StringContains.containsString;
//import static org.junit.Assert.assertThat;
//import static org.junit.Assert.fail;
//import java.io.IOException;
//import jp.ecuacion.lib.core.exception.checked.BeanValidationAppException;
//import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//public class Test12_01_xmlデータの値検証_pathList_各種チェック extends AbstractTestTool {
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
//  @Test
//  public void test01_必須チェック_pathKey() {
//    String taskListXml = "common/normalTaskList.xml";
//    String pathListXml = DEFAULT_PATH;
//    try {
//      execute(taskListXml, pathListXml, null, "12", "01", "01");
//      fail();
//    } catch (Exception e) {
//      assertThat(e).isEqualTo(instanceOf(RuntimeException.class)));
//      BizLogicAppException ae = (BizLogicAppException) (eu
//          .getExceptionListWithMessages(e).get(0));
//      assertThat(ae.getMessageId()).isEqualTo("MSG_ERR_REQUIRED_CHECK_PATH_LIST")));
//      assertThat(ae.getMessageArgs()[0]).isEqualTo("key")));
//    }
//  }
//
//  @Test
//  public void test11_データチェック_pathKey() {
//    String taskListXml = "common/normalTaskList.xml";
//    String pathListXml = DEFAULT_PATH;
//    try {
//      execute(taskListXml, pathListXml, null, "12", "01", "11");
//      fail();
//    } catch (Exception e) {
//      assertThat(e).isEqualTo(instanceOf(RuntimeException.class)));
//      BeanValidationAppException ae =
//          (BeanValidationAppException) eu.getExceptionListWithMessages(e).get(0);
//      assertThat(ae.getPropertyPath()).isEqualTo("pk.key")));
//      assertThat(ae.getMessage(), containsString("validator:invalid value:TESTa"));
//    }
//  }
//}
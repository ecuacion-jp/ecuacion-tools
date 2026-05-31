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

import jakarta.validation.ConstraintViolation;
import java.io.IOException;
import java.util.ArrayList;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.DoNothingInConstructorForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Test11_031_XmlDataValidation_taskList_Check_Common extends TestTool {

  @SuppressWarnings("null")
  DoNothingInConstructorForm form = null;

  @BeforeAll
  public static void beforeClass() throws IOException {}

  @BeforeEach
  public void before() throws IOException {

    try {
      form = new DoNothingInConstructorForm();
      form.getTaskInfoHdRec().recList = new ArrayList<>();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("null")
  @Test
  public void test01_systemNameIsNull() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "AAA", null,
        "aPath", "TRUE", "DAY", "7", "IGNORE", "aPath", "TRUE", "FALSE", "IGNORE", null);
    form.getTaskInfoHdRec().recList.add(rec);

    try {
      new HousekeepFilesBlf().execute(form);
      fail();
    } catch (ViolationException e) {
      var cvList = e.getViolations().getConstraintViolations();
      assertEquals(1, cvList.size());
      ConstraintViolation<?> cv = cvList.get(0);
      Assertions.assertEquals("sysName", cv.getPropertyPath().toString());
      Assertions.assertEquals("jakarta.validation.constraints.NotEmpty",
          cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
    }
  }

  @SuppressWarnings("null")
  @Test
  public void test02_systemNameIsEmpty() throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "AAA", null,
        "aPath", "TRUE", "DAY", "7", "IGNORE", "aPath", "TRUE", "FALSE", "IGNORE", null);
    form.getTaskInfoHdRec().recList.add(rec);
    form.getTaskInfoHdRec().setSysName("");

    try {
      new HousekeepFilesBlf().execute(form);
      fail();
    } catch (ViolationException e) {
      var cvList = e.getViolations().getConstraintViolations();
      assertEquals(2, cvList.size());
      ConstraintViolation<?> cv1 = cvList.get(0);
      ConstraintViolation<?> cv2 = cvList.get(1);
      assertEquals("sysName", cv1.getPropertyPath().toString());
      assertEquals("sysName", cv2.getPropertyPath().toString());
    }
  }

  @Test
  public void test03_systemNameIsValid() {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE",
        null, "aPath", "TRUE", "DAY", "7", "IGNORE", "aPath", "TRUE", "FALSE", "IGNORE", null);
    form.getTaskInfoHdRec().recList.add(rec);
    form.getTaskInfoHdRec().setSysName("test-system");

    try {
      new HousekeepFilesBlf().execute(form);

    } catch (Exception ex) {
      ex.printStackTrace();
      fail();
    }
  }

  // @Test
  // public void test11_behaviorWhenValueIsEmpty_taskId() {
  // HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord(null, "aTaskName", "MOVE", null,
  // "TRUE", "aPath", "TRUE", "aPath", "DAY", "7", "IGNORE", "FALSE", "IGNORE");
  // form.taskInfoHdRec.recList.add(rec);
  // form.taskInfoHdRec.setSysName("aSystem");
  //
  // try {
  // new HousekeepFilesBlf().execute(form);
  // fail();
  // } catch (Exception e) {
  // List<Throwable> thList = exUtil.getExceptionListWithMessages((AppException) e);
  // assertThat(thList.size()).isEqualTo(1));
  // BizLogicAppException ace = (BizLogicAppException) thList.get(0);
  // assertThat(ace.getMessageId()).isEqualTo("MSG_ERR_TASK_REQUIRED_CHECK")));
  // assertThat(ace.getMessageArgs().length).isEqualTo(3));
  // assertThat(ace.getMessageArgs()[0]).isEqualTo("")));
  // assertThat(ace.getMessageArgs()[1]).isEqualTo("")));
  // assertThat(ace.getMessageArgs()[2]).isEqualTo("taskId")));
  // }
  // }
  //
  // @Test
  // public void test12_behaviorWhenValueIsEmpty_taskName() {
  // HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", null, "MOVE", null,
  // "TRUE", "aPath", "TRUE", "aPath", "DAY", "7", "IGNORE", "FALSE", "IGNORE");
  // form.taskInfoHdRec.recList.add(rec);
  // form.taskInfoHdRec.setSysName("aSystem");
  // try {
  // new HousekeepFilesBlf().execute(form);
  // fail();
  // } catch (Exception e) {
  // List<Throwable> thList = exUtil.getExceptionListWithMessages((AppException) e);
  // assertThat(thList.size()).isEqualTo(1));
  // BizLogicAppException ace = (BizLogicAppException) thList.get(0);
  // assertThat(ace.getMessageId()).isEqualTo("MSG_ERR_TASK_REQUIRED_CHECK")));
  // assertThat(ace.getMessageArgs().length).isEqualTo(3));
  // assertThat(ace.getMessageArgs()[0]).isEqualTo("aTaskId")));
  // assertThat(ace.getMessageArgs()[1]).isEqualTo("")));
  // assertThat(ace.getMessageArgs()[2]).isEqualTo("taskName")));
  // }
  // }
  //
  // @Test
  // public void test13_behaviorWhenValueIsEmpty_taskPtn() {
  // HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", null, null,
  // "TRUE", "aPath", "TRUE", "aPath", "DAY", "7", "IGNORE", "FALSE", "IGNORE");
  // form.taskInfoHdRec.recList.add(rec);
  // form.taskInfoHdRec.setSysName("aSystem");
  //
  // try {
  // new HousekeepFilesBlf().execute(form);
  // fail();
  // } catch (Exception e) {
  // List<Throwable> thList = exUtil.getExceptionListWithMessages((AppException) e);
  // assertThat(thList.size()).isEqualTo(1));
  // BizLogicAppException ace = (BizLogicAppException) thList.get(0);
  // assertThat(ace.getMessageId()).isEqualTo("MSG_ERR_TASK_REQUIRED_CHECK")));
  // assertThat(ace.getMessageArgs().length).isEqualTo(3));
  // assertThat(ace.getMessageArgs()[0]).isEqualTo("aTaskId")));
  // assertThat(ace.getMessageArgs()[1]).isEqualTo("")));
  // assertThat(ace.getMessageArgs()[2]).isEqualTo("taskPtn")));
  // }
  // }
}

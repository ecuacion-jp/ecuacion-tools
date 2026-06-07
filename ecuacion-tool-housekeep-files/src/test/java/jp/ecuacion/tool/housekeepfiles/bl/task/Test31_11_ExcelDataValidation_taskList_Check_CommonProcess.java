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
import java.util.ArrayList;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.enums.TaskPtnEnum;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for HousekeepFilesXmlDataChecker#checkTaskItem.
 */
public class Test31_11_ExcelDataValidation_taskList_Check_CommonProcess extends TestTool {

  @BeforeAll
  public static void beforeClass() throws IOException {
  }

  @BeforeEach
  public void before() throws IOException {
  }

  @SuppressWarnings({"NullAway", "null"})
  @Test
  public void test01_checkTaskItem_required_null() {
    Violations violations = new Violations();
    new Move().checkTaskItem(violations, "aTaskId", TaskPtnEnum.MOVE,
        TaskAttrCheckPtnEnum.REQUIRED, "unit", null);
    ArrayList<BusinessViolation> bvList =
        new ArrayList<>(violations.getBusinessViolations());
    assertEquals(1, bvList.size());
    assertEquals("MSG_ERR_TASK_REQUIRED_CHECK", bvList.get(0).getMessageId());
  }

  @SuppressWarnings("null")
  @Test
  public void test02_checkTaskItem_required_emptyString() {
    Violations violations = new Violations();
    new Move().checkTaskItem(violations, "aTaskId", TaskPtnEnum.MOVE,
        TaskAttrCheckPtnEnum.REQUIRED, "unit", "");
    ArrayList<BusinessViolation> bvList =
        new ArrayList<>(violations.getBusinessViolations());
    assertEquals(1, bvList.size());
    assertEquals("MSG_ERR_TASK_REQUIRED_CHECK", bvList.get(0).getMessageId());
  }

  @Test
  public void test03_checkTaskItem_required_normalString() {
    Violations violations = new Violations();
    new Move().checkTaskItem(violations, "aTaskId", TaskPtnEnum.MOVE,
        TaskAttrCheckPtnEnum.REQUIRED, "anItem", "aValue");
    assertTrue(violations.getBusinessViolations().isEmpty());
  }

  @SuppressWarnings("NullAway")
  @Test
  public void test11_checkTaskItem_prohibited_null() {
    Violations violations = new Violations();
    new Move().checkTaskItem(violations, "aTaskId", TaskPtnEnum.MOVE,
        TaskAttrCheckPtnEnum.PROHIBITED, "anItem", null);
    assertTrue(violations.getBusinessViolations().isEmpty());
  }

  @Test
  public void test12_checkTaskItem_prohibited_emptyString() {
    Violations violations = new Violations();
    new Move().checkTaskItem(violations, "aTaskId", TaskPtnEnum.MOVE,
        TaskAttrCheckPtnEnum.PROHIBITED, "anItem", "");
    assertTrue(violations.getBusinessViolations().isEmpty());
  }

  @SuppressWarnings("null")
  @Test
  public void test13_checkTaskItem_prohibited_normalString() {
    Violations violations = new Violations();
    new Move().checkTaskItem(violations, "aTaskId", TaskPtnEnum.MOVE,
        TaskAttrCheckPtnEnum.PROHIBITED, "unit", "aValue");
    ArrayList<BusinessViolation> bvList =
        new ArrayList<>(violations.getBusinessViolations());
    assertEquals(1, bvList.size());
    assertEquals("MSG_ERR_TASK_PROHIBITED_CHECK", bvList.get(0).getMessageId());
  }

  @SuppressWarnings("NullAway")
  @Test
  public void test21_checkTaskItem_arbitrary_null() {
    Violations violations = new Violations();
    new Move().checkTaskItem(violations, "aTaskId", TaskPtnEnum.MOVE,
        TaskAttrCheckPtnEnum.ARBITRARY, "anItem", null);
    assertTrue(violations.getBusinessViolations().isEmpty());
  }

  @Test
  public void test22_checkTaskItem_arbitrary_emptyString() {
    Violations violations = new Violations();
    new Move().checkTaskItem(violations, "aTaskId", TaskPtnEnum.MOVE,
        TaskAttrCheckPtnEnum.ARBITRARY, "anItem", "");
    assertTrue(violations.getBusinessViolations().isEmpty());
  }

  @Test
  public void test23_checkTaskItem_arbitrary_normalString() {
    Violations violations = new Violations();
    new Move().checkTaskItem(violations, "aTaskId", TaskPtnEnum.MOVE,
        TaskAttrCheckPtnEnum.ARBITRARY, "anItem", "aValue");
    assertTrue(violations.getBusinessViolations().isEmpty());
  }
}

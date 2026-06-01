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
package jp.ecuacion.tool.housekeepfiles.reader;

import jakarta.validation.ConstraintViolation;
import java.util.ArrayList;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.junit.jupiter.api.Test;

@SuppressWarnings("null")
public class Test11_14_ExcelDataValidation_taskList_ItemCheck_OtherItems extends TestTool {

  // test01_remoteServer_invalid_tooLong
  // test11_srcPath_invalid_tooLong

  @Test
  public void test21_srcPathIsDir_invalid_unsupportedString()
      throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE",
        null, "aPath", "はい", "DAY", "7", "IGNORE", "aPath", "TRUE", "FALSE", "IGNORE", null);

    try {
      new Violations().validate(rec).throwIfAny();
      fail();

    } catch (ViolationException ex) {
      ConstraintViolation<?> cv =
          new ArrayList<>(ex.getViolations().getConstraintViolations()).get(0);
      assertEquals("jp.ecuacion.lib.validation.constraints.BooleanString",
          cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("isSrcPathDirEnumName", cv.getPropertyPath().toString());
    }
  }

  // test31_srcPathPeriodUnit_invalid_unexpectedString
  // test41_srcPathPeriodValue_invalid_decimalValue
  // test42_srcPathPeriodValue_invalid_commaIncludedNumber
  // test43_srcPathPeriodValue_invalid_notANumber

  // test61_destPath_invalid_tooLong
  // test62_destPath_invalid_wildcard_asterisk
  // test63_destPath_invalid_wildcard_questionMark
  // test71_destPathIsDir_invalid_unsupportedString
  // test81_overwriteDestPath_invalid_unsupportedString
  // test91_whenDestPathExists_invalid_unsupportedString

  @Test
  public void test51_actionForNoSrcPath_invalid_unsupportedString()
      throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE",
        null, "aPath", "TRUE", "DAY", "7", "無視", "aPath", "TRUE", "FALSE", "IGNORE", null);

    try {
      new Violations().validate(rec).throwIfAny();
      fail();

    } catch (ViolationException ex) {
      ConstraintViolation<?> cv =
          new ArrayList<>(ex.getViolations().getConstraintViolations()).get(0);
      assertEquals("jp.ecuacion.lib.validation.constraints.EnumElement",
          cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("actionForNoSrcPathEnumName", cv.getPropertyPath().toString());
    }
  }

}

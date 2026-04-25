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

import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.ArrayList;
import jakarta.validation.ConstraintViolation;
import jp.ecuacion.lib.core.util.ValidationUtil;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.TestTool;
import org.apache.poi.EncryptedDocumentException;
import org.junit.jupiter.api.Test;

public class Test11_14_excelデータの値検証_taskList_単体項目チェック_その他項目 extends TestTool {

  // test01_接続先サーバ_異常系_長さ上限超過
  // test11_元パス_異常系_長さ上限超過

  @Test
  public void test21_元パスがディレクトリ_異常系_指定外文字列()
      throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE",
        null, "aPath", "はい", "DAY", "7", "IGNORE", "aPath", "TRUE", "FALSE", "IGNORE", null);

    try {
      ValidationUtil.validateThenThrow(rec);
      fail();

    } catch (ConstraintViolationException ex) {
      ConstraintViolation<?> cv = new ArrayList<>(ex.getConstraintViolations()).get(0);
      assertEquals("jp.ecuacion.lib.validation.constraints.BooleanString",
          cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("isSrcPathDirEnumName", cv.getPropertyPath().toString());
    }
  }

  // test31_元パス処理実施対象経過期間単位＿想定外文字列
  // test41_元パス処理実施対象経過期間値_異常系_小数値
  // test42_元パス処理実施対象経過期間値_異常系_カンマ入り数値
  // test43_元パス処理実施対象経過期間値_異常系_数値でない

  // test61_先パス_異常系_長さ上限超過
  // test62_先パス_異常系_ワイルドカード_asterisk
  // test63_先パス_異常系_ワイルドカード_question_mark
  // test71_先パスがディレクトリ_異常系_指定外文字列
  // test81_先パス存在時上書き_異常系_指定外文字列
  // test91_先パス存在時処理_異常系_指定外文字列

  @Test
  public void test51_元パス存在なし時処理_異常系_指定外文字列()
      throws Exception {
    HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "MOVE",
        null, "aPath", "TRUE", "DAY", "7", "無視", "aPath", "TRUE", "FALSE", "IGNORE", null);

    try {
      ValidationUtil.validateThenThrow(rec);
      fail();

    } catch (ConstraintViolationException ex) {
      ConstraintViolation<?> cv = new ArrayList<>(ex.getConstraintViolations()).get(0);
      assertEquals("jp.ecuacion.lib.validation.constraints.EnumElement",
          cv.getConstraintDescriptor().getAnnotation().annotationType().getName());
      assertEquals("actionForNoSrcPathEnumName", cv.getPropertyPath().toString());
    }
  }

}

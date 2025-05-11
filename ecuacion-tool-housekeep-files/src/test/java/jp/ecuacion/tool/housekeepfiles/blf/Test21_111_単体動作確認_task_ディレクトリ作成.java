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
//package jp.ecuacion.util.housekeepfiles.blf;
//
//import java.io.File;
//import java.util.Arrays;
//import jp.ecuacion.util.housekeepfiles.dto.form.HousekeepFilesForm;
//import jp.ecuacion.util.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
//import org.junit.jupiter.api.Test;
//
//public class Test21_111_単体動作確認_task_ディレクトリ作成 extends BlfTestTool {
//
//    @Test
//    public void test01_正常系_1階層() {
//      HousekeepFilesForm form = getDefaultForm();
//
//      String testDirPath = getCurDirPath() + "/" + TEST_HOME_PATH + "/testdir";
//      // task
//      HousekeepFilesTaskRecord task = new HousekeepFilesTaskRecord("01", "task01", "CREATE_DIR",
//          "", "FALSE", testDirPath, "", "", "DAY", "0", "ERROR", "TRUE", "IGNORE");
//      form.taskInfoHdRec.recList = Arrays.asList(new HousekeepFilesTaskRecord[] {task});
//
//      File testDir = new File(testDirPath);
//      try {
//        assertEquals(false, testDir.exists());
//
//        new HousekeepFilesBlf().execute(form);
//
//        assertEquals(true, testDir.exists());
//
//      } catch (Exception ex) {
//        ex.printStackTrace();
//        fail();
//      }
//
//    }
//    
////  test02_正常系_複数階層
////  test11_異常系_ファイル存在
////  test12_異常系_ディレクトリ存在
//}
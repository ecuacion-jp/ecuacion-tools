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
package jp.ecuacion.tool.housekeepfiles.blf;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import org.junit.jupiter.api.Test;

public class Test21_201_単体動作確認_taskList_Zip_元を削除 extends BlfTestTool {

  @Test
  public void test01_zip1File() throws IOException {
    HousekeepFilesForm form = getDefaultForm();

    String testFilePath = getCurDirPath() + "/" + TEST_HOME_PATH + "/test.txt";
    // task
    HousekeepFilesTaskRecord task = new HousekeepFilesTaskRecord("01", "task01", "ZIP_DELETE_ORIG",
        "", testFilePath, "FALSE", "DAY", "0", "ERROR", "", "", "TRUE", "IGNORE", null);
    form.getTaskInfoHdRec().recList = Arrays.asList(new HousekeepFilesTaskRecord[] {task});

    //
    File fromFile = new File(testFilePath);
    fromFile.createNewFile();
    File toFile = new File(fromFile.getAbsolutePath() + ".zip");

    try {
      assertEquals(false, toFile.exists());

      new HousekeepFilesBlf().execute(form);

      assertEquals(true, toFile.exists());
      assertEquals(true, !fromFile.exists());

    } catch (Exception ex) {
      ex.printStackTrace();
      fail();
    }
  }

  @Test
  public void test02_zip2Files() throws IOException {
    HousekeepFilesForm form = getDefaultForm();

    String test1FilePath = getCurDirPath() + "/" + TEST_HOME_PATH + "/test1.txt";
    String test2FilePath = getCurDirPath() + "/" + TEST_HOME_PATH + "/test2.txt";
    String testFilesPath = getCurDirPath() + "/" + TEST_HOME_PATH + "/test*.txt";

    // task
    HousekeepFilesTaskRecord task = new HousekeepFilesTaskRecord("01", "task01", "ZIP_DELETE_ORIG",
        "", testFilesPath, "FALSE", "DAY", "0", "ERROR", "", "", "TRUE", "IGNORE", null);
    form.getTaskInfoHdRec().recList = Arrays.asList(new HousekeepFilesTaskRecord[] {task});

    //
    File fromFile1 = new File(test1FilePath);
    File fromFile2 = new File(test2FilePath);
    fromFile1.createNewFile();
    fromFile2.createNewFile();
    File toFile1 = new File(fromFile1.getAbsolutePath() + ".zip");
    File toFile2 = new File(fromFile2.getAbsolutePath() + ".zip");

    try {
      assertEquals(false, toFile1.exists());
      assertEquals(false, toFile2.exists());

      new HousekeepFilesBlf().execute(form);
      assertEquals(true, toFile1.exists());
      assertEquals(true, toFile2.exists());
      assertEquals(true, !fromFile1.exists());
      assertEquals(true, !fromFile2.exists());

    } catch (Exception ex) {
      fail();
    }
  }
}

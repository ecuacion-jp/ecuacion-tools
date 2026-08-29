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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.constraints.NotEmpty;
import java.io.File;
import java.nio.file.Path;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link HousekeepFilesBlf}. */
@DisplayName("HousekeepFilesBlf")
class HousekeepFilesBlfTest {

  private HousekeepFilesForm form(@Nullable String sysName, HousekeepFilesTaskRecord taskRec) {
    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);

    if (sysName != null) {
      form.getTaskInfoHdRec().setSysName(sysName);
    }

    return form;
  }

  @Nested
  @DisplayName("execute(): sysName validation")
  class ExecuteSysNameValidation {

    private HousekeepFilesTaskRecord aTaskRecord(String taskPtn) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", taskPtn, null, "aPath", "TRUE",
          "DAY", "7", "IGNORE", "aPath", "TRUE", "FALSE", "IGNORE", null);
    }

    @Test
    @DisplayName("null sysName violates @NotEmpty")
    void sysNameIsNull() {
      HousekeepFilesForm form = form(null, aTaskRecord("AAA"));

      assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getConstraintViolations()).singleElement()
                  .satisfies(cv -> {
                    assertThat(cv.getPropertyPath().toString()).isEqualTo("sysName");
                    assertThat(cv.getConstraintDescriptor().getAnnotation().annotationType())
                        .isEqualTo(NotEmpty.class);
                  }));
    }

    @Test
    @DisplayName("empty sysName violates @NotEmpty and @Size")
    void sysNameIsEmpty() {
      HousekeepFilesForm form = form("", aTaskRecord("AAA"));

      assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getConstraintViolations()).hasSize(2)
                  .allSatisfy(
                      cv -> assertThat(cv.getPropertyPath().toString()).isEqualTo("sysName")));
    }

    @Test
    @DisplayName("valid sysName passes")
    void sysNameIsValid() throws Exception {
      HousekeepFilesForm form = form("test-system", aTaskRecord("MOVE"));

      new HousekeepFilesBlf().execute(form);
    }
  }

  @Nested
  @DisplayName("execute(): ZIP_DELETE_ORIG task")
  class ExecuteZipDeleteOrig {

    @TempDir
    @SuppressWarnings("null")
    Path tempDir;

    private HousekeepFilesTaskRecord zipDeleteOrigRecord(String srcPath) {
      return new HousekeepFilesTaskRecord("01", "task01", "ZIP_DELETE_ORIG", "", srcPath, "FALSE",
          "DAY", "0", "ERROR", "", "", "TRUE", "IGNORE", null);
    }

    @Test
    @DisplayName("zips a single file and deletes the original")
    void zipOneFile() throws Exception {
      File fromFile = tempDir.resolve("test.txt").toFile();
      fromFile.createNewFile();
      File zippedFile = new File(fromFile.getAbsolutePath() + ".zip");

      HousekeepFilesForm form =
          form("test-system", zipDeleteOrigRecord(fromFile.getAbsolutePath()));

      assertThat(zippedFile).doesNotExist();

      new HousekeepFilesBlf().execute(form);

      assertThat(zippedFile).exists();
      assertThat(fromFile).doesNotExist();
    }

    @Test
    @DisplayName("zips each file matched by a wildcard and deletes the originals")
    void zipTwoFilesWithWildcard() throws Exception {
      File fromFile1 = tempDir.resolve("test1.txt").toFile();
      File fromFile2 = tempDir.resolve("test2.txt").toFile();
      fromFile1.createNewFile();
      fromFile2.createNewFile();
      File zippedFile1 = new File(fromFile1.getAbsolutePath() + ".zip");
      File zippedFile2 = new File(fromFile2.getAbsolutePath() + ".zip");

      HousekeepFilesForm form = form("test-system",
          zipDeleteOrigRecord(tempDir.resolve("test*.txt").toString()));

      assertThat(zippedFile1).doesNotExist();
      assertThat(zippedFile2).doesNotExist();

      new HousekeepFilesBlf().execute(form);

      assertThat(zippedFile1).exists();
      assertThat(zippedFile2).exists();
      assertThat(fromFile1).doesNotExist();
      assertThat(fromFile2).doesNotExist();
    }
  }
}

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

import java.io.File;
import java.nio.file.Path;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

/** Tests for {@link HousekeepFilesBlf}. */
@DisplayName("HousekeepFilesBlf")
class HousekeepFilesBlfTest {

  private HousekeepFilesForm form(HousekeepFilesTaskRecord taskRec) {
    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);

    return form;
  }

  @Nested
  @DisplayName("execute(): ZIP_DELETE_ORIG task")
  class ExecuteZipDeleteOrig {

    @TempDir
    @SuppressWarnings("null")
    Path tempDir;

    private HousekeepFilesTaskRecord zipDeleteOrigRecord(String srcPath) {
      return new HousekeepFilesTaskRecord("01", "task01", "ZIP_DELETE_ORIG", "", srcPath, "FALSE",
          "0", "ERROR", "", "", "TRUE", "IGNORE");
    }

    @Test
    @DisplayName("zips a single file and deletes the original")
    void zipOneFile() throws Exception {
      File fromFile = tempDir.resolve("test.txt").toFile();
      fromFile.createNewFile();
      File zippedFile = new File(fromFile.getAbsolutePath() + ".zip");

      HousekeepFilesForm form =
          form(zipDeleteOrigRecord(fromFile.getAbsolutePath()));

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

      // Built via java.io.File rather than Path#resolve: on Windows, Path validates
      // characters eagerly and rejects "*" with InvalidPathException.
      HousekeepFilesForm form = form(
          zipDeleteOrigRecord(new File(tempDir.toFile(), "test*.txt").getAbsolutePath()));

      assertThat(zippedFile1).doesNotExist();
      assertThat(zippedFile2).doesNotExist();

      new HousekeepFilesBlf().execute(form);

      assertThat(zippedFile1).exists();
      assertThat(zippedFile2).exists();
      assertThat(fromFile1).doesNotExist();
      assertThat(fromFile2).doesNotExist();
    }
  }

  @Nested
  @DisplayName("execute(form, env): path variable resolution")
  class ExecuteEnvVarResolution {

    @TempDir
    @SuppressWarnings("null")
    Path tempDir;

    private HousekeepFilesTaskRecord moveRecord(String srcPath, String destPath) {
      return new HousekeepFilesTaskRecord("01", "task01", "MOVE", null, srcPath, "FALSE",
          "0", "ERROR", destPath, "TRUE", "TRUE", "IGNORE");
    }

    @Test
    @DisplayName("resolves ${VAR} from the given Environment and executes the task")
    void resolvesPathVariableFromEnvironment() throws Exception {
      File fromFile = tempDir.resolve("from.txt").toFile();
      fromFile.createNewFile();
      File toDir = tempDir.resolve("to").toFile();
      toDir.mkdir();

      MockEnvironment env = new MockEnvironment();
      env.setProperty("BASE_DIR", tempDir.toString());

      HousekeepFilesForm form =
          form(moveRecord("${BASE_DIR}/from.txt", "${BASE_DIR}/to/"));

      new HousekeepFilesBlf().execute(form, env);

      assertThat(fromFile).doesNotExist();
      assertThat(toDir.toPath().resolve("from.txt").toFile()).exists();
    }

    @Test
    @DisplayName("throws when a referenced ${VAR} isn't resolvable via env")
    void throwsWhenVariableUnresolved() {
      HousekeepFilesForm form = form(
          moveRecord("${UNDEFINED_VAR}/from.txt", tempDir.resolve("to").toString()));

      assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form, new MockEnvironment()))
          .isInstanceOf(RuntimeException.class);
    }
  }
}

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.AbstractSftpTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link SftpCopyFromServer}, executed through {@link HousekeepFilesBlf}. */
@SuppressWarnings("null")
@DisplayName("SftpCopyFromServer")
class SftpCopyFromServerTest extends AbstractSftpTest {

  @TempDir
  Path tempDir;

  private HousekeepFilesForm form(HousekeepFilesTaskRecord taskRec) {
    HousekeepFilesAuthRecord authRec = new HousekeepFilesAuthRecord(SFTP_HOST, "SFTP",
        String.valueOf(sftpPort), "PASSWORD", SFTP_USER, SFTP_PASSWORD, null);

    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);
    form.getAuthInfoRecList().add(authRec);

    return form;
  }

  @Nested
  @DisplayName("input validation")
  class InputValidation {

    @Test
    @DisplayName("remoteServer is required")
    void remoteServerEmpty() {
      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "SFTP_COPY_FROM_SERVER", null, "aPath", "FALSE", "0", "IGNORE", "aPath", "FALSE",
          "FALSE", "IGNORE");
      HousekeepFilesForm form = form(rec);

      assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId)
                  .containsExactly("MSG_ERR_TASK_REQUIRED_CHECK"));
    }
  }

  @Nested
  @DisplayName("copy")
  class Copy {

    @Test
    @DisplayName("copies a file from the server to a new local path, leaving the remote file in "
        + "place")
    void copiesFileFromServer() throws Exception {
      String fromPath = SFTP_ROOT_PATH + "/from.txt";
      channel.put(new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)), fromPath);
      Path to = tempDir.resolve("to.txt");

      HousekeepFilesTaskRecord rec = new HousekeepFilesTaskRecord("aTaskId", "aTaskName",
          "SFTP_COPY_FROM_SERVER", SFTP_HOST, fromPath, "FALSE", "0", "IGNORE", to.toString(),
          "FALSE", "FALSE", "IGNORE");

      new HousekeepFilesBlf().execute(form(rec));

      assertThat(to).exists().hasContent("content");
      assertThat(sftpExists(channel, fromPath)).isTrue();
    }
  }
}

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

import com.jcraft.jsch.ChannelSftp.LsEntry;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.tool.housekeepfiles.bl.HousekeepFilesBl;
import jp.ecuacion.tool.housekeepfiles.blf.HousekeepFilesBlf;
import jp.ecuacion.tool.housekeepfiles.dto.form.HousekeepFilesForm;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesAuthRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesHdRecord;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.testtool.AbstractSftpTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link SftpCreateDir}, executed through {@link HousekeepFilesBlf}. */
@SuppressWarnings("null")
@DisplayName("SftpCreateDir")
class SftpCreateDirTest extends AbstractSftpTest {

  private HousekeepFilesForm form(HousekeepFilesTaskRecord taskRec) {
    HousekeepFilesAuthRecord authRec = new HousekeepFilesAuthRecord(SFTP_HOST, "SFTP",
        String.valueOf(sftpPort), "PASSWORD", SFTP_USER, SFTP_PASSWORD, null);

    HousekeepFilesForm form = new HousekeepFilesForm();
    form.getTaskInfoHdRec().recList.add(taskRec);
    form.getAuthInfoRecList().add(authRec);
    form.getTaskInfoHdRec().setSysName("test-system");

    return form;
  }

  private static void assertSingleBusinessViolation(HousekeepFilesForm form, String messageId) {
    assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form))
        .isInstanceOfSatisfying(ViolationException.class,
            ex -> assertThat(ex.getViolations().getBusinessViolations())
                .extracting(BusinessViolation::getMessageId).containsExactly(messageId));
  }

  /**
   * Asserts on the violation raised while the task runs, which reaches the caller wrapped in a
   * RuntimeException (unlike the pre-run input validation, which throws ViolationException
   * directly).
   */
  private static void assertSingleBusinessViolationWrappedInRuntimeException(
      HousekeepFilesForm form, String messageId) {
    assertThatThrownBy(() -> new HousekeepFilesBlf().execute(form))
        .isInstanceOf(RuntimeException.class).cause()
        .isInstanceOfSatisfying(ViolationException.class,
            ex -> assertThat(ex.getViolations().getBusinessViolations())
                .extracting(BusinessViolation::getMessageId).contains(messageId));
  }

  @Nested
  @DisplayName("input validation")
  class InputValidation {

    @Test
    @DisplayName("remoteServer is required")
    void remoteServerEmpty() {
      HousekeepFilesTaskRecord rec =
          new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "SFTP_CREATE_DIR", null, null, null,
              null, null, null, "aPath", "TRUE", "FALSE", "IGNORE", null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_REQUIRED_CHECK");
    }

    @Test
    @DisplayName("srcPath is prohibited")
    void srcPathNotEmpty() {
      HousekeepFilesTaskRecord rec =
          new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "SFTP_CREATE_DIR", SFTP_HOST,
              "aPath", "TRUE", "7", "DAY", "7", "aPath", "TRUE", "FALSE", "IGNORE", null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_PROHIBITED_CHECK");
    }

    @Test
    @DisplayName("destPath is required")
    void destPathEmpty() {
      HousekeepFilesTaskRecord rec =
          new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "SFTP_CREATE_DIR", SFTP_HOST, null,
              null, null, null, null, null, null, null, null, null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_REQUIRED_CHECK");
    }

    @Test
    @DisplayName("isDestPathDir cannot be FALSE")
    void isDestPathDirFalse() {
      HousekeepFilesTaskRecord rec =
          new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "SFTP_CREATE_DIR", SFTP_HOST, null,
              null, null, null, null, SFTP_ROOT_PATH + "/destPath", "FALSE", "FALSE", "IGNORE",
              null);

      assertSingleBusinessViolation(form(rec),
          "MSG_ERR_TASK_CANNOT_SET_IS_DEST_PATH_DIR_TO_VALUE");
    }

    @Test
    @DisplayName("doesOverwriteDestPath cannot be TRUE")
    void doesOverwriteDestPathTrue() {
      HousekeepFilesTaskRecord rec =
          new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "SFTP_CREATE_DIR", SFTP_HOST, null,
              null, null, null, null, SFTP_ROOT_PATH + "/test-dir", "TRUE", "TRUE", "IGNORE",
              null);

      assertSingleBusinessViolation(form(rec), "MSG_ERR_TASK_CANNOT_SET_OVERWRITE_TO_VALUE");
    }
  }

  @Nested
  @DisplayName("destPath already exists")
  class DestPathAlreadyExists {

    private HousekeepFilesTaskRecord sftpCreateDirRecord(String destPath,
        String whenDestPathExists) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "SFTP_CREATE_DIR", SFTP_HOST,
          null, null, null, null, null, destPath, "TRUE", "FALSE", whenDestPathExists, null);
    }

    private HousekeepFilesBl warnMailDetectingBl(AtomicBoolean warnMailSent) {
      // Replace HousekeepFilesBl#sendWarnMail to detect when it is called.
      return new HousekeepFilesBl() {
        @Override
        public void sendWarnMail(List<BusinessViolation> warnList, HousekeepFilesHdRecord hdE)
            throws Exception {
          warnMailSent.set(true);
        }
      };
    }

    @Test
    @DisplayName("existing directory with IGNORE: passes without changing the directory")
    void dirExistsIgnore() throws Exception {
      String dir = SFTP_ROOT_PATH + "/test-dir";
      String testFilePath = dir + "/test.txt";
      sftpCreateDir(channel, dir);
      sftpCreateFile(channel, testFilePath);
      // mtime (not atime) is compared: a mere directory listing during the existence check inside
      // SftpCreateDir can bump atime on filesystems using relatime semantics, which made this
      // assertion flaky around whole-second boundaries even though nothing was actually modified.
      // mtime only changes when directory entries are added/removed, so it reliably proves IGNORE
      // mode made no change.
      String mtimeBefore = sftpLsSelfDetail(channel, dir).getAttrs().getMtimeString();

      AtomicBoolean warnMailSent = new AtomicBoolean();
      new HousekeepFilesBlf(warnMailDetectingBl(warnMailSent))
          .execute(form(sftpCreateDirRecord(dir, "IGNORE")));

      LsEntry self = sftpLsSelfDetail(channel, dir);
      assertThat(self.getAttrs().getMtimeString()).isEqualTo(mtimeBefore);
      assertThat(sftpExists(channel, testFilePath)).isTrue();
      assertThat(warnMailSent).isFalse();
    }

    @Test
    @DisplayName("existing directory with WARN: warn mail is sent, directory unchanged")
    void dirExistsWarn() throws Exception {
      String dir = SFTP_ROOT_PATH + "/test-dir";
      String testFilePath = dir + "/test.txt";
      sftpCreateDir(channel, dir);
      sftpCreateFile(channel, testFilePath);
      // See dirExistsIgnore's comment: mtime is compared instead of atime to avoid flakiness from
      // the existence-check's directory listing bumping atime.
      String mtimeBefore = sftpLsSelfDetail(channel, dir).getAttrs().getMtimeString();

      AtomicBoolean warnMailSent = new AtomicBoolean();
      new HousekeepFilesBlf(warnMailDetectingBl(warnMailSent))
          .execute(form(sftpCreateDirRecord(dir, "WARN")));

      LsEntry self = sftpLsSelfDetail(channel, dir);
      assertThat(self.getAttrs().getMtimeString()).isEqualTo(mtimeBefore);
      assertThat(sftpExists(channel, testFilePath)).isTrue();
      assertThat(warnMailSent).isTrue();
    }

    @Test
    @DisplayName("existing directory with ERROR: MSG_ERR_DEST_PATH_EXISTS")
    void dirExistsError() throws Exception {
      String dir = SFTP_ROOT_PATH + "/test-dir";
      sftpCreateDir(channel, dir);

      assertSingleBusinessViolationWrappedInRuntimeException(
          form(sftpCreateDirRecord(dir, "ERROR")), "MSG_ERR_DEST_PATH_EXISTS");
    }

    @Test
    @DisplayName("existing file with IGNORE: MSG_ERR_DEST_PATH_IS_FILE")
    void fileExistsIgnore() throws Exception {
      String filePath = SFTP_ROOT_PATH + "/testfile.txt";
      sftpCreateFile(channel, filePath);

      assertSingleBusinessViolationWrappedInRuntimeException(
          form(sftpCreateDirRecord(filePath, "IGNORE")), "MSG_ERR_DEST_PATH_IS_FILE");
    }

    @Test
    @DisplayName("existing file with WARN: MSG_ERR_DEST_PATH_IS_FILE")
    void fileExistsWarn() throws Exception {
      String filePath = SFTP_ROOT_PATH + "/testfile.txt";
      sftpCreateFile(channel, filePath);

      assertSingleBusinessViolationWrappedInRuntimeException(
          form(sftpCreateDirRecord(filePath, "WARN")), "MSG_ERR_DEST_PATH_IS_FILE");
    }

    @Test
    @DisplayName("existing file with ERROR: MSG_ERR_DEST_PATH_IS_FILE")
    void fileExistsError() throws Exception {
      String filePath = SFTP_ROOT_PATH + "/testfile.txt";
      sftpCreateFile(channel, filePath);

      assertSingleBusinessViolationWrappedInRuntimeException(
          form(sftpCreateDirRecord(filePath, "ERROR")), "MSG_ERR_DEST_PATH_IS_FILE");
    }
  }

  @Nested
  @DisplayName("directory creation")
  class DirectoryCreation {

    private HousekeepFilesTaskRecord sftpCreateDirRecord(String destPath) {
      return new HousekeepFilesTaskRecord("aTaskId", "aTaskName", "SFTP_CREATE_DIR", SFTP_HOST,
          null, null, null, null, null, destPath, "TRUE", "FALSE", "ERROR", null);
    }

    @Test
    @DisplayName("creates a single-level directory")
    void singleLevel() throws Exception {
      String dir = SFTP_ROOT_PATH + "/test-dir";

      new HousekeepFilesBlf().execute(form(sftpCreateDirRecord(dir)));

      assertThat(sftpExists(channel, dir)).isTrue();
    }

    @Test
    @DisplayName("creates a multi-level directory")
    void multipleLevels() throws Exception {
      String dir = SFTP_ROOT_PATH + "/test-dir/1/2/3";

      new HousekeepFilesBlf().execute(form(sftpCreateDirRecord(dir)));

      assertThat(sftpExists(channel, dir)).isTrue();
    }
  }
}

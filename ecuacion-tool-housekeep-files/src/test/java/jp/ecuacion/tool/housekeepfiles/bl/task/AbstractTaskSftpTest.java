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
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepfiles.bean.ConnectionToRemoteServer;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;
import jp.ecuacion.tool.housekeepfiles.enums.TaskActionKindEnum;
import jp.ecuacion.tool.housekeepfiles.testtool.AbstractSftpTest;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the SFTP access methods of {@link AbstractTaskSftp}. */
@SuppressWarnings("null")
@DisplayName("AbstractTaskSftp")
class AbstractTaskSftpTest extends AbstractSftpTest {

  private final AbstractTaskSftp task = new AbstractTaskSftp() {
    @Override
    protected void doSpecificTask(@Nullable ConnectionToRemoteServer connection,
        HousekeepFilesTaskRecord taskRec, @Nullable String fromPath, @Nullable String toPath,
        List<BusinessViolation> warnList) throws Exception {}

    @Override
    public void taskDependentCheck(HousekeepFilesTaskRecord taskRec, Violations violations) {}

    @SuppressWarnings({"NullAway", "null"})
    @Override
    public TaskActionKindEnum getTaskActionKind() {
      return null;
    }

    @Override
    public @Nullable Boolean isSrcPathLocal() {
      return null;
    }

    @Override
    public @Nullable Boolean isDestPathLocal() {
      return null;
    }
  };

  /**
   * The default preparation resets the SFTP root directory with the very methods under test here,
   * so it is skipped; each test isolates itself with a random directory instead.
   */
  @Override
  protected void prepareRemoteTestRoot() {}

  /** Creates a path string that does not exist by incorporating a random value. */
  private String getTestRootPath() {
    return SFTP_ROOT_PATH + "/" + new Random().nextLong();
  }

  private void createTestRootDir(String testRootPath) {
    // Create if not exists, skip if already exists.
    try {
      channel.mkdir(SFTP_ROOT_PATH);
    } catch (Exception ex) {
      // Already exists. No action needed.
    }

    // Create if not exists, skip if already exists.
    try {
      channel.mkdir(testRootPath);
    } catch (Exception ex) {
      // Already exists. No action needed.
    }
  }

  private void createFile(String filePath) {
    try {
      channel.put(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)), filePath);

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static List<LsEntry> checkExistenceOfDotAndDoubleDotThenReturnElse(List<LsEntry> list) {
    assertThat(list).filteredOn(e -> e.getFilename().equals(".")).hasSize(1);
    assertThat(list).filteredOn(e -> e.getFilename().equals("..")).hasSize(1);

    return list.stream()
        .filter(e -> !e.getFilename().equals(".") && !e.getFilename().equals("..")).toList();
  }

  @Nested
  @DisplayName("getRemoteAll()")
  class GetRemoteAll {

    @Test
    @DisplayName("nonexistent path returns an empty list")
    void pathNotExist() throws Exception {
      assertThat(task.getRemoteAll(channel, getTestRootPath())).isEmpty();
    }

    @Test
    @DisplayName("file path returns the file entry with '.' and '..'")
    void fileExists() throws Exception {
      String testRootPath = getTestRootPath();
      String testFilePath = testRootPath + "/testfile";
      createTestRootDir(testRootPath);
      createFile(testFilePath);

      List<LsEntry> list = task.getRemoteAll(channel, testRootPath);

      assertThat(list).hasSize(3);
      assertThat(checkExistenceOfDotAndDoubleDotThenReturnElse(list)).singleElement()
          .satisfies(entry -> {
            assertThat(entry.getFilename()).isEqualTo("testfile");
            assertThat(entry.getAttrs().isDir()).isFalse();
          });
    }

    @Test
    @DisplayName("empty directory returns only '.' and '..'")
    void dirExists() throws Exception {
      String testRootPath = getTestRootPath();
      createTestRootDir(testRootPath);

      List<LsEntry> list = task.getRemoteAll(channel, testRootPath);

      assertThat(list).hasSize(2);
      assertThat(checkExistenceOfDotAndDoubleDotThenReturnElse(list)).isEmpty();
    }

    @Test
    @DisplayName("directory with children returns all child files and directories")
    void dirExistsWithChildFilesAndDirs() throws Exception {
      String testRootPath = getTestRootPath();
      createTestRootDir(testRootPath);
      createFile(testRootPath + "/childFile1.txt");
      createFile(testRootPath + "/childFile2.txt");
      channel.mkdir(testRootPath + "/childDir1");
      channel.mkdir(testRootPath + "/childDir2");

      List<LsEntry> list = task.getRemoteAll(channel, testRootPath);

      assertThat(list).hasSize(6);
      assertThat(checkExistenceOfDotAndDoubleDotThenReturnElse(list))
          .extracting(LsEntry::getFilename)
          .containsExactlyInAnyOrder("childFile1.txt", "childFile2.txt", "childDir1", "childDir2");
    }
  }

  @Nested
  @DisplayName("remoteExists()")
  class RemoteExists {

    @Test
    @DisplayName("nonexistent path returns false")
    void pathNotExist() throws Exception {
      assertThat(task.remoteExists(channel, getTestRootPath())).isFalse();
    }

    @Test
    @DisplayName("existing file returns true")
    void fileExists() throws Exception {
      String testRootPath = getTestRootPath();
      String testFilePath = testRootPath + "/testfile";
      createTestRootDir(testRootPath);
      createFile(testFilePath);

      assertThat(task.remoteExists(channel, testFilePath)).isTrue();
    }

    @Test
    @DisplayName("existing directory returns true")
    void dirExists() throws Exception {
      String testRootPath = getTestRootPath();
      createTestRootDir(testRootPath);

      assertThat(task.remoteExists(channel, testRootPath)).isTrue();
    }
  }

  @Nested
  @DisplayName("remoteDirExists()")
  class RemoteDirExists {

    @Test
    @DisplayName("nonexistent path returns false")
    void pathNotExist() throws Exception {
      assertThat(task.remoteDirExists(channel, getTestRootPath())).isFalse();
    }

    @Test
    @DisplayName("existing file returns false")
    void fileExists() throws Exception {
      String testRootPath = getTestRootPath();
      String testFilePath = testRootPath + "/testfile";
      createTestRootDir(testRootPath);
      createFile(testFilePath);

      assertThat(task.remoteDirExists(channel, testFilePath)).isFalse();
    }

    @Test
    @DisplayName("existing directory returns true")
    void dirExists() throws Exception {
      String testRootPath = getTestRootPath();
      createTestRootDir(testRootPath);

      assertThat(task.remoteDirExists(channel, testRootPath)).isTrue();
    }
  }

  @Nested
  @DisplayName("remoteFileExists()")
  class RemoteFileExists {

    @Test
    @DisplayName("nonexistent path returns false")
    void pathNotExist() throws Exception {
      assertThat(task.remoteFileExists(channel, getTestRootPath())).isFalse();
    }

    @Test
    @DisplayName("existing file returns true")
    void fileExists() throws Exception {
      String testRootPath = getTestRootPath();
      String testFilePath = testRootPath + "/testfile";
      createTestRootDir(testRootPath);
      createFile(testFilePath);

      assertThat(task.remoteFileExists(channel, testFilePath)).isTrue();
    }

    @Test
    @DisplayName("existing directory returns false")
    void dirExists() throws Exception {
      String testRootPath = getTestRootPath();
      createTestRootDir(testRootPath);

      assertThat(task.remoteFileExists(channel, testRootPath)).isFalse();
    }
  }

  @Nested
  @DisplayName("getRemoteDetail()")
  class GetRemoteDetail {

    @Test
    @DisplayName("nonexistent path returns null")
    void pathNotExist() throws Exception {
      assertThat(task.getRemoteDetail(channel, getTestRootPath())).isNull();
    }

    @Test
    @DisplayName("existing file returns the file entry")
    void fileExists() throws Exception {
      String testRootPath = getTestRootPath();
      String testFilePath = testRootPath + "/testfile";
      createTestRootDir(testRootPath);
      createFile(testFilePath);

      LsEntry entry = Objects.requireNonNull(task.getRemoteDetail(channel, testFilePath));

      assertThat(entry.getFilename()).isEqualTo("testfile");
      assertThat(entry.getAttrs().isDir()).isFalse();
    }

    @Test
    @DisplayName("existing directory returns the '.' entry")
    void dirExists() throws Exception {
      String testRootPath = getTestRootPath();
      createTestRootDir(testRootPath);

      LsEntry entry = Objects.requireNonNull(task.getRemoteDetail(channel, testRootPath));

      assertThat(entry.getFilename()).isEqualTo(".");
      assertThat(entry.getAttrs().isDir()).isTrue();
    }
  }

  @Nested
  @DisplayName("getRemoteDirChildrenList()")
  class GetRemoteDirChildrenList {

    @Test
    @DisplayName("nonexistent path throws RuntimeException")
    void pathNotExist() {
      assertThatThrownBy(() -> task.getRemoteDirChildrenList(channel, getTestRootPath()))
          .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("file path throws RuntimeException")
    void fileExists() {
      String testRootPath = getTestRootPath();
      String testFilePath = testRootPath + "/testfile";
      createTestRootDir(testRootPath);
      createFile(testFilePath);

      assertThatThrownBy(() -> task.getRemoteDirChildrenList(channel, testFilePath))
          .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("empty directory returns an empty list")
    void dirExistsNoChildren() throws Exception {
      String testRootPath = getTestRootPath();
      createTestRootDir(testRootPath);

      assertThat(task.getRemoteDirChildrenList(channel, testRootPath)).isEmpty();
    }

    @Test
    @DisplayName("directory with children returns them without '.' and '..'")
    void dirExistsWithChildren() throws Exception {
      String testRootPath = getTestRootPath();
      createTestRootDir(testRootPath);
      createFile(testRootPath + "/childFile1.txt");
      channel.mkdir(testRootPath + "/childDir1");

      assertThat(task.getRemoteDirChildrenList(channel, testRootPath))
          .extracting(LsEntry::getFilename)
          .containsExactlyInAnyOrder("childFile1.txt", "childDir1");
    }
  }

  @Nested
  @DisplayName("getRemoteDirChildrenNameList()")
  class GetRemoteDirChildrenNameList {

    @Test
    @DisplayName("nonexistent path throws RuntimeException")
    void pathNotExist() {
      assertThatThrownBy(() -> task.getRemoteDirChildrenNameList(channel, getTestRootPath()))
          .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("file path throws RuntimeException")
    void fileExists() {
      String testRootPath = getTestRootPath();
      String testFilePath = testRootPath + "/testfile";
      createTestRootDir(testRootPath);
      createFile(testFilePath);

      assertThatThrownBy(() -> task.getRemoteDirChildrenNameList(channel, testFilePath))
          .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("empty directory returns an empty list")
    void dirExistsNoChildren() throws Exception {
      String testRootPath = getTestRootPath();
      createTestRootDir(testRootPath);

      assertThat(task.getRemoteDirChildrenNameList(channel, testRootPath)).isEmpty();
    }

    @Test
    @DisplayName("directory with children returns their names")
    void dirExistsWithChildren() throws Exception {
      String testRootPath = getTestRootPath();
      createTestRootDir(testRootPath);
      createFile(testRootPath + "/childFile1.txt");
      channel.mkdir(testRootPath + "/childDir1");

      assertThat(task.getRemoteDirChildrenNameList(channel, testRootPath))
          .containsExactlyInAnyOrder("childFile1.txt", "childDir1");
    }
  }
}

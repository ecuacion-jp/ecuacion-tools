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
package jp.ecuacion.tool.housekeepfiles.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.tool.housekeepfiles.dto.record.HousekeepFilesTaskRecord;

/**
 * Provides utilities.
 */
@SuppressWarnings("NullAway")
public class HkFileManipulateUtil {

  /**
   * Checks if overwritten files or directories exist.
   */
  public boolean checkIfToOverwrittenFileOrDirExists(HousekeepFilesTaskRecord taskRec,
      String fromPath, String toPath) {

    if (taskRec.getIsDestPathDir() == false) {
      // When the destination is a file.
      // Taking file extension into account.
      // toPath = toPath + extension;
      return new File(toPath).exists();

    } else {
      // When the destination is a directory, check inside that directory.
      // Covers both from=directory and from=file cases: check whether a file/dir with the same
      // name as the from entry exists under the to directory.
      return new File(FileUtil.concatFilePaths(toPath, new File(fromPath).getName())).exists();
    }
  }

  /**
   * Returns {@code true} if {@code dirPath} (a directory) contains a symbolic link anywhere in
   * its subtree, including at its own top level.
   *
   * <p>Used to guard directory copy/move: {@code commons-io}'s {@code copyDirectory}/
   * {@code moveDirectory} follow symbolic links, and a monitored directory can contain entries
   * placed by a less-trusted party (e.g. an upload drop location). Following a symlink there
   * (e.g. {@code link -> /etc}) would copy/move file-system content the operator never intended
   * to include, and a self-referential link (e.g. {@code link -> ..}) would recurse forever.
   * {@link Files#walk(Path, java.nio.file.FileVisitOption...)} does not follow symbolic links by
   * default, so it safely reports each link itself without descending into it.</p>
   */
  public boolean containsSymbolicLink(String dirPath) throws IOException {
    File dir = new File(dirPath);
    if (!dir.isDirectory()) {
      return false;
    }

    try (Stream<Path> stream = Files.walk(dir.toPath())) {
      return stream.anyMatch(Files::isSymbolicLink);
    }
  }

}

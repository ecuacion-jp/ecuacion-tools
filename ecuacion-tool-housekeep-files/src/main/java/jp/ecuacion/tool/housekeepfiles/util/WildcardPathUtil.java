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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.util.FileUtil;
import jp.ecuacion.lib.core.util.ObjectsUtil;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

/**
 * Provides wildcard-based path expansion utilities for local file system paths.
 *
 * <p>This utility is specific to {@code ecuacion-tool-housekeep-files} and is not
 * intended as a general-purpose library component.</p>
 */
public class WildcardPathUtil {

  private WildcardPathUtil() {}

  /**
   * Returns true if the argument path contains wildcard strings.
   */
  public static boolean containsWildCard(String path) {
    return (path.contains("?") || path.contains("*"));
  }

  /**
   * Returns a list of paths which match the path passed by the argument path with wildcards.
   *
   * <p>"*", "?" are supported, but "**" not supported.<br>
   * The separator of returning Paths is "/"</p>
   *
   * <p><strong>Security note:</strong> This method does not sanitize {@code ../} sequences.
   *     Callers must not pass paths constructed directly from untrusted user input,
   *     as doing so may allow path traversal to unintended directories.</p>
   */
  public static List<@NonNull String> getPathListFromPathWithWildcard(String path) {

    final List<@NonNull String> fullPathList = new ArrayList<>();
    path = FileUtil.cleanPathStrWithSlash(path);
    if (path.endsWith("/") || path.endsWith("\\")) {
      path = path.substring(0, path.length() - 1);
    }

    if (isRelativePath(path)) {
      path = changeRelPathToFullPath(path);
    }

    path = FileUtil.cleanPathStrWithSlash(path);
    getPathListFromPathWithWildcardRecursively(path, "", fullPathList);

    return fullPathList;
  }

  /**
   * Returns true if the path is relative.
   *
   * @param path path
   * @return true if the path is relative
   */
  public static boolean isRelativePath(String path) {
    if (StringUtils.isEmpty(path)) {
      throw new ViolationException(
          new Violations().add(new BusinessViolation("MSG_ERR_PATH_IS_NULL")));
    }

    if (Objects.requireNonNull(System.getProperty("os.name")).toUpperCase(Locale.ROOT)
        .contains("WINDOWS")) {
      if (path.length() >= 2 && path.substring(1, 2).equals(":")) {
        return false;
      }

    } else {
      if (path.length() >= 1 && path.substring(0, 1).equals("/")) {
        return false;
      }
    }

    return true;
  }

  private static String changeRelPathToFullPath(String path) {
    String curPath = new File(".").getAbsolutePath();
    String fullPath = FileUtil.concatFilePaths(curPath, path);
    fullPath = fullPath.replaceAll("\\.\\\\", "").replaceAll("\\./", "");
    return fullPath;
  }

  /*
   * Returns the leftmost separator position of the path in the path string.
   * Supports both slash (/) and backslash (\).
   * Returns -1 if there is no separator position.
   */
  private static int getFirstPathSeparatorIndex(String path) {
    ObjectsUtil.requireNonNull(path);

    int firstSlashIndex = path.indexOf("/");
    int firstBackSlashIndex = path.indexOf("\\");

    if (firstSlashIndex == -1 && firstBackSlashIndex == -1) {
      return -1;

    } else if (firstSlashIndex == -1) {
      return firstBackSlashIndex;

    } else if (firstBackSlashIndex == -1) {
      return firstSlashIndex;

    } else {
      return (firstSlashIndex < firstBackSlashIndex) ? firstSlashIndex : firstBackSlashIndex;
    }
  }

  private static void getPathListFromPathWithWildcardRecursively(String fullPath, String parentPath,
      List<@NonNull String> rtnFullPathList) {
    ObjectsUtil.requireNonNull(fullPath, parentPath, rtnFullPathList);

    String myFileOrDirnameWithWildcard = null;
    boolean hasReachedFullPathDirDepth = false;

    if (parentPath.isEmpty()) {
      String myPathWithWildcard = fullPath.substring(0, getFirstPathSeparatorIndex(fullPath) + 1);
      if (myPathWithWildcard.contains("*") || myPathWithWildcard.contains("?")) {
        throw new ViolationException(new Violations().add(
            new BusinessViolation("MSG_ERR_1ST_LEVEL_CANNOT_HAVE_WILDCARD", fullPath)));
      }

      getPathListFromPathWithWildcardRecursively(fullPath, myPathWithWildcard, rtnFullPathList);

    } else {
      int numOfSeparatorOfParentPath = StringUtils.countMatches(parentPath, "/");
      String fullPathMinusParentPath = fullPath
          .substring(StringUtils.ordinalIndexOf(fullPath, "/", numOfSeparatorOfParentPath) + 1);
      int ind = getFirstPathSeparatorIndex(fullPathMinusParentPath);
      if (ind >= 0) {
        myFileOrDirnameWithWildcard = fullPathMinusParentPath.substring(0, ind + 1);
      } else {
        myFileOrDirnameWithWildcard = fullPathMinusParentPath;
        hasReachedFullPathDirDepth = true;
      }

      if (myFileOrDirnameWithWildcard.contains("?") || myFileOrDirnameWithWildcard.contains("*")) {
        String myFileOrDirnameWithRegEx = myFileOrDirnameWithWildcard.replaceAll("\\.", "\\\\.");
        myFileOrDirnameWithRegEx =
            myFileOrDirnameWithRegEx.replaceAll("\\?", ".").replaceAll("\\*", ".*");
        Pattern pattern1 = Pattern.compile(parentPath + myFileOrDirnameWithRegEx);

        String[] arr = new File(parentPath).list();
        if (arr == null) {
          throw new RuntimeException("arr cannot be null.");
        }

        for (String path : arr) {
          String myFullPath = parentPath + path;
          myFullPath = FileUtil.cleanPathStrWithSlash(myFullPath);

          Matcher matcher = pattern1.matcher(myFullPath);
          if (matcher.matches()) {
            if (hasReachedFullPathDirDepth) {
              rtnFullPathList.add(myFullPath);
            } else {
              getPathListFromPathWithWildcardRecursively(fullPath, myFullPath, rtnFullPathList);
            }
          }
        }
      } else {
        String myFullPath = parentPath + myFileOrDirnameWithWildcard;
        if (new File(myFullPath).exists()) {
          if (hasReachedFullPathDirDepth) {
            rtnFullPathList.add(myFullPath);
          } else {
            getPathListFromPathWithWildcardRecursively(fullPath, myFullPath, rtnFullPathList);
          }
        }
      }
    }
  }
}

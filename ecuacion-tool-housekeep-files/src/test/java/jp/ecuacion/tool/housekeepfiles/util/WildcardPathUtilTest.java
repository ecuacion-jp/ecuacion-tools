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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import jp.ecuacion.lib.core.exception.ViolationException;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests for {@link WildcardPathUtil}. */
@SuppressWarnings("null")
@DisplayName("WildcardPathUtil")
class WildcardPathUtilTest {

  @BeforeEach
  void skipOnWindows() {
    // isRelativePath()/getPathListFromPathWithWildcard() branch on os.name; these tests assume
    // the Unix-style absolute-path branch ("/" prefix), matching this project's dev/CI platforms.
    assumeFalse(Objects.requireNonNull(System.getProperty("os.name")).toUpperCase(Locale.ROOT)
        .contains("WINDOWS"));
  }

  @Nested
  @DisplayName("containsWildCard()")
  class ContainsWildCard {

    @ParameterizedTest(name = "path={0} -> {1}")
    @CsvSource({
        "/a/b/c.txt, false",
        "/a/*/c.txt, true",
        "/a/b?.txt, true",
        "/a/b*c?.txt, true"})
    void containsWildCard(String path, boolean expected) {
      assertThat(WildcardPathUtil.containsWildCard(path)).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("isRelativePath()")
  class IsRelativePath {

    @Test
    @DisplayName("an absolute unix path returns false")
    void absolutePath() {
      assertThat(WildcardPathUtil.isRelativePath("/a/b")).isFalse();
    }

    @Test
    @DisplayName("a relative path returns true")
    void relativePath() {
      assertThat(WildcardPathUtil.isRelativePath("a/b")).isTrue();
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("null throws ViolationException with MSG_ERR_PATH_IS_NULL")
    void nullPath() {
      assertThatThrownBy(() -> WildcardPathUtil.isRelativePath(null))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId).containsExactly("MSG_ERR_PATH_IS_NULL"));
    }

    @Test
    @DisplayName("empty string throws ViolationException with MSG_ERR_PATH_IS_NULL")
    void emptyPath() {
      assertThatThrownBy(() -> WildcardPathUtil.isRelativePath(""))
          .isInstanceOfSatisfying(ViolationException.class,
              ex -> assertThat(ex.getViolations().getBusinessViolations())
                  .extracting(BusinessViolation::getMessageId).containsExactly("MSG_ERR_PATH_IS_NULL"));
    }
  }

  @Nested
  @DisplayName("getPathListFromPathWithWildcard()")
  class GetPathListFromPathWithWildcard {

    @Test
    @DisplayName("no wildcard, existing file: returns the single path")
    void noWildcardExistingFile(@TempDir Path tempDir) throws Exception {
      File file = tempDir.resolve("a.txt").toFile();
      file.createNewFile();

      assertThat(WildcardPathUtil.getPathListFromPathWithWildcard(file.getAbsolutePath()))
          .containsExactly(file.getAbsolutePath());
    }

    @Test
    @DisplayName("no wildcard, nonexistent path: returns an empty list")
    void noWildcardNonexistentPath(@TempDir Path tempDir) {
      String path = tempDir.resolve("nonexistent.txt").toString();

      assertThat(WildcardPathUtil.getPathListFromPathWithWildcard(path)).isEmpty();
    }

    @Test
    @DisplayName("'*' matches multiple sibling files")
    void wildcardMatchesMultipleFiles(@TempDir Path tempDir) throws Exception {
      File file1 = tempDir.resolve("test1.txt").toFile();
      File file2 = tempDir.resolve("test2.txt").toFile();
      File other = tempDir.resolve("other.txt").toFile();
      file1.createNewFile();
      file2.createNewFile();
      other.createNewFile();

      String pattern = new File(tempDir.toFile(), "test*.txt").getAbsolutePath();

      List<String> result = WildcardPathUtil.getPathListFromPathWithWildcard(pattern);

      assertThat(result).containsExactlyInAnyOrder(file1.getAbsolutePath(),
          file2.getAbsolutePath());
    }

    @Test
    @DisplayName("'?' matches a single character")
    void singleCharWildcard(@TempDir Path tempDir) throws Exception {
      File file1 = tempDir.resolve("a1.txt").toFile();
      File file2 = tempDir.resolve("a22.txt").toFile();
      file1.createNewFile();
      file2.createNewFile();

      String pattern = new File(tempDir.toFile(), "a?.txt").getAbsolutePath();

      assertThat(WildcardPathUtil.getPathListFromPathWithWildcard(pattern))
          .containsExactly(file1.getAbsolutePath());
    }

    @Test
    @DisplayName("wildcard matching a directory returns that directory's path")
    void wildcardMatchesDirectory(@TempDir Path tempDir) throws Exception {
      File dir = tempDir.resolve("childDir").toFile();
      dir.mkdir();

      String pattern = new File(tempDir.toFile(), "child*").getAbsolutePath();

      assertThat(WildcardPathUtil.getPathListFromPathWithWildcard(pattern))
          .containsExactly(dir.getAbsolutePath());
    }

    @Test
    @DisplayName("an intermediate directory whose real name contains unbalanced regex "
        + "metacharacters (e.g. '(') does not break a wildcard match below it")
    void intermediateDirWithRegexMetacharactersInName(@TempDir Path tempDir) throws Exception {
      // The wildcard segment ("*.txt") is below this directory, so its name ends up interpolated
      // as-is into the generated regex as the literal prefix (parentPath) - an unbalanced '(' here
      // used to throw PatternSyntaxException instead of matching literally.
      File weirdDir = tempDir.resolve("weird(unbalanced").toFile();
      weirdDir.mkdir();
      File file = new File(weirdDir, "data.txt");
      file.createNewFile();

      String pattern = new File(weirdDir, "*.txt").getAbsolutePath();

      assertThat(WildcardPathUtil.getPathListFromPathWithWildcard(pattern))
          .containsExactly(file.getAbsolutePath());
    }

    @Test
    @DisplayName("a literal '+' next to a wildcard in the same segment matches only a literal "
        + "'+', not the regex quantifier meaning \"one or more of the preceding character\"")
    void literalPlusInWildcardSegmentIsNotARegexQuantifier(@TempDir Path tempDir)
        throws Exception {
      // Under the pre-fix behavior, "+" was passed through unescaped into the compiled regex, so
      // the segment "a+*.txt" became the regex "a+.*\.txt" ("one or more 'a's" + anything +
      // ".txt"), which would wrongly match "aaax.txt" (no literal '+' at all).
      File hasLiteralPlus = new File(tempDir.toFile(), "a+x.txt");
      hasLiteralPlus.createNewFile();
      File noLiteralPlus = new File(tempDir.toFile(), "aaax.txt");
      noLiteralPlus.createNewFile();

      String pattern = new File(tempDir.toFile(), "a+*.txt").getAbsolutePath();

      assertThat(WildcardPathUtil.getPathListFromPathWithWildcard(pattern))
          .containsExactly(hasLiteralPlus.getAbsolutePath());
    }
  }
}

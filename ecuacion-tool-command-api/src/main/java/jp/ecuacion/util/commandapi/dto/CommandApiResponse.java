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
package jp.ecuacion.util.commandapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Contains the result of a script execution, returned as the JSON response body.
 *
 * <p>{@code stdoutTruncated} / {@code stderrTruncated} are present in the JSON only when the
 *     corresponding output actually was truncated (rather than always being present as
 *     {@code false}), so the common (untruncated) case isn't cluttered with a field that never
 *     matters.</p>
 *
 * @param returnCode the script's exit code
 * @param stdout the script's captured standard output, lines joined by the line separator
 * @param stderr the script's captured standard error, lines joined by the line separator
 * @param stdoutTruncated {@code true} if {@code stdout} was truncated, may be {@code null}
 *     (omitted from the JSON) otherwise
 * @param stderrTruncated {@code true} if {@code stderr} was truncated, may be {@code null}
 *     (omitted from the JSON) otherwise
 */
public record CommandApiResponse(int returnCode, String stdout, String stderr,
    @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable Boolean stdoutTruncated,
    @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable Boolean stderrTruncated) {
}

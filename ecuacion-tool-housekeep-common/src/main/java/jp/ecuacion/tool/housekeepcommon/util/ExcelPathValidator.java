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
package jp.ecuacion.tool.housekeepcommon.util;

import jakarta.validation.Validation;
import java.io.IOException;
import java.util.Objects;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.util.excel.util.ExcelReadUtil;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;

/**
 * Validates the excel settings file path shared by housekeep-db's and housekeep-files' tasklets.
 */
public final class ExcelPathValidator {

  private ExcelPathValidator() {}

  /**
   * Validates {@code excelPath} and returns it as non-null once confirmed openable.
   *
   * @param excelPathBean the caller (typically the tasklet itself), whose {@code excelPath}
   *     field carries the {@code @NotEmpty @FileExists @FileExtension(".xlsx")} annotations bean
   *     validation runs against. Passing the caller itself (rather than a validator-owned holder
   *     bean) keeps the resolved item name (e.g. {@code housekeepDbTasklet.excelPath}) unchanged.
   * @param excelPath the same value as {@code excelPathBean}'s {@code excelPath} field
   * @return {@code excelPath}, confirmed non-null and openable as an excel file
   * @throws jp.ecuacion.lib.core.exception.ViolationException when {@code excelPath} is
   *     empty, doesn't point to an existing {@code .xlsx} file (both via bean validation), or
   *     can't be opened as an excel file (message ID {@code MSG_ERR_EXCEL_PATH_CANNOT_OPEN},
   *     which the caller's module must define in its own {@code messages.properties})
   */
  public static String validate(Object excelPathBean, @Nullable String excelPath) {
    new Violations()
        .addAll(Validation.buildDefaultValidatorFactory().getValidator().validate(excelPathBean))
        .messageParameters(Violations.newMessageParameters().isMessageWithItemName(true))
        .throwIfAny();

    String nonnullExcelPath = Objects.requireNonNull(excelPath);

    try (Workbook workbook = ExcelReadUtil.openForRead(nonnullExcelPath)) {
      // Only verifying the file can be opened as an excel file here.
      // Its content is read later.
    } catch (EncryptedDocumentException | IOException e) {
      new Violations()
          .add(new BusinessViolation("MSG_ERR_EXCEL_PATH_CANNOT_OPEN", nonnullExcelPath))
          .throwIfAny();
    }

    return nonnullExcelPath;
  }
}

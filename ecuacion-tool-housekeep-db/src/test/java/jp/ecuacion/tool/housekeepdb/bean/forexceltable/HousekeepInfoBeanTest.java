package jp.ecuacion.tool.housekeepdb.bean.forexceltable;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HousekeepInfoBeanTest {

  private static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @DisplayName("requiredTest")
  @Test
  public void inputValidationCheckTest() throws BizLogicAppException {

    // A-G columns are required

    List<String> list = Arrays.asList(new String[] {null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null});

    Set<ConstraintViolation<@NonNull HousekeepInfoBean>> set =
        validator.validate(new HousekeepInfoBean(list));

    Assertions.assertEquals(7, set.size());

    for (ConstraintViolation<?> cv : set) {
      Assertions.assertEquals("jakarta.validation.constraints.NotEmpty",
          cv.getConstraintDescriptor().getAnnotation().getClass().getCanonicalName());

      String field = cv.getPropertyPath().toString();

      // field must be one of the one in A-F column
      boolean bl = Arrays
          .asList(new String[] {"taskId", "dbConnectionInfoId", "isSoftDelete",
              "isSoftDeleteInternalValue", "table", "idColumn", "idColumnNeedsQuotationMark"})
          .contains(field);
      Assertions.assertEquals(true, bl);
    }

    // D (isSoftDeleteInternalValue) column is either "SOFT_DELETE" or "HARD_DELETE".
    // F (idColumnNeedsQuotationMark) column is either "(none)" or "quotes(')".

    list = Arrays.asList(new String[] {"taskId", "dbConnectionInfoId", "isSoftDelete",
        "isSoftDeleteInternalValue", "table", "idColumn", "idColumnNeedsQuotationMark", null, null,
        null, null, null, null, null, null});
    set = validator.validate(new HousekeepInfoBean(list));
    Assertions.assertEquals(2, set.size());
    for (ConstraintViolation<?> cv : set) {
      Assertions.assertEquals("jakarta.validation.constraints.Pattern",
          cv.getConstraintDescriptor().getAnnotation().getClass().getCanonicalName());
    }

    // J (softDeleteColumn) column is required only when isSoftDelete == true

    list = Arrays.asList(new String[] {"taskId", "dbConnectionInfoId", "論理廃止", "SOFT_DELETE",
        "table", "idColumn", "(none)", null, null, null, null, null, null, null, null});
    set = validator.validate(new HousekeepInfoBean(list));
    Assertions.assertEquals(1, set.size());
    Assertions.assertEquals("jp.ecuacion.lib.validation.constraints.NotEmptyWhen", set.iterator()
        .next().getConstraintDescriptor().getAnnotation().getClass().getCanonicalName());

    // no error with "HARD_DELETE" and "quotes(')".

    list = Arrays.asList(new String[] {"taskId", "dbConnectionInfoId", "削除", "HARD_DELETE", "table",
        "idColumn", "quotes(')", null, null, null, null, null, null, null, null});
    Set<ConstraintViolation<@NonNull HousekeepInfoBean>> opt =
        validator.validate(new HousekeepInfoBean(list));
    Assertions.assertTrue(opt.isEmpty());

    // soft-delete related columns needs to be empty when "HARD_DELETE"
    // softDeleteUpdateUserIdColumnNeedsQuotationMark should be one of the patterns

    list = Arrays.asList(
        new String[] {"taskId", "dbConnectionInfoId", "削除", "HARD_DELETE", "table", "idColumn",
            "(none)", "lst_upd_time", "OffsetDateTime", "30", "rem_flg", "a", "b", "c", "d"});
    set = validator.validate(new HousekeepInfoBean(list));
    Assertions.assertEquals(2, set.size());
  }
}

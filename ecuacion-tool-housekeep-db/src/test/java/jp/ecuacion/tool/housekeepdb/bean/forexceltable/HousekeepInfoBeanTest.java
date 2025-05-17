package jp.ecuacion.tool.housekeepdb.bean.forexceltable;

import java.util.Arrays;
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.exception.checked.SingleAppException;
import jp.ecuacion.lib.core.exception.checked.ValidationAppException;
import jp.ecuacion.lib.core.util.ValidationUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HousekeepInfoBeanTest {

  @DisplayName("requiredTest")
  @Test
  public void inputValidationCheckTest() throws BizLogicAppException {

    // A-G columns are required

    List<String> list = Arrays.asList(new String[] {null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null});

    MultipleAppException multiple = ValidationUtil.validateThenReturn(new HousekeepInfoBean(list)).get();

    Assertions.assertEquals(7, multiple.getList().size());

    for (SingleAppException ex : multiple.getList()) {
      Assertions.assertTrue(ex instanceof ValidationAppException);
      ValidationAppException valEx = ((ValidationAppException) ex);

      Assertions.assertEquals("jakarta.validation.constraints.NotEmpty",
          valEx.getConstraintViolationBean().getMessageId());

      String field = valEx.getConstraintViolationBean().getPropertyPath();

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
    multiple = ValidationUtil.validateThenReturn(new HousekeepInfoBean(list)).get();
    Assertions.assertEquals(2, multiple.getList().size());
    for (SingleAppException ex : multiple.getList()) {
      Assertions.assertTrue(ex instanceof ValidationAppException);
      ValidationAppException valEx = ((ValidationAppException) ex);
      Assertions.assertEquals("jakarta.validation.constraints.Pattern",
          valEx.getConstraintViolationBean().getMessageId());
    }

    // J (softDeleteColumn) column is required only when isSoftDelete == true

    list = Arrays.asList(new String[] {"taskId", "dbConnectionInfoId", "論理廃止", "SOFT_DELETE",
        "table", "idColumn", "(none)", null, null, null, null, null, null, null, null});
    multiple = ValidationUtil.validateThenReturn(new HousekeepInfoBean(list)).get();
    Assertions.assertEquals(1, multiple.getList().size());
    ValidationAppException valEx = (ValidationAppException) multiple.getList().get(0);
    Assertions.assertEquals("jp.ecuacion.lib.core.jakartavalidation.validator.ConditionalNotEmpty",
        valEx.getConstraintViolationBean().getAnnotation());

    // no error with "HARD_DELETE" and "quotes(')".

    list = Arrays.asList(new String[] {"taskId", "dbConnectionInfoId", "削除", "HARD_DELETE", "table",
        "idColumn", "quotes(')", null, null, null, null, null, null, null, null});
    multiple = ValidationUtil.validateThenReturn(new HousekeepInfoBean(list)).get();
    Assertions.assertEquals((String) null, multiple);

    // soft-delete related columns needs to be empty when "HARD_DELETE"
    // softDeleteUpdateUserIdColumnNeedsQuotationMark should be one of the patterns

    list = Arrays.asList(
        new String[] {"taskId", "dbConnectionInfoId", "削除", "HARD_DELETE", "table", "idColumn",
            "(none)", "lst_upd_time", "OffsetDateTime", "30", "rem_flg", "a", "b", "c", "d"});
    multiple = ValidationUtil.validateThenReturn(new HousekeepInfoBean(list)).get();
    Assertions.assertEquals(2, multiple.getList().size());
  }
}

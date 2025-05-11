package jp.ecuacion.tool.housekeepdb.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;

public class SqlUtil {

  private OffsetDateTime now = OffsetDateTime.now();

  public String getTimestampNow(String protocol) {
    if (protocol.equals("postgresql")) {
      String str = now.format(DateTimeFormatter.ISO_DATE_TIME);
      return str;

    } else {
      throw new RuntimeException("Protocol not recognized. protocol: " + protocol);
    }
  }

  public String getWhere(List<SqlConditionInterface> list) {
    StringBuilder sb = new StringBuilder();

    boolean is1st = true;
    for (SqlConditionInterface bean : list) {
      if (is1st) {
        sb.append(" where ");
        is1st = false;

      } else {
        sb.append(" and ");
      }

      sb.append(bean.getCondition());
    }

    return sb.toString();
  }

  public String getWhere(SqlConditionInterface... array) {
    return getWhere(Arrays.asList(array));
  }

  public String getUpdateSet(List<SqlConditionInterface> list) {
    StringBuilder sb = new StringBuilder();
    sb.append("\nset ");

    boolean is1st = true;
    for (SqlConditionInterface bean : list) {
      if (is1st) {
        is1st = false;

      } else {
        sb.append(", ");
      }

      sb.append(bean.getCondition());
    }

    return sb.toString();
  }

  public String getUpdateSet(SqlConditionInterface... array) {
    return getUpdateSet(Arrays.asList(array));
  }
}

package jp.ecuacion.tool.housekeepdb.lang;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import jp.ecuacion.lib.core.util.PropertyFileUtil;

/**
 * Provides message IDs to internationalize excel settings file.
 */
public class LangExcel {
  public static final String DB_CONNECTION_SETTINGS = "EXCEL_SHEET_DB_CONNECTION_SETTINGS";
  public static final String HOUSEKEEP_DB_SETTINGS = "EXCEL_SHEET_HOUSEKEEP_DB_SETTINGS";
  public static final String RELATED_TABLE_SETTINGS = "EXCEL_SHEET_RELATED_TABLE_SETTINGS";
  public static final String SEARCH_CONDITION_SETTINGS = "EXCEL_SHEET_SEARCH_CONDITION_SETTINGS";

  private Locale locale;

  /**
   * Constructs a new instance.
   * 
   * @param locale locale
   */
  public LangExcel(Locale locale) {
    this.locale = locale;
  }

  /**
   * Gets localized message from {@code key}.
   * 
   * @param key message ID
   * @return message
   */
  public String get(String key) {
    return PropertyFileUtil.getMessage(locale, key);
  }

  /**
   * Provides a localized array of header labels.
   * 
   * @param headerLabelKeys headerLabelKeys
   * @return localized header labels
   */
  public String[] getHeaderLabels(String[] headerLabelKeys) {
    List<String> list = Arrays.asList(headerLabelKeys).stream().map(key -> get(key)).toList();
    return list.toArray(new String[list.size()]);
  }

  /**
   * Contains message IDs related to DbConnectionSettings.
   */
  public static class DbConnectionSettings {
    public static final String DB_CONNECTION_ID = "EXCEL_TABLE_HEADER_DB_CONNECTION_ID";
    public static final String DB_DRIVER_NAME = "EXCEL_TABLE_HEADER_DB_DRIVER_NAME";
    public static final String DB_CONNECTION_URL_PROTOCOL =
        "EXCEL_TABLE_HEADER_DB_CONNECTION_URL_PROTOCOL";
    public static final String DB_CONNECTION_URL_SERVER =
        "EXCEL_TABLE_HEADER_DB_CONNECTION_URL_SERVER";
    public static final String DB_CONNECTION_URL_PORT = "EXCEL_TABLE_HEADER_DB_CONNECTION_URL_PORT";
    public static final String DB_CONNECTION_URL_DATABASE_NAME =
        "EXCEL_TABLE_HEADER_DB_CONNECTION_URL_DATABASE_NAME";
    public static final String DB_CONNECTION_URL_SCHEMA_NAME =
        "EXCEL_TABLE_HEADER_DB_CONNECTION_URL_SCHEMA_NAME";
    public static final String DB_USER = "EXCEL_TABLE_HEADER_DB_USER";
    public static final String DB_PASSWORD = "EXCEL_TABLE_HEADER_DB_PASSWORD";

    public static final String[] HEADER_LABELS = new String[] {DB_CONNECTION_ID, DB_DRIVER_NAME,
        DB_CONNECTION_URL_PROTOCOL, DB_CONNECTION_URL_SERVER, DB_CONNECTION_URL_PORT,
        DB_CONNECTION_URL_DATABASE_NAME, DB_CONNECTION_URL_SCHEMA_NAME, DB_USER, DB_PASSWORD};
  }

  /**
   * Contains message IDs related to HousekeepDbSettings.
   */
  public static class HousekeepDbSettings {
    public static final String TASK_ID = "EXCEL_TABLE_HEADER_TASK_ID";
    public static final String SOFT_OR_HARD_DELETE = "EXCEL_TABLE_HEADER_SOFT_OR_HARD_DELETE";
    public static final String SOFT_OR_HARD_DELETE_INTERNAL_VALUE =
        "EXCEL_TABLE_HEADER_SOFT_OR_HARD_DELETE_INTERNAL_VALUE";
    public static final String TABLE_NAME = "EXCEL_TABLE_HEADER_TABLE_NAME";
    public static final String ID_COLUMN_NAME = "EXCEL_TABLE_HEADER_ID_COLUMN_NAME";
    public static final String ID_COLUMN_LITERAL_SYMBOL =
        "EXCEL_TABLE_HEADER_ID_COLUMN_LITERAL_SYMBOL";
    public static final String EXPIRATION_CHECK_TIMESTAMP_COLUMN_NAME =
        "EXCEL_TABLE_HEADER_EXPIRATION_CHECK_TIMESTAMP_COLUMN_NAME";
    public static final String EXPIRATION_CHECK_TIMESTAMP_COLUMN_DATA_TYPE =
        "EXCEL_TABLE_HEADER_EXPIRATION_CHECK_TIMESTAMP_COLUMN_DATA_TYPE";
    public static final String EXPIRATION_CHECK_VALIDITY_DAYS =
        "EXCEL_TABLE_HEADER_EXPIRATION_CHECK_VALIDITY_DAYS";
    public static final String SOFT_DELETE_COLUMN_NAME =
        "EXCEL_TABLE_HEADER_SOFT_DELETE_COLUMN_NAME";
    public static final String SOFT_DELETE_UPDATE_TIMESTAMP_COLUMN_NAME =
        "EXCEL_TABLE_HEADER_SOFT_DELETE_UPDATE_TIMESTAMP_COLUMN_NAME";
    public static final String SOFT_DELETE_UPDATE_USER_ID_COLUMN_NAME =
        "EXCEL_TABLE_HEADER_SOFT_DELETE_UPDATE_USER_ID_COLUMN_NAME";
    public static final String SOFT_DELETE_UPDATE_USER_ID_COLUMN_LITERAL_SYMBOL =
        "EXCEL_TABLE_HEADER_SOFT_DELETE_UPDATE_USER_ID_COLUMN_LITERAL_SYMBOL";
    public static final String SOFT_DELETE_UPDATE_USER_ID_COLUMN_VALUE =
        "EXCEL_TABLE_HEADER_SOFT_DELETE_UPDATE_USER_ID_COLUMN_VALUE";
    public static final String VALUE_SOFT_DELETE = "EXCEL_VALUE_SOFT_DELETE";
    public static final String VALUE_HARD_DELETE = "EXCEL_VALUE_HARD_DELETE";

    public static final String[] HEADER_LABELS = new String[] {TASK_ID,
        DbConnectionSettings.DB_CONNECTION_ID, SOFT_OR_HARD_DELETE,
        SOFT_OR_HARD_DELETE_INTERNAL_VALUE, TABLE_NAME, ID_COLUMN_NAME, ID_COLUMN_LITERAL_SYMBOL,
        EXPIRATION_CHECK_TIMESTAMP_COLUMN_NAME, EXPIRATION_CHECK_TIMESTAMP_COLUMN_DATA_TYPE,
        EXPIRATION_CHECK_VALIDITY_DAYS, SOFT_DELETE_COLUMN_NAME,
        SOFT_DELETE_UPDATE_TIMESTAMP_COLUMN_NAME, SOFT_DELETE_UPDATE_USER_ID_COLUMN_NAME,
        SOFT_DELETE_UPDATE_USER_ID_COLUMN_LITERAL_SYMBOL, SOFT_DELETE_UPDATE_USER_ID_COLUMN_VALUE};
  }

  /**
   * Contains message IDs related to RelatedTableSettings.
   */
  public static class RelatedTableSettings {
    public static final String RELATED_TABLE_PROCESS_PATTERN =
        "EXCEL_TABLE_HEADER_RELATED_TABLE_PROCESS_PATTERN";
    public static final String RELATED_TABLE_PROCESS_PATTERN_INTERNAL_VALUE =
        "EXCEL_TABLE_HEADER_RELATED_TABLE_PROCESS_PATTERN_INTERNAL_VALUE";
    public static final String TARGET_TABLE_COLUMN_NAME =
        "EXCEL_TABLE_HEADER_TARGET_TABLE_COLUMN_NAME";
    public static final String RELATED_TABLE_NAME = "EXCEL_TABLE_HEADER_RELATED_TABLE_NAME";
    public static final String RELATED_TABLE_ID_COLUMN_NAME =
        "EXCEL_TABLE_HEADER_RELATED_TABLE_ID_COLUMN_NAME";
    public static final String RELATED_TABLE_ID_COLUMN_LITERAL_SYMBOL =
        "EXCEL_TABLE_HEADER_RELATED_TABLE_ID_COLUMN_LITERAL_SYMBOL";
    public static final String VALUE_RELATED_TABLE_PROCESS_PATTERN_DELETE =
        "EXCEL_VALUE_RELATED_TABLE_PROCESS_PATTERN_DELETE";
    public static final String VALUE_RELATED_TABLE_PROCESS_PATTERN_CHECK_AND_SKIP_DELETE =
        "EXCEL_VALUE_RELATED_TABLE_PROCESS_PATTERN_CHECK_AND_SKIP_DELETE";

    public static final String[] HEADER_LABELS = new String[] {HousekeepDbSettings.TASK_ID,
        HousekeepDbSettings.SOFT_OR_HARD_DELETE_INTERNAL_VALUE, RELATED_TABLE_PROCESS_PATTERN,
        RELATED_TABLE_PROCESS_PATTERN_INTERNAL_VALUE, TARGET_TABLE_COLUMN_NAME, RELATED_TABLE_NAME,
        RELATED_TABLE_ID_COLUMN_NAME, RELATED_TABLE_ID_COLUMN_LITERAL_SYMBOL,
        HousekeepDbSettings.SOFT_DELETE_COLUMN_NAME,
        HousekeepDbSettings.SOFT_DELETE_UPDATE_TIMESTAMP_COLUMN_NAME,
        HousekeepDbSettings.SOFT_DELETE_UPDATE_USER_ID_COLUMN_NAME,
        HousekeepDbSettings.SOFT_DELETE_UPDATE_USER_ID_COLUMN_LITERAL_SYMBOL,
        HousekeepDbSettings.SOFT_DELETE_UPDATE_USER_ID_COLUMN_VALUE};
  }

  /**
   * Contains message IDs related to SearchConditionSettings.
   */
  public static class SearchConditionSettings {
    public static final String CONDITION_COLUMN_NAME = "EXCEL_TABLE_HEADER_CONDITION_COLUMN_NAME";
    public static final String CONDITION_COLUMN_LITERAL_SYMBOL =
        "EXCEL_TABLE_HEADER_CONDITION_COLUMN_LITERAL_SYMBOL";
    public static final String CONDITION_COLUMN_VALUE = "EXCEL_TABLE_HEADER_CONDITION_COLUMN_VALUE";

    public static final String[] HEADER_LABELS = new String[] {HousekeepDbSettings.TASK_ID,
        CONDITION_COLUMN_NAME, CONDITION_COLUMN_LITERAL_SYMBOL, CONDITION_COLUMN_VALUE};
  }
}

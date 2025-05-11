package jp.ecuacion.tool.housekeepdb.bean.forexceltable;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.List;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.tool.housekeepdb.lang.LangExcel;
import jp.ecuacion.util.poi.excel.table.bean.StringExcelTableBean;

/**
 * 
 */
public class DbConnectionInfoBean extends StringExcelTableBean {

  @NotEmpty
  private String id;
  @NotEmpty
  private String driverName;
  @NotEmpty
  private String protocol;
  @NotEmpty
  private String server;
  @NotEmpty
  private String port;
  @NotEmpty
  private String database;
  private String schema;
  @NotEmpty
  private String username;
  @NotEmpty
  private String password;

  public static final String[] HEADER_LABEL_KEYS = LangExcel.DbConnectionSettings.HEADER_LABELS;

  @Override
  protected @Nonnull String[] getFieldNameArray() {
    return new String[] {"id", "driverName", "protocol", "server", "port", "database", "schema",
        "username", "password"};
  }

  /** Used for unit test only. */
  public DbConnectionInfoBean(String id, String driverName, String protocol, String server,
      String port, String database, String schema, String username, String password) {
    super(Arrays.asList(new String[] {id, driverName, protocol, server, port, database, schema,
        username, password}));
  }

  /**  */
  public DbConnectionInfoBean(List<String> colList) {
    super(colList);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDriverName() {
    return driverName;
  }

  public void setDriverName(String driverName) {
    this.driverName = driverName;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public void afterReading() throws AppException {

  }
}

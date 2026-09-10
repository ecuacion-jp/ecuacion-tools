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
package jp.ecuacion.tool.housekeepdb.bean.forexceltable;

import jakarta.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import jp.ecuacion.lib.core.util.EmbeddedVariableUtil;
import jp.ecuacion.lib.validation.constraints.PatternWithDescription;
import jp.ecuacion.tool.housekeepdb.util.LangExcelUtil;
import jp.ecuacion.util.excel.table.bean.StringExcelTableBean;
import org.jspecify.annotations.Nullable;

/**
 * Stores database connection settings.
 */
@SuppressWarnings("NullAway.Init")
public class DbConnectionInfoBean extends StringExcelTableBean {

  @NotEmpty
  private String id;
  @NotEmpty
  private String driverName;
  // SqlUtil branches on this value with an exact-match check, throwing for anything else.
  @NotEmpty
  @PatternWithDescription(regexp = "^(postgresql|mysql)$",
      description = "\"postgresql\" or \"mysql\"")
  private String protocol;
  @NotEmpty
  private String server;
  // Embedded as-is into the JDBC connection URL.
  @NotEmpty
  @PatternWithDescription(regexp = "^[0-9]+$", description = "digits only")
  private String port;
  @NotEmpty
  private String database;
  private String schema;
  @NotEmpty
  private String username;
  // No @Size here: a password's length is dictated by the DB server, not by this tool, and any
  // length cap would risk surfacing the value itself via a validation error message.
  @NotEmpty
  private String password;

  // Fields not in the Excel sheet.

  private @Nullable String envVarExpandedPassword;

  private Function<String, String> envVarValueGetter;

  public static final String[] HEADER_LABEL_KEYS = LangExcelUtil.DbConnectionSettings.HEADER_LABELS;

  @Override
  protected @Nullable String[] getFieldNameArray() {
    return new String[] {"id", "driverName", "protocol", "server", "port", "database", "schema",
        "username", "password"};
  }

  /** Used for unit test only. */
  @SuppressWarnings("null")
  public DbConnectionInfoBean(String id, String driverName, String protocol, String server,
      String port, String database, String schema, String username, String password) {
    super(Arrays.asList(new String[] {id, driverName, protocol, server, port, database, schema,
        username, password}));
  }

  /** 
   * Constructs a new instance.
   */
  @SuppressWarnings("null")
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

  /**
   * Returns the password, with any {@code ${VAR}} reference it contains already expanded via
   * {@link #setEnvVarValueGetter}, if that was called; otherwise returns the raw Excel value
   * unchanged (e.g. when this bean was built directly in a test without going through
   * {@link jp.ecuacion.tool.housekeepdb.tasklet.HousekeepDbTasklet#execute}).
   *
   * <p>This lets an operator keep the actual secret out of the settings Excel file entirely by
   * writing e.g. {@code ${DB_PASSWORD}} and defining {@code DB_PASSWORD} as an OS environment
   * variable / JVM system property / {@code application.properties} entry instead.</p>
   */
  public String getPassword() {
    String expanded = envVarExpandedPassword;
    if (expanded != null) {
      return expanded;
    }

    return password;
  }

  public void setPassword(String password) {
    this.password = password;
    // Clear any previously-computed expansion: it was computed from the old value and no longer
    // applies. It is recomputed lazily by getPassword() falling back to the raw value here; a
    // caller that also needs the ${VAR} expansion of the new value must call
    // setEnvVarValueGetter() again.
    envVarExpandedPassword = null;
  }

  /**
   * Sets the ${VAR} value resolver and eagerly expands any {@code ${VAR}} reference in
   * {@code password}, throwing (via EmbeddedVariableUtil.VariableNotFoundException, wrapped in
   * RuntimeException) if a referenced variable cannot be resolved.
   */
  public void setEnvVarValueGetter(Function<String, String> envVarValueGetter) {
    this.envVarValueGetter = envVarValueGetter == null ? key -> null : envVarValueGetter;
    envVarExpandedPassword = password == null ? null : substituteEnvVars(password);
  }

  private String substituteEnvVars(String value) {
    try {
      return EmbeddedVariableUtil.getVariableReplacedString(value, "${", "}", envVarValueGetter);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void afterReading() {

  }
}

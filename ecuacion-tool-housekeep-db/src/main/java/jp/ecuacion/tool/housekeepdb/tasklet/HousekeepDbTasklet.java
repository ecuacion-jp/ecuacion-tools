package jp.ecuacion.tool.housekeepdb.tasklet;

import static jp.ecuacion.tool.housekeepdb.bean.forexceltable.RelatedTableInfoBean.RelatedTableProcessPatternEnum.deleteRelatedTableRecord;
import static jp.ecuacion.tool.housekeepdb.bean.forexceltable.RelatedTableInfoBean.RelatedTableProcessPatternEnum.skipTargetTableRecordDeletion;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jp.ecuacion.lib.core.exception.checked.AppException;
import jp.ecuacion.lib.core.exception.checked.BizLogicAppException;
import jp.ecuacion.lib.core.exception.checked.MultipleAppException;
import jp.ecuacion.lib.core.exception.unchecked.UncheckedAppException;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.lib.core.util.ValidationUtil;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueStringBean;
import jp.ecuacion.tool.housekeepdb.bean.SqlConditionInterface;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.DbConnectionInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.RelatedTableInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.WhereConditionInfoBean;
import jp.ecuacion.tool.housekeepdb.lang.LangExcel;
import jp.ecuacion.tool.housekeepdb.util.SqlUtil;
import jp.ecuacion.util.poi.excel.table.reader.concrete.StringOneLineHeaderExcelTableReader;
import jp.ecuacion.util.poi.excel.table.reader.concrete.StringOneLineHeaderExcelTableToBeanReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.slf4j.event.Level;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Executes housekeeping DB.
 */
@Component
public class HousekeepDbTasklet implements Tasklet {

  private static final int MAX_SELECT_LINES = 1000;
  private DetailLogger detailLogger = new DetailLogger(this);
  private LangExcel lang = null;

  /**
   * Executes the procedure.
   */
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {

    String excelPath = getExcelPathFromParameter(chunkContext);

    final Map<String, String> infoMap = getInfoMap(excelPath);

    lang = new LangExcel(Locale.of(infoMap.get("locale")));

    final Map<String, DbConnectionInfoBean> dbConnectionInfoMap = getDbConnectionInfoMap(excelPath);
    final List<HousekeepInfoBean> housekeepInfoList =
        getHousekeepInfoList(excelPath, dbConnectionInfoMap);

    detailLogger.info("Format Excel Version: " + infoMap.get("format-version"));
    detailLogger.info("Locale              : " + infoMap.get("locale"));
    detailLogger.info("database            : " + infoMap.get("database"));
    detailLogger
        .info("SQLs for per-record soft / hard delete will be logged with \"debug\" loglevel "
            + "because of the amount.");

    for (HousekeepInfoBean info : housekeepInfoList) {
      detailLogger.info("[task start ] " + info.getTaskId());
      detailLogger.info("DB Connection ID: " + info.getDbConnectionInfoId() + " / "
          + (info.isSoftDelete() ? "Soft Delete" : "Hard Delete") + " / " + "Table Name: "
          + info.getTable() + ")");

      Map<String, Integer> tableRecordDeleted = new LinkedHashMap<>();

      // DB Connection settings
      try (Connection conn = connectionSettings(dbConnectionInfoMap, info)) {

        // GetMAX_SELECT_LINES 行分、idを取得
        String selectSql = getMainSelectSql(info);

        // 大量件数がある場合でもMAX_SELECT_LINES件で区切って処理
        while (true) {
          String msg = "The following procedure is Looped and committed every " + MAX_SELECT_LINES
              + " lines to prevent from using too much memory and time.";
          detailLogger.info(msg);

          try (PreparedStatement stmt = getStatement(conn, selectSql)) {
            ResultSet rs = stmt.executeQuery();

            // 検索結果が1件以上あったかどうかを判別するフラグ
            boolean isResultZero = true;

            // 取得したレコード1件ごとに処理
            while (rs.next()) {
              Object idValue = rs.getObject(info.getIdColumnInfo().getColumn());

              // skipすべきデータが存在する場合はskipなのでそのためのチェック
              if (needsSkipFromRelatedTableDataCheck(conn, info, idValue, rs)) {
                continue;
              }

              isResultZero = false;

              deleteRelatedData(conn, info, idValue, tableRecordDeleted);
              deleteTargetData(conn, info, idValue, tableRecordDeleted);
            }

            // resultSetが0件の場合は終了
            if (isResultZero) {
              break;
            }

            conn.commit();
          }
        }
      }

      tableRecordDeleted.keySet().stream().forEach(table -> detailLogger
          .info("[Delete lines] table:" + table + ", count:" + tableRecordDeleted.get(table)));

      detailLogger.info("[task finish] " + info.getTaskId());
    }

    return RepeatStatus.FINISHED;
  }

  private String getExcelPathFromParameter(ChunkContext chunkContext) throws BizLogicAppException {
    Map<String, Object> paramMap = chunkContext.getStepContext().getJobParameters();

    String excelPath = (String) paramMap.get("excelPath");

    if (excelPath == null) {
      throw new BizLogicAppException("MSG_ERR_EXCEL_PATH_NOT_SPECIFIED");
    }

    File excelFile = new File(excelPath);
    if (!excelFile.exists() || !excelFile.isFile()) {
      throw new BizLogicAppException("MSG_ERR_EXCEL_PATH_NOT_FOUND");
    }

    return excelPath;
  }

  private String getMainSelectSql(HousekeepInfoBean info) {
    // where句の作成
    List<SqlConditionInterface> whereList = new ArrayList<>();

    whereList.addAll(
        info.getWhereConditionInfoList().stream().map(e -> e.getConditionColumnInfo()).toList());

    if (info.timestampColumnDefines()) {
      whereList.add(new ColumnAndValueStringBean(
          "'" + SqlUtil.getTimestampNow(info.getDbConnectionInfo().getProtocol()) + "' - "
              + info.getTimestampColumn() + " > '" + info.getDeleteTargetInDays() + " days'"));
    }

    if (info.isSoftDelete()) {
      // 何度も更新しないよう、論理廃止フラグが立っていないもののみを検索対象とする
      whereList.add(new ColumnAndValueInfoBean(info.getSoftDeleteColumn(), false, "false"));

    } else {
      // 削除でかつ「論理廃止：カラム名」が指定されている場合はwhere句に追加
      if (StringUtils.isNotEmpty(info.getSoftDeleteColumn())) {
        whereList.add(new ColumnAndValueInfoBean(info.getSoftDeleteColumn(), false, "true"));
      }
    }

    String where = SqlUtil.getWhere(whereList);

    return "select * from " + info.getTable() + where + " order by "
        + info.getIdColumnInfo().getColumn() + " limit " + MAX_SELECT_LINES;
  }

  private Connection connectionSettings(Map<String, DbConnectionInfoBean> dbConnectionInfoMap,
      HousekeepInfoBean info) throws BizLogicAppException, ClassNotFoundException, SQLException {
    DbConnectionInfoBean dbInfo = dbConnectionInfoMap.get(info.getDbConnectionInfoId());
    if (dbInfo == null) {
      throw new BizLogicAppException("MSG_ERR_DB_CONNECITON_INFO_ID_NOT_EXIST",
          info.getDbConnectionInfoId());
    }

    Class.forName(dbInfo.getDriverName());
    Connection conn = DriverManager.getConnection(getDbConnectionUrl(dbInfo), dbInfo.getUsername(),
        dbInfo.getPassword());
    conn.setAutoCommit(false);
    return conn;
  }

  private PreparedStatement getStatement(Connection conn, String sql) throws SQLException {
    return getStatement(conn, sql, Level.INFO);
  }

  private PreparedStatement getStatement(Connection conn, String sql, Level logLevel)
      throws SQLException {

    if (logLevel != null) {
      detailLogger.log(logLevel, sql);
    }

    return conn.prepareStatement(sql);
  }

  /**
   * Skip deleting if specified related-table record exists.
   * 
   * <p>Returning true means that record is skipped to delete.</p>
   */
  private boolean needsSkipFromRelatedTableDataCheck(Connection connection, HousekeepInfoBean info,
      Object id, ResultSet mainSqlRs) throws SQLException {
    List<RelatedTableInfoBean> relatedSkipList = info.getRelatedRecordTableInfoList().stream()
        .filter(bean -> bean.getRelatedTableProcessPattern() == skipTargetTableRecordDeletion)
        .toList();

    for (RelatedTableInfoBean relatedBean : relatedSkipList) {
      Object value = mainSqlRs.getObject(relatedBean.getTargetTableColumn());

      String selectSql = "select count(*) count from " + relatedBean.getRelatedTable() + " where "
          + relatedBean.getRelatedTableIdColumnInfo().getColumnAndValueInfo(value).getCondition();

      PreparedStatement stmt = getStatement(connection, selectSql, Level.DEBUG);
      ResultSet rs = stmt.executeQuery();

      rs.next();
      Integer integer = rs.getInt("count");
      if (integer > 0) {
        return true;
      }
    }

    return false;
  }

  private void deleteRelatedData(Connection conn, HousekeepInfoBean info, Object id,
      Map<String, Integer> tableRecordDeleted) throws SQLException {
    List<RelatedTableInfoBean> list = info.getRelatedRecordTableInfoList().stream()
        .filter(bean -> bean.getRelatedTableProcessPattern() == deleteRelatedTableRecord).toList();

    for (RelatedTableInfoBean relatedInfo : list) {
      if (!tableRecordDeleted.containsKey(relatedInfo.getRelatedTable())) {
        tableRecordDeleted.put(relatedInfo.getRelatedTable(), 0);
      }

      // Organize a delete (or update in case of soft delete) statement of a record linked to the id
      // of the target table.

      // Put parameters of the set clause in a update statement
      List<SqlConditionInterface> updateSetList = new ArrayList<>();

      if (info.isSoftDelete()) {
        // '<softDeleteColumn> = true'
        updateSetList.add(relatedInfo.getSoftDeleteColumnInfo().getColumnAndValueInfo("true"));

        // '<SoftDeleteUpdateTimestampColumn> = now()'
        if (!StringUtils.isEmpty(relatedInfo.getSoftDeleteUpdateTimestampColumn())) {
          updateSetList.add(relatedInfo.getSoftDeleteUpdateTimestampColumnInfo()
              .getTimestampColumnNowInfo(info.getDbConnectionInfo().getProtocol()));
        }

        // <SoftDeleteUpdateUserIdColumn = 'xxx'
        if (!StringUtils.isEmpty(relatedInfo.getSoftDeleteUpdateUserIdColumn())) {
          updateSetList.add(relatedInfo.getSoftDeleteUpdateUserIdColumnAndValueInfo());
        }
      }

      // まずtargetTableから対象カラムの値を取得
      String sqlTargetSelect =
          "select " + relatedInfo.getTargetTableColumn() + " from " + info.getTable() + " where "
              + info.getIdColumnInfo().getColumnAndValueInfo(id).getCondition();

      try (PreparedStatement stmt = getStatement(conn, sqlTargetSelect, Level.DEBUG);
          ResultSet rs = stmt.executeQuery();) {

        // number of records is always one because 'id' is specified to the where clause.
        rs.next();

        // where clause
        final Object val = rs.getObject(relatedInfo.getTargetTableColumn());
        List<SqlConditionInterface> whereList = new ArrayList<>();
        whereList.add(relatedInfo.getRelatedTableIdColumnInfo().getColumnAndValueInfo(val));

        // 物理削除で論理廃止カラムが指定されている場合は、そのカラムがtrueであることもwhere句に追加
        if (!info.isSoftDelete() && !StringUtils.isEmpty(relatedInfo.getSoftDeleteColumn())) {
          whereList.add(relatedInfo.getSoftDeleteColumnInfo().getColumnAndValueInfo("true"));
        }

        // related tableのcolumnに取得した値があるレコードは削除
        String softDeleteSql =
            "update " + relatedInfo.getRelatedTable() + SqlUtil.getUpdateSet(updateSetList);
        String hardDeleteSql = "delete from " + relatedInfo.getRelatedTable();

        String sql = info.isSoftDelete() ? softDeleteSql : hardDeleteSql;
        sql = sql + SqlUtil.getWhere(whereList);

        PreparedStatement delStmt = getStatement(conn, sql, Level.DEBUG);
        int count = delStmt.executeUpdate();
        tableRecordDeleted.put(relatedInfo.getRelatedTable(),
            tableRecordDeleted.get(relatedInfo.getRelatedTable()) + count);

        delStmt.close();

        logDeleteLines(relatedInfo.getRelatedTable(), count,
            relatedInfo.getRelatedTableIdColumnInfo().getColumnAndValueInfo(val).getCondition(),
            Level.DEBUG);
      }
    }
  }

  private void deleteTargetData(Connection conn, HousekeepInfoBean info, Object idValue,
      Map<String, Integer> tableRecordDeleted) throws SQLException {

    List<SqlConditionInterface> updateSetList = new ArrayList<>();
    if (info.isSoftDelete()) {
      updateSetList.add(info.getSoftDeleteColumnInfo().getColumnAndValueInfo("true"));

      if (!StringUtils.isEmpty(info.getSoftDeleteUpdateTimestampColumn())) {
        updateSetList.add(info.getSoftDeleteUpdateTimestampColumnInfo()
            .getTimestampColumnNowInfo(info.getDbConnectionInfo().getProtocol()));
      }

      if (!StringUtils.isEmpty(info.getSoftDeleteUpdateUserIdColumn())) {
        updateSetList.add(info.getSoftDeleteUpdateUserIdColumnAndValueInfo());
      }
    }

    String softDeleteSql = "update " + info.getTable() + SqlUtil.getUpdateSet(updateSetList);
    String hardDeleteSql = "delete from " + info.getTable();

    List<SqlConditionInterface> whereList = new ArrayList<>();
    whereList.add(info.getIdColumnInfo().getColumnAndValueInfo(idValue));

    // 物理削除で論理廃止カラムが指定されている場合は、そのカラムがtrueであることも条件に追加
    if (!info.isSoftDelete() && !StringUtils.isEmpty(info.getSoftDeleteColumn())) {
      whereList.add(info.getSoftDeleteColumnInfo().getColumnAndValueInfo("true"));
    }

    String sql = info.isSoftDelete() ? softDeleteSql : hardDeleteSql;
    sql = sql + SqlUtil.getWhere(whereList);

    PreparedStatement delStmt = getStatement(conn, sql, Level.DEBUG);
    int count = delStmt.executeUpdate();

    if (count > 0 && !tableRecordDeleted.containsKey(info.getTable())) {
      tableRecordDeleted.put(info.getTable(), 0);
    }
    tableRecordDeleted.put(info.getTable(), tableRecordDeleted.get(info.getTable()) + count);

    delStmt.close();

    logDeleteLines(info.getTable(), count,
        info.getIdColumnInfo().getColumnAndValueInfo(idValue).getCondition(), Level.DEBUG);
  }

  private void logDeleteLines(String table, int count, String condition, Level logLevel) {
    if (logLevel != null) {
      detailLogger.log(logLevel, table + ": " + count + " lines deleted. (" + condition + ")");
    }
  }

  private String getDbConnectionUrl(DbConnectionInfoBean dbInfo) {
    String param =
        StringUtils.isEmpty(dbInfo.getSchema()) ? "" : "?currentSchema=" + dbInfo.getSchema();
    return "jdbc:" + dbInfo.getProtocol() + "://" + dbInfo.getServer() + ":" + dbInfo.getPort()
        + "/" + dbInfo.getDatabase() + param;
  }

  private Map<String, String> getInfoMap(String filePath)
      throws EncryptedDocumentException, AppException, IOException {
    List<List<String>> list = new StringOneLineHeaderExcelTableReader("Info",
        new String[] {"item", "value"}, null, 1, null).read(filePath);

    return list.stream().collect(Collectors.toMap(l -> l.get(0), l -> l.get(1)));
  }

  private Map<String, DbConnectionInfoBean> getDbConnectionInfoMap(String filePath)
      throws EncryptedDocumentException, URISyntaxException, IOException, AppException {

    Map<String, DbConnectionInfoBean> dbConnectionInfoMap =
        new StringOneLineHeaderExcelTableToBeanReader<DbConnectionInfoBean>(
            DbConnectionInfoBean.class, lang.get(LangExcel.DB_CONNECTION_SETTINGS),
            lang.getHeaderLabels(DbConnectionInfoBean.HEADER_LABEL_KEYS), null, 1, null)
                .readToBean(filePath).stream().collect(Collectors.toMap(e -> e.getId(), e -> e));

    dbConnectionInfoMap.values().stream().forEach(info -> {
      try {
        ValidationUtil.validateThenThrow(info);

      } catch (MultipleAppException e) {
        throw new UncheckedAppException(e);
      }
    });

    return dbConnectionInfoMap;
  }

  private List<HousekeepInfoBean> getHousekeepInfoList(String filePath,
      Map<String, DbConnectionInfoBean> dbConnectionMap)
      throws EncryptedDocumentException, URISyntaxException, IOException, AppException {
    List<HousekeepInfoBean> housekeepList =
        new StringOneLineHeaderExcelTableToBeanReader<HousekeepInfoBean>(HousekeepInfoBean.class,
            lang.get(LangExcel.HOUSEKEEP_DB_SETTINGS),
            lang.getHeaderLabels(HousekeepInfoBean.HEADER_LABEL_KEYS), null, 1, null)
                .readToBean(filePath);
    List<WhereConditionInfoBean> whereConditionList =
        new StringOneLineHeaderExcelTableToBeanReader<WhereConditionInfoBean>(
            WhereConditionInfoBean.class, lang.get(LangExcel.SEARCH_CONDITION_SETTINGS),
            lang.getHeaderLabels(WhereConditionInfoBean.HEADER_LABEL_KEYS), null, 1, null)
                .readToBean(filePath);
    List<RelatedTableInfoBean> relatedTableList =
        new StringOneLineHeaderExcelTableToBeanReader<RelatedTableInfoBean>(
            RelatedTableInfoBean.class, lang.get(LangExcel.RELATED_TABLE_SETTINGS),
            lang.getHeaderLabels(RelatedTableInfoBean.HEADER_LABEL_KEYS), null, 1, null)
                .readToBean(filePath);

    // task IDの重複を検知するためのset
    Set<String> housekeepInfoTaskIdSet = new HashSet<>();
    for (HousekeepInfoBean hpBean : housekeepList) {
      // task ID重複チェック
      if (housekeepInfoTaskIdSet.contains(hpBean.getTaskId())) {
        throw new BizLogicAppException("MSG_ERR_TASK_ID_DUPLICATED", hpBean.getTaskId());
      }

      housekeepInfoTaskIdSet.add(hpBean.getTaskId());

      // DB Connectionは必須なのでない場合はエラー
      if (!dbConnectionMap.containsKey(hpBean.getDbConnectionInfoId())) {
        throw new BizLogicAppException("MSG_ERR_DB_CONN_ID_NOT_FOUND", hpBean.getTaskId(),
            hpBean.getDbConnectionInfoId());
      }

      hpBean.setDbConnectionInfo(dbConnectionMap.get(hpBean.getDbConnectionInfoId()));

      hpBean.setWhereConditionInfoList(whereConditionList.stream()
          .filter(bean -> bean.getTaskId().equals(hpBean.getTaskId())).toList());

      hpBean.setRelatedRecordTableInfoList(relatedTableList.stream()
          .filter(bean -> bean.getTaskId().equals(hpBean.getTaskId())).toList());
    }

    // 「関連テーブル処理設定」、「データ検索条件設定」未使用のデータがないかを確認。
    // あれば、task IDのずれにより想定通り設定ができていない可能性があるのでエラーとする。
    // 「DB接続設定」は、1 taskに対して1つのみで、かつ必須にしているので、使用されていないものがあっても大きな問題とは思いにくいことから、未使用があっても問題なしとする。
    Set<RelatedTableInfoBean> relSet = new HashSet<>();
    housekeepList.stream().forEach(bean -> relSet.addAll(bean.getRelatedRecordTableInfoList()));
    for (RelatedTableInfoBean relBean : relatedTableList) {
      // 一致するか否かを判断するkeyがないので、objectとしての同一性で比較
      if (!relSet.contains(relBean)) {
        throw new BizLogicAppException("MSG_ERR_DATA_NOT_USED_REL", relBean.getTaskId(),
            lang.get(relBean.getRelatedTableProcessPatternStringKey()),
            relBean.getTargetTableColumn(), relBean.getRelatedTable());
      }
    }

    Set<WhereConditionInfoBean> condSet = new HashSet<>();
    housekeepList.stream().forEach(bean -> condSet.addAll(bean.getWhereConditionInfoList()));
    for (WhereConditionInfoBean condBean : whereConditionList) {
      // 一致するか否かを判断するkeyがないので、objectとしての同一性で比較
      if (!condSet.contains(condBean)) {
        throw new BizLogicAppException("MSG_ERR_DATA_NOT_USED_COND", condBean.getTaskId(),
            condBean.getConditionColumn());
      }
    }

    return housekeepList;
  }
}

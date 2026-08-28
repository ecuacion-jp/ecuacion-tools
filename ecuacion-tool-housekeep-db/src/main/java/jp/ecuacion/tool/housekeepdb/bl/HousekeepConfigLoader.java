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
package jp.ecuacion.tool.housekeepdb.bl;

import jakarta.validation.Validation;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jp.ecuacion.lib.core.violation.BusinessViolation;
import jp.ecuacion.lib.core.violation.Violations;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.DbConnectionInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.RelatedTableInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.WhereConditionInfoBean;
import jp.ecuacion.tool.housekeepdb.util.LangExcelUtil;
import jp.ecuacion.util.excel.table.reader.concrete.StringOneLineHeaderExcelTableReader;
import jp.ecuacion.util.excel.table.reader.concrete.StringOneLineHeaderExcelTableToBeanReader;
import org.jspecify.annotations.Nullable;

/**
 * Reads the housekeep-db settings excel file and links its rows across sheets into beans, ready
 * for {@link HousekeepRecordDeleter} to execute.
 */
public class HousekeepConfigLoader {

  /**
   * Marks constraints that depend on data only available after this loader links a bean read
   * from one Excel sheet to its counterpart on another sheet (e.g. a {@code RelatedTableInfoBean}
   * to its {@code HousekeepInfoBean} by task ID) - such data isn't there yet when beans are
   * validated immediately at Excel-read time, so those constraints are deferred to this group and
   * validated explicitly once the linking is done. Lives here, not on any one bean class, since
   * more than one bean may need it.
   */
  public interface AfterMergeValidation {
  }

  private Map<String, String> infoMap = Map.of();
  private @Nullable LangExcelUtil lang;
  private Map<String, DbConnectionInfoBean> dbConnectionInfoMap = Map.of();
  private List<HousekeepInfoBean> housekeepInfoList = List.of();

  /**
   * Reads {@code excelPath} and populates the info map, db connection map and housekeep task
   * list, linking related rows across sheets by task ID.
   *
   * @param excelPath the excel file path, already validated to exist and be openable
   */
  public void load(String excelPath) throws Exception {
    infoMap = readInfoMap(excelPath);
    lang = new LangExcelUtil(Locale.of(infoMap.get("locale")));
    dbConnectionInfoMap = readDbConnectionInfoMap(excelPath);
    housekeepInfoList = readHousekeepInfoList(excelPath, dbConnectionInfoMap);
  }

  public Map<String, String> getInfoMap() {
    return infoMap;
  }

  public Map<String, DbConnectionInfoBean> getDbConnectionInfoMap() {
    return dbConnectionInfoMap;
  }

  public List<HousekeepInfoBean> getHousekeepInfoList() {
    return housekeepInfoList;
  }

  private Map<String, String> readInfoMap(String filePath) throws Exception {
    List<List<String>> list =
        new StringOneLineHeaderExcelTableReader("Info", new String[] {"item", "value"})
            .read(filePath);

    return list.stream().collect(Collectors.toMap(l -> l.get(0), l -> l.get(1)));
  }

  @SuppressWarnings("null")
  private Map<String, DbConnectionInfoBean> readDbConnectionInfoMap(String filePath)
      throws Exception {

    LangExcelUtil langLocal = Objects.requireNonNull(lang);
    Map<String, DbConnectionInfoBean> map =
        new StringOneLineHeaderExcelTableToBeanReader<DbConnectionInfoBean>(
            DbConnectionInfoBean.class, langLocal.get(LangExcelUtil.DB_CONNECTION_SETTINGS),
            langLocal.getHeaderLabels(DbConnectionInfoBean.HEADER_LABEL_KEYS)).readToBean(filePath)
                .stream().collect(Collectors.toMap(e -> e.getId(), e -> e));

    map.values().stream().forEach(info -> {
      new Violations()
          .addAll(Validation.buildDefaultValidatorFactory().getValidator().validate(info))
          .throwIfAny();
    });

    return map;
  }

  private List<HousekeepInfoBean> readHousekeepInfoList(String filePath,
      Map<String, DbConnectionInfoBean> dbConnectionMap) throws Exception {
    LangExcelUtil langLocal = Objects.requireNonNull(lang);
    List<HousekeepInfoBean> housekeepList =
        new StringOneLineHeaderExcelTableToBeanReader<HousekeepInfoBean>(HousekeepInfoBean.class,
            langLocal.get(LangExcelUtil.HOUSEKEEP_DB_SETTINGS),
            langLocal.getHeaderLabels(HousekeepInfoBean.HEADER_LABEL_KEYS)).readToBean(filePath,
                true);
    List<WhereConditionInfoBean> whereConditionList =
        new StringOneLineHeaderExcelTableToBeanReader<WhereConditionInfoBean>(
            WhereConditionInfoBean.class, langLocal.get(LangExcelUtil.SEARCH_CONDITION_SETTINGS),
            langLocal.getHeaderLabels(WhereConditionInfoBean.HEADER_LABEL_KEYS))
                .readToBean(filePath, true);
    List<RelatedTableInfoBean> relatedTableList =
        new StringOneLineHeaderExcelTableToBeanReader<RelatedTableInfoBean>(
            RelatedTableInfoBean.class, langLocal.get(LangExcelUtil.RELATED_TABLE_SETTINGS),
            langLocal.getHeaderLabels(RelatedTableInfoBean.HEADER_LABEL_KEYS)).readToBean(filePath,
                true);

    // Set for detecting duplicate task IDs.
    Set<String> housekeepInfoTaskIdSet = new HashSet<>();
    for (HousekeepInfoBean hpBean : housekeepList) {
      // Check for duplicate task IDs.
      if (housekeepInfoTaskIdSet.contains(hpBean.getTaskId())) {
        new Violations()
            .add(new BusinessViolation("MSG_ERR_TASK_ID_DUPLICATED", hpBean.getTaskId()))
            .throwIfAny();
      }

      housekeepInfoTaskIdSet.add(hpBean.getTaskId());

      // DB Connection is required; error if not found.
      if (!dbConnectionMap.containsKey(hpBean.getDbConnectionInfoId())) {
        new Violations().add(new BusinessViolation("MSG_ERR_DB_CONN_ID_NOT_FOUND",
            hpBean.getTaskId(), hpBean.getDbConnectionInfoId())).throwIfAny();
      }

      hpBean.setDbConnectionInfo(
          Objects.requireNonNull(dbConnectionMap.get(hpBean.getDbConnectionInfoId())));

      hpBean.setWhereConditionInfoList(whereConditionList.stream()
          .filter(bean -> bean.getTaskId().equals(hpBean.getTaskId())).toList());

      hpBean.setRelatedRecordTableInfoList(relatedTableList.stream()
          .filter(bean -> bean.getTaskId().equals(hpBean.getTaskId())).toList());

      // isSoftDeleteInternalValue on each related-table row is populated here, only now
      // available since it's copied from the linked HousekeepInfoBean rather than read from an
      // Excel column - see RelatedTableInfoBean's class Javadoc. The constraints depending on it
      // are deferred to the AfterMergeValidation group for the same reason.
      for (RelatedTableInfoBean relBean : hpBean.getRelatedRecordTableInfoList()) {
        relBean.setIsSoftDeleteInternalValue(hpBean.getIsSoftDeleteInternalValue());
        new Violations().validate(relBean, AfterMergeValidation.class).throwIfAny();
      }
    }

    // Verify there are no unused records in "Related Table Settings" and
    // "Search Condition Settings".
    // If found, a task ID mismatch may mean the configuration is not as intended, so treat as
    // an error.
    // "DB Connection Settings" is limited to one per task and is required, so unused entries
    // are unlikely to indicate a significant problem — treat as acceptable.
    Set<RelatedTableInfoBean> relSet = new HashSet<>();
    housekeepList.stream().forEach(bean -> relSet.addAll(bean.getRelatedRecordTableInfoList()));
    for (RelatedTableInfoBean relBean : relatedTableList) {
      // Since there is no key to match on, compare by object identity.
      if (!relSet.contains(relBean)) {
        new Violations().add(new BusinessViolation("MSG_ERR_DATA_NOT_USED_REL", relBean.getTaskId(),
            langLocal.get(relBean.getRelatedTableProcessPatternStringKey()),
            relBean.getTargetTableColumn(), relBean.getRelatedTable())).throwIfAny();
      }
    }

    Set<WhereConditionInfoBean> condSet = new HashSet<>();
    housekeepList.stream().forEach(bean -> condSet.addAll(bean.getWhereConditionInfoList()));
    for (WhereConditionInfoBean condBean : whereConditionList) {
      // Since there is no key to match on, compare by object identity.
      if (!condSet.contains(condBean)) {
        new Violations().add(new BusinessViolation("MSG_ERR_DATA_NOT_USED_COND",
            condBean.getTaskId(), condBean.getConditionColumn())).throwIfAny();
      }
    }

    return housekeepList;
  }
}

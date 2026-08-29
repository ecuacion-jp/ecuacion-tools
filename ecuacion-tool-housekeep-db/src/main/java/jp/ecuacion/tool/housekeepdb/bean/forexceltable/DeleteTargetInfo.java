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

import jp.ecuacion.tool.housekeepdb.bean.ColumnAndValueInfoBean;
import jp.ecuacion.tool.housekeepdb.bean.ColumnInfoBean;

/**
 * Common shape shared by {@link HousekeepInfoBean} (a housekeep target table) and
 * {@link RelatedTableInfoBean} (a related table to delete from / soft-delete in): both describe a
 * table where one record is identified by a single column's value and optionally soft-deleted via
 * a flag / update-timestamp / update-user-id column, letting
 * {@code jp.ecuacion.tool.housekeepdb.bl.RecordDeleter} delete or soft-delete a record in either
 * kind of table through the same logic.
 */
public interface DeleteTargetInfo {

  /**
   * The table to delete (or soft-delete) a record in.
   */
  String getTargetTable();

  /**
   * The column identifying the one record to delete (or soft-delete).
   */
  ColumnInfoBean getDeleteKeyColumnInfo();

  /**
   * The column holding the soft-delete flag, or empty if this target is never soft-deleted.
   */
  String getSoftDeleteColumn();

  /**
   * The column/value info for {@link #getSoftDeleteColumn()}, or {@code null} if that column is
   * empty.
   */
  ColumnInfoBean getSoftDeleteColumnInfo();

  /**
   * The column to stamp with the current time on soft delete, or empty if not configured.
   */
  String getSoftDeleteUpdateTimestampColumn();

  /**
   * The column/value info for {@link #getSoftDeleteUpdateTimestampColumn()}, or {@code null} if
   * that column is empty.
   */
  ColumnInfoBean getSoftDeleteUpdateTimestampColumnInfo();

  /**
   * The column to stamp with the deleting user's id on soft delete, or empty if not configured.
   */
  String getSoftDeleteUpdateUserIdColumn();

  /**
   * The column/value info for {@link #getSoftDeleteUpdateUserIdColumn()}, or {@code null} if
   * that column is empty.
   */
  ColumnAndValueInfoBean getSoftDeleteUpdateUserIdColumnAndValueInfo();
}

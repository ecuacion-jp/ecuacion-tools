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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jp.ecuacion.lib.core.logging.DetailLogger;
import jp.ecuacion.tool.housekeepdb.bean.forexceltable.HousekeepInfoBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link RecordDeleter#softOrHardDeleteOne}, mocking the JDBC layer so the
 * generated SQL text and bind-value order can be verified directly, without a real database.
 *
 * <p>Uses {@link HousekeepInfoBean} (one of the two {@code DeleteTargetInfo} implementations) as
 *     the target - {@link RecordDeleter} only ever depends on the {@code DeleteTargetInfo}
 *     interface, so either bean exercises the same code; {@code HousekeepInfoBean} was picked
 *     since {@link HousekeepInfoBeanTest} already establishes its column-layout / {@code bean()}
 *     helper conventions.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RecordDeleter")
class RecordDeleterTest {

  // Column order matches HousekeepInfoBeanTest's HARD_BASE / SOFT_BASE: taskId,
  // dbConnectionInfoId, isSoftDelete, isSoftDeleteInternalValue, table, idColumn,
  // idColumnNeedsQuotationMark, timestampColumn, timestampColumnKind, deleteTargetInDays,
  // softDeleteColumn, softDeleteUpdateTimestampColumn, softDeleteUpdateUserIdColumn,
  // softDeleteUpdateUserIdColumnNeedsQuotationMark, softDeleteUpdateUserIdColumnValue
  private static final String[] HARD_BASE_NO_FLAG = {"task1", "conn1", "Hard Delete",
      "HARD_DELETE", "tbl1", "id1", "(none)", null, null, null, null, null, null, null, null};
  private static final String[] HARD_BASE_WITH_FLAG = {"task1", "conn1", "Hard Delete",
      "HARD_DELETE", "tbl1", "id1", "(none)", null, null, null, "del_flg", null, null, null, null};
  private static final String[] SOFT_BASE_FLAG_ONLY = {"task1", "conn1", "Soft Delete",
      "SOFT_DELETE", "tbl1", "id1", "(none)", null, null, null, "del_flg", null, null, null, null};
  private static final String[] SOFT_BASE_ALL_OPTIONAL = {"task1", "conn1", "Soft Delete",
      "SOFT_DELETE", "tbl1", "id1", "(none)", null, null, null, "del_flg", "upd_at", "upd_by",
      "quotes(')", "SYSTEM"};

  private static HousekeepInfoBean bean(String[] cols) {
    HousekeepInfoBean b = new HousekeepInfoBean(Arrays.asList(cols));
    b.afterReading();
    return b;
  }

  @Nested
  @DisplayName("hard delete")
  class HardDelete {

    @Test
    @DisplayName("no soft-delete-column filter configured -> plain delete by key only")
    void noFilterColumnGeneratesPlainDelete() throws Exception {
      @SuppressWarnings("null")
      Connection conn = mock(Connection.class);
      @SuppressWarnings("null")
      PreparedStatement stmt = mock(PreparedStatement.class);
      when(conn.prepareStatement(anyString())).thenReturn(stmt);
      when(stmt.executeUpdate()).thenReturn(1);

      RecordDeleter deleter = new RecordDeleter(new DetailLogger(RecordDeleterTest.class));
      Map<String, Integer> tableRecordDeleted = new HashMap<>();

      deleter.softOrHardDeleteOne(conn, bean(HARD_BASE_NO_FLAG), false, "postgresql", 42,
          tableRecordDeleted, 0);

      @SuppressWarnings("null")
      ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
      verify(conn).prepareStatement(sqlCaptor.capture());
      assertThat(sqlCaptor.getValue()).isEqualTo("delete from tbl1 where id1 = ?");

      @SuppressWarnings("null")
      ArgumentCaptor<Object> bindCaptor = ArgumentCaptor.forClass(Object.class);
      verify(stmt).setObject(anyInt(), bindCaptor.capture());
      assertThat(bindCaptor.getAllValues()).containsExactly(42);

      assertThat(tableRecordDeleted).containsEntry("tbl1", 1);
    }

    @Test
    @DisplayName("with a soft-delete-column filter configured -> delete by key AND the flag "
        + "being true, both bound as JDBC parameters")
    void withFilterColumnAddsBoundFlagCondition() throws Exception {
      @SuppressWarnings("null")
      Connection conn = mock(Connection.class);
      @SuppressWarnings("null")
      PreparedStatement stmt = mock(PreparedStatement.class);
      when(conn.prepareStatement(anyString())).thenReturn(stmt);
      when(stmt.executeUpdate()).thenReturn(1);

      RecordDeleter deleter = new RecordDeleter(new DetailLogger(RecordDeleterTest.class));
      Map<String, Integer> tableRecordDeleted = new HashMap<>();

      deleter.softOrHardDeleteOne(conn, bean(HARD_BASE_WITH_FLAG), false, "postgresql", 42,
          tableRecordDeleted, 0);

      @SuppressWarnings("null")
      ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
      verify(conn).prepareStatement(sqlCaptor.capture());
      assertThat(sqlCaptor.getValue()).isEqualTo("delete from tbl1 where id1 = ? and del_flg = ?");

      @SuppressWarnings("null")
      ArgumentCaptor<Object> bindCaptor = ArgumentCaptor.forClass(Object.class);
      verify(stmt, times(2)).setObject(anyInt(),
          bindCaptor.capture());
      // The soft-delete-column filter is bound (RecordDeleter.getSoftDeleteColumnInfo() ->
      // BoundCondition), not embedded as a literal - see BoundCondition's class Javadoc.
      assertThat(bindCaptor.getAllValues()).containsExactly(42, Boolean.TRUE);

      assertThat(tableRecordDeleted).containsEntry("tbl1", 1);
    }
  }

  @Nested
  @DisplayName("soft delete")
  class SoftDelete {

    @Test
    @DisplayName("only the flag column configured -> update sets just the flag")
    void onlyFlagColumnConfiguredUpdatesJustTheFlag() throws Exception {
      @SuppressWarnings("null")
      Connection conn = mock(Connection.class);
      @SuppressWarnings("null")
      PreparedStatement stmt = mock(PreparedStatement.class);
      when(conn.prepareStatement(anyString())).thenReturn(stmt);
      when(stmt.executeUpdate()).thenReturn(1);

      RecordDeleter deleter = new RecordDeleter(new DetailLogger(RecordDeleterTest.class));
      Map<String, Integer> tableRecordDeleted = new HashMap<>();

      deleter.softOrHardDeleteOne(conn, bean(SOFT_BASE_FLAG_ONLY), true, "postgresql", 42,
          tableRecordDeleted, 0);

      @SuppressWarnings("null")
      ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
      verify(conn).prepareStatement(sqlCaptor.capture());
      assertThat(sqlCaptor.getValue()).isEqualTo("update tbl1 set del_flg = ? where id1 = ?");

      @SuppressWarnings("null")
      ArgumentCaptor<Object> bindCaptor = ArgumentCaptor.forClass(Object.class);
      verify(stmt, times(2)).setObject(anyInt(),
          bindCaptor.capture());
      assertThat(bindCaptor.getAllValues()).containsExactly(Boolean.TRUE, 42);

      assertThat(tableRecordDeleted).containsEntry("tbl1", 1);
    }

    @Test
    @DisplayName("all optional columns configured -> update sets flag, timestamp and "
        + "user-id, in that order")
    void allOptionalColumnsConfiguredUpdatesAllThree() throws Exception {
      @SuppressWarnings("null")
      Connection conn = mock(Connection.class);
      @SuppressWarnings("null")
      PreparedStatement stmt = mock(PreparedStatement.class);
      when(conn.prepareStatement(anyString())).thenReturn(stmt);
      when(stmt.executeUpdate()).thenReturn(1);

      RecordDeleter deleter = new RecordDeleter(new DetailLogger(RecordDeleterTest.class));
      Map<String, Integer> tableRecordDeleted = new HashMap<>();

      deleter.softOrHardDeleteOne(conn, bean(SOFT_BASE_ALL_OPTIONAL), true, "postgresql", 42,
          tableRecordDeleted, 0);

      @SuppressWarnings("null")
      ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
      verify(conn).prepareStatement(sqlCaptor.capture());
      // upd_by = 'SYSTEM' is a literal (excel-authored value via ColumnAndValueInfoBean), not
      // bound - see ColumnAndValueInfoBean's class Javadoc.
      assertThat(sqlCaptor.getValue()).isEqualTo(
          "update tbl1 set del_flg = ?, upd_at = ?, upd_by = 'SYSTEM' where id1 = ?");

      @SuppressWarnings("null")
      ArgumentCaptor<Object> bindCaptor = ArgumentCaptor.forClass(Object.class);
      verify(stmt, times(3)).setObject(anyInt(),
          bindCaptor.capture());
      List<Object> binds = bindCaptor.getAllValues();
      assertThat(binds).hasSize(3);
      assertThat(binds.get(0)).isEqualTo(Boolean.TRUE);
      // binds.get(1) is the "now" timestamp (an OffsetDateTime for postgresql) - not
      // deterministic, so just confirm its type rather than an exact value.
      assertThat(binds.get(1)).isInstanceOf(OffsetDateTime.class);
      assertThat(binds.get(2)).isEqualTo(42);

      assertThat(tableRecordDeleted).containsEntry("tbl1", 1);
    }
  }

  @Nested
  @DisplayName("tableRecordDeleted accumulation")
  class TableRecordDeletedAccumulation {

    @Test
    @DisplayName("a second delete on the same table merges (adds) onto the existing count")
    void secondCallOnSameTableAccumulates() throws Exception {
      @SuppressWarnings("null")
      Connection conn = mock(Connection.class);
      @SuppressWarnings("null")
      PreparedStatement stmt = mock(PreparedStatement.class);
      when(conn.prepareStatement(anyString())).thenReturn(stmt);
      when(stmt.executeUpdate()).thenReturn(3, 5);

      RecordDeleter deleter = new RecordDeleter(new DetailLogger(RecordDeleterTest.class));
      Map<String, Integer> tableRecordDeleted = new HashMap<>();

      deleter.softOrHardDeleteOne(conn, bean(HARD_BASE_NO_FLAG), false, "postgresql", 1,
          tableRecordDeleted, 0);
      deleter.softOrHardDeleteOne(conn, bean(HARD_BASE_NO_FLAG), false, "postgresql", 2,
          tableRecordDeleted, 0);

      assertThat(tableRecordDeleted).containsEntry("tbl1", 8);
    }
  }
}

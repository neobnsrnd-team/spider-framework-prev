package com.example.spider_admin.global.log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import org.h2.api.Trigger;

/**
 * H2 로그 테이블 자동 정리 트리거
 * <p>
 * LOGGING_EVENT 테이블에 INSERT 발생 시 7일 이상 된 로그를 자동 삭제합니다.
 * 성능을 위해 100건마다 한 번씩 정리 작업을 수행합니다.
 * </p>
 */
public class H2LogCleanupTrigger implements Trigger {

    private static final int CLEANUP_INTERVAL = 100;
    private static final int RETENTION_DAYS = 7;
    private static final int BATCH_DELETE_SIZE = 1000;

    private static final AtomicInteger insertCount = new AtomicInteger(0);

    private static final String DELETE_SQL =
            """
            DELETE FROM LOGGING_EVENT
            WHERE EVENT_ID IN (
                SELECT EVENT_ID FROM LOGGING_EVENT
                WHERE TIMESTMP < ?
                LIMIT ?
            )
            """;

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type)
            throws SQLException {
        // 초기화 불필요
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        int count = insertCount.incrementAndGet();

        if (count % CLEANUP_INTERVAL == 0) {
            cleanupOldLogs(conn);
        }
    }

    private void cleanupOldLogs(Connection conn) throws SQLException {
        long cutoffTimestamp = System.currentTimeMillis() - ((long) RETENTION_DAYS * 24 * 60 * 60 * 1000);

        try (PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {
            stmt.setLong(1, cutoffTimestamp);
            stmt.setInt(2, BATCH_DELETE_SIZE);
            stmt.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        // 정리 불필요
    }

    @Override
    public void remove() throws SQLException {
        // 정리 불필요
    }
}

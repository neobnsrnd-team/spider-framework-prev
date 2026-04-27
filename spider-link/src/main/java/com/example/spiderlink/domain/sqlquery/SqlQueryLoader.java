package com.example.spiderlink.domain.sqlquery;

import com.example.spiderlink.domain.sqlquery.dto.SqlQueryRecord;
import com.example.spiderlink.domain.sqlquery.mapper.SqlQueryDynamicMapper;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;

/**
 * 기동 시 FWK_SQL_QUERY 테이블에서 활성(USE_YN='Y') SQL을 전체 조회해
 * MyBatis Configuration에 MappedStatement로 동적 등록한다.
 *
 * <p>statementId 포맷: {@code SQL_GROUP_ID.QUERY_ID}
 * MetaDrivenCommandHandler의 {@code COMPONENT_CLASS_NAME.COMPONENT_METHOD_NAME}과 일치한다.
 *
 * <p>이미 등록된 static mapper와 충돌하는 statementId는 건너뛰고 경고 로그를 출력한다.
 * Reload API(Task #6)에서 {@link #reloadAll()} / {@link #reloadById(String)}를 호출해
 * WAS 재시작 없이 실시간 반영한다.
 *
 * <p>동시 리로드 요청은 {@code reloadLock}으로 직렬화하며, Zombie Statement 방지를 위해
 * DB에서 사라진 항목은 메모리에서 함께 제거한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlQueryLoader {

    private final SqlSessionFactory sqlSessionFactory;
    private final SqlQueryDynamicMapper sqlQueryDynamicMapper;

    /**
     * MyBatis Configuration의 내부 mappedStatements / resultMaps 맵에 리플렉션으로 접근하기 위해
     * static block에서 한 번만 Field 객체를 조회해 캐싱한다. 매 호출마다 getDeclaredField()를
     * 재수행하는 비효율과 ReflectiveOperationException 중복 처리를 방지한다.
     */
    private static final Field MAPPED_STATEMENTS_FIELD;
    private static final Field RESULT_MAPS_FIELD;

    static {
        try {
            MAPPED_STATEMENTS_FIELD = Configuration.class.getDeclaredField("mappedStatements");
            MAPPED_STATEMENTS_FIELD.setAccessible(true);
            RESULT_MAPS_FIELD = Configuration.class.getDeclaredField("resultMaps");
            RESULT_MAPS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** 동시 리로드 요청을 직렬화하여 Configuration 내부 맵의 Race Condition을 방지 */
    private final ReentrantLock reloadLock = new ReentrantLock();

    /**
     * 동적으로 등록된 statementId 목록. reloadAll() 시 DB에서 사라진 항목(Zombie)을
     * 식별해 메모리에서 제거하는 데 사용한다.
     */
    private final Set<String> dynamicStatementIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** 기동 시 FWK_SQL_QUERY 전체 로딩 */
    @PostConstruct
    public void loadAll() {
        List<SqlQueryRecord> queries = sqlQueryDynamicMapper.findAllActive();
        Configuration configuration = sqlSessionFactory.getConfiguration();

        int loaded = 0;
        int skipped = 0;

        for (SqlQueryRecord query : queries) {
            if (query.getSqlQuery() == null || query.getSqlQuery().isBlank()) {
                log.warn("[SqlQueryLoader] SQL_QUERY 비어있음 — 건너뜀: {}", query.getQueryId());
                skipped++;
                continue;
            }

            String statementId = buildStatementId(query);

            // 기존 static mapper와 충돌 방지 — 이미 등록된 statementId는 건너뜀
            if (configuration.hasStatement(statementId, false)) {
                log.warn("[SqlQueryLoader] statementId 충돌로 건너뜀: {}", statementId);
                skipped++;
                continue;
            }

            try {
                registerStatement(configuration, statementId, query.getSqlQuery(), query.getSqlType());
                dynamicStatementIds.add(statementId);
                loaded++;
                log.debug("[SqlQueryLoader] 등록: {}", statementId);
            } catch (Exception e) {
                log.error("[SqlQueryLoader] 등록 실패: {} — {}", statementId, e.getMessage());
                skipped++;
            }
        }

        log.info("[SqlQueryLoader] FWK_SQL_QUERY 로딩 완료 — 등록: {}건, 건너뜀: {}건", loaded, skipped);
    }

    /**
     * 특정 queryId를 DB에서 재조회해 MappedStatement를 갱신한다 (Reload API 용).
     *
     * <p>기존 statement를 제거 후 재등록하므로 WAS 재시작 없이 실시간 반영된다.
     *
     * @param queryId FWK_SQL_QUERY.QUERY_ID
     * @throws IllegalArgumentException queryId가 존재하지 않거나 SQL이 비어있을 때
     */
    public void reloadById(String queryId) {
        SqlQueryRecord record = sqlQueryDynamicMapper.findById(queryId);
        if (record == null) {
            throw new IllegalArgumentException("존재하지 않는 queryId: " + queryId);
        }
        if (record.getSqlQuery() == null || record.getSqlQuery().isBlank()) {
            throw new IllegalArgumentException("SQL_QUERY가 비어있음: " + queryId);
        }

        Configuration configuration = sqlSessionFactory.getConfiguration();
        String statementId = buildStatementId(record);

        reloadLock.lock();
        try {
            removeStatementIfExists(configuration, statementId);
            registerStatement(configuration, statementId, record.getSqlQuery(), record.getSqlType());
            dynamicStatementIds.add(statementId);
        } finally {
            reloadLock.unlock();
        }
        log.info("[SqlQueryLoader] 리로드 완료: {}", statementId);
    }

    /**
     * FWK_SQL_QUERY 전체를 DB에서 재조회해 MappedStatement를 전체 갱신한다 (Reload API 용).
     *
     * <p>DB에서 사라진(USE_YN='N' 또는 삭제된) statement를 메모리에서도 제거(Zombie 정리)하고,
     * 활성 statement를 재등록한다. static mapper로 등록된 statement는 건드리지 않는다.
     */
    public void reloadAll() {
        Configuration configuration = sqlSessionFactory.getConfiguration();
        List<SqlQueryRecord> queries = sqlQueryDynamicMapper.findAllActive();

        // DB 활성 statementId 목록 — 유효하지 않은 SQL은 제외
        Set<String> activeIds = queries.stream()
                .filter(q -> q.getSqlQuery() != null && !q.getSqlQuery().isBlank())
                .map(this::buildStatementId)
                .collect(Collectors.toSet());

        reloadLock.lock();
        try {
            // Zombie Statement 정리: 동적 등록 목록에 있지만 DB에서 사라진 항목 제거
            Set<String> zombieIds = new HashSet<>(dynamicStatementIds);
            zombieIds.removeAll(activeIds);
            for (String zombieId : zombieIds) {
                removeStatementIfExists(configuration, zombieId);
                dynamicStatementIds.remove(zombieId);
                log.info("[SqlQueryLoader] Zombie statement 제거: {}", zombieId);
            }

            int reloaded = 0;
            int skipped = 0;

            for (SqlQueryRecord query : queries) {
                if (query.getSqlQuery() == null || query.getSqlQuery().isBlank()) {
                    skipped++;
                    continue;
                }

                String statementId = buildStatementId(query);

                try {
                    removeStatementIfExists(configuration, statementId);
                    registerStatement(configuration, statementId, query.getSqlQuery(), query.getSqlType());
                    dynamicStatementIds.add(statementId);
                    reloaded++;
                } catch (Exception e) {
                    log.error("[SqlQueryLoader] 리로드 실패: {} — {}", statementId, e.getMessage());
                    skipped++;
                }
            }

            log.info("[SqlQueryLoader] 전체 리로드 완료 — 갱신: {}건, Zombie 제거: {}건, 실패: {}건",
                    reloaded, zombieIds.size(), skipped);
        } finally {
            reloadLock.unlock();
        }
    }

    /**
     * 특정 statementId를 비활성화한다 (USE_YN='N' 변경 시 호출).
     *
     * @param queryId FWK_SQL_QUERY.QUERY_ID
     */
    public void removeByQueryId(String queryId) {
        SqlQueryRecord record = sqlQueryDynamicMapper.findById(queryId);
        if (record == null) return;

        Configuration configuration = sqlSessionFactory.getConfiguration();
        String statementId = buildStatementId(record);

        reloadLock.lock();
        try {
            removeStatementIfExists(configuration, statementId);
            dynamicStatementIds.remove(statementId);
        } finally {
            reloadLock.unlock();
        }
        log.info("[SqlQueryLoader] statement 제거: {}", statementId);
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────

    private String buildStatementId(SqlQueryRecord record) {
        return record.getSqlGroupId() + "." + record.getQueryId();
    }

    /**
     * SQL 문자열로 MappedStatement를 생성해 Configuration에 등록한다.
     *
     * <p>Language Driver를 통해 SqlSource를 생성하므로 #{} 파라미터와
     * ${} 변수 치환이 모두 지원된다. 파라미터 타입은 Map (MetaDrivenCommandHandler
     * 가 paramMap을 Map<String,Object>로 전달하기 때문).
     */
    private void registerStatement(Configuration configuration, String statementId,
                                   String sql, String sqlType) {
        SqlCommandType commandType = toSqlCommandType(sqlType);

        LanguageDriver langDriver = configuration.getDefaultScriptingLanguageInstance();
        SqlSource sqlSource = langDriver.createSqlSource(configuration, sql, Map.class);

        MappedStatement.Builder builder = new MappedStatement.Builder(
                configuration, statementId, sqlSource, commandType);

        if (commandType == SqlCommandType.SELECT) {
            // SELECT 결과를 Map으로 반환 — resultType="map" 과 동일한 효과
            ResultMap resultMap = new ResultMap.Builder(
                    configuration,
                    statementId + "-Inline",
                    Map.class,
                    Collections.emptyList())
                    .build();
            builder.resultMaps(List.of(resultMap));
        } else {
            builder.resultMaps(Collections.emptyList());
        }

        configuration.addMappedStatement(builder.build());
    }

    /**
     * Configuration 내부의 mappedStatements / resultMaps 맵에서 해당 statement를 제거한다.
     *
     * <p>MyBatis Configuration은 removeStatement() API를 제공하지 않으므로 static block에서
     * 미리 캐싱한 Field 객체로 접근한다. SELECT 쿼리의 ResultMap도 함께 제거하여
     * 반복 리로드 시 resultMaps가 누적되는 Memory Leak을 방지한다.
     */
    private void removeStatementIfExists(Configuration configuration, String statementId) {
        if (!configuration.hasStatement(statementId, false)) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mappedStatements = (Map<String, Object>) MAPPED_STATEMENTS_FIELD.get(configuration);
            mappedStatements.remove(statementId);

            // SELECT 쿼리에 등록된 ResultMap도 함께 제거 — 누적 Memory Leak 방지
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMaps = (Map<String, Object>) RESULT_MAPS_FIELD.get(configuration);
            resultMaps.remove(statementId + "-Inline");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "MappedStatement 제거 실패: " + statementId, e);
        }
    }

    /** FWK_SQL_QUERY.SQL_TYPE 단일 문자 코드를 MyBatis SqlCommandType으로 변환한다. */
    private SqlCommandType toSqlCommandType(String sqlType) {
        if (sqlType == null) return SqlCommandType.SELECT;
        return switch (sqlType.toUpperCase().trim()) {
            case "R", "S", "SELECT" -> SqlCommandType.SELECT;
            case "C", "I", "INSERT" -> SqlCommandType.INSERT;
            case "U", "UPDATE"      -> SqlCommandType.UPDATE;
            case "D", "DELETE"      -> SqlCommandType.DELETE;
            default -> {
                log.warn("[SqlQueryLoader] 알 수 없는 SQL_TYPE '{}' — SELECT로 처리", sqlType);
                yield SqlCommandType.SELECT;
            }
        };
    }
}

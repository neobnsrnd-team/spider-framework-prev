package com.example.admin_demo.domain.worklist.service;

import com.example.admin_demo.domain.worklist.dto.WorkListResponse;
import com.example.admin_demo.domain.worklist.dto.WorkListScriptResponse;
import com.example.admin_demo.domain.worklist.mapper.WorkListMapper;
import com.example.admin_demo.global.exception.InternalException;
import com.example.admin_demo.global.exception.InvalidInputException;
import com.example.admin_demo.global.exception.NotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Clob;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이행스크립트 생성·조회·다운로드 서비스.
 *
 * <p>FWK_WORK_LIST 항목의 원본 데이터를 조회하여 DELETE + INSERT SQL 파일을 생성하고,
 * 파일시스템에 저장한 후 FILE_NAME을 FWK_WORK_LIST에 업데이트한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkListScriptService {

    private final WorkListMapper workListMapper;
    private final JdbcTemplate jdbcTemplate;

    @Value("${worklist.script.path:${user.dir}/worklist-scripts}")
    private String scriptBasePath;

    /** WORK_ID별 테이블 설정 (메인 테이블 + 자식 테이블). */
    private static final Map<String, ScriptConfig> SCRIPT_CONFIGS = buildConfigs();

    /**
     * 이행스크립트 생성 및 파일 저장.
     * 생성 완료 후 FWK_WORK_LIST.FILE_NAME을 업데이트한다.
     */
    @Transactional
    public WorkListScriptResponse generateAndSave(int workSeq) {
        WorkListResponse item = getItemOrThrow(workSeq);
        ScriptConfig config = getConfigOrThrow(item.getWorkOriId());

        String content = buildScript(config, item.getWorkOriId(), item.getWorkDataPk());
        String fileName = saveToFile(item.getWorkOriId(), item.getWorkDataPk(), content);
        workListMapper.updateFileName(workSeq, fileName);

        return new WorkListScriptResponse(fileName, content);
    }

    /** 저장된 이행스크립트 내용 조회. */
    public WorkListScriptResponse getContent(int workSeq) {
        WorkListResponse item = getItemOrThrow(workSeq);
        if (item.getFileName() == null) {
            throw new NotFoundException("이행스크립트가 존재하지 않습니다. workSeq: " + workSeq);
        }
        String content = readFile(item.getFileName());
        return new WorkListScriptResponse(item.getFileName(), content);
    }

    /** 다운로드용 파일 경로 반환. */
    public Path getFilePath(int workSeq) {
        WorkListResponse item = getItemOrThrow(workSeq);
        if (item.getFileName() == null) {
            throw new NotFoundException("이행스크립트가 존재하지 않습니다. workSeq: " + workSeq);
        }
        return Paths.get(scriptBasePath, item.getFileName());
    }

    // ============================================================
    // private — 스크립트 생성
    // ============================================================

    private String buildScript(ScriptConfig config, String workOriId, String workDataPk) {
        // WORK_DATA_PK는 복합 PK일 경우 '@' 구분자로 연결된 문자열
        String[] pkValues = workDataPk.split("@", -1);
        StringBuilder sb = new StringBuilder();

        sb.append("-- ==================================================\n");
        sb.append("-- 이행스크립트: [")
                .append(workOriId)
                .append("] ")
                .append(workDataPk)
                .append("\n");
        sb.append("-- 생성일시: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("\n");
        sb.append("-- ==================================================\n\n");

        // 메인 테이블
        appendTableScript(sb, config.getTableName(), config.getPkColumns(), pkValues);

        // 자식 테이블 (부모 PK = 자식 FK 순서 동일)
        for (ChildTableConfig child : config.getChildTables()) {
            appendChildTableScript(sb, child.getTableName(), child.getFkColumns(), pkValues);
        }

        sb.append("COMMIT;\n");
        return sb.toString();
    }

    private void appendTableScript(StringBuilder sb, String tableName, List<String> pkColumns, String[] pkValues) {
        sb.append("-- ## ").append(tableName).append(" ##\n");
        sb.append("DELETE FROM ")
                .append(tableName)
                .append("\nWHERE ")
                .append(buildWhereLiteral(pkColumns, pkValues))
                .append(";\n\n");

        List<ColumnInfo> columns = getColumnMetadata(tableName);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + tableName + " WHERE " + buildWhereParam(pkColumns), (Object[]) pkValues);

        if (rows.isEmpty()) {
            sb.append("-- [INFO] 현재 DB에 해당 데이터가 없습니다 (삭제된 항목). INSERT 생략.\n\n");
        } else {
            for (Map<String, Object> row : rows) {
                sb.append(buildInsert(tableName, columns, row)).append(";\n");
            }
            sb.append("\n");
        }
    }

    private void appendChildTableScript(
            StringBuilder sb, String childTableName, List<String> fkColumns, String[] pkValues) {
        sb.append("-- ## ").append(childTableName).append(" ##\n");
        sb.append("DELETE FROM ")
                .append(childTableName)
                .append("\nWHERE ")
                .append(buildWhereLiteral(fkColumns, pkValues))
                .append(";\n\n");

        List<ColumnInfo> columns = getColumnMetadata(childTableName);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + childTableName + " WHERE " + buildWhereParam(fkColumns), (Object[]) pkValues);

        for (Map<String, Object> row : rows) {
            sb.append(buildInsert(childTableName, columns, row)).append(";\n");
        }
        sb.append("\n");
    }

    /** WHERE col1 = 'val1' AND col2 = 'val2' 형식 (SQL 리터럴 — 스크립트 출력용). */
    private String buildWhereLiteral(List<String> columns, String[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(" AND ");
            sb.append(columns.get(i))
                    .append(" = '")
                    .append(values[i].replace("'", "''"))
                    .append("'");
        }
        return sb.toString();
    }

    /** WHERE col1 = ? AND col2 = ? 형식 (JDBC 파라미터 바인딩용). */
    private String buildWhereParam(List<String> columns) {
        return columns.stream().map(c -> c + " = ?").collect(Collectors.joining(" AND "));
    }

    private List<ColumnInfo> getColumnMetadata(String tableName) {
        return jdbcTemplate.query(
                "SELECT COLUMN_NAME, DATA_TYPE FROM ALL_TAB_COLUMNS "
                        + "WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') "
                        + "AND TABLE_NAME = ? ORDER BY COLUMN_ID",
                (rs, rowNum) -> new ColumnInfo(rs.getString("COLUMN_NAME"), rs.getString("DATA_TYPE")),
                tableName.toUpperCase());
    }

    private String buildInsert(String tableName, List<ColumnInfo> columns, Map<String, Object> row) {
        List<String> colNames = new ArrayList<>();
        List<String> colValues = new ArrayList<>();
        for (ColumnInfo col : columns) {
            colNames.add(col.getName());
            colValues.add(formatValue(row.get(col.getName()), col.getDataType()));
        }
        return "INSERT INTO " + tableName + " ("
                + String.join(", ", colNames)
                + ")\nVALUES ("
                + String.join(", ", colValues) + ")";
    }

    private String formatValue(Object value, String dataType) {
        if (value == null) return "NULL";

        // Timestamp 처리 (Oracle DATE도 Timestamp로 반환될 수 있음)
        if (value instanceof Timestamp ts) {
            String formatted = new SimpleDateFormat("yyyyMMddHHmmss").format(ts);
            return "TO_DATE('" + formatted + "', 'YYYYMMDDHH24MISS')";
        }
        if (value instanceof java.sql.Date d) {
            String formatted = new SimpleDateFormat("yyyyMMdd").format(d);
            return "TO_DATE('" + formatted + "', 'YYYYMMDD')";
        }
        // CLOB 처리
        if (value instanceof Clob clob) {
            try {
                value = clob.getSubString(1, (int) clob.length());
            } catch (Exception e) {
                log.warn("CLOB 읽기 실패: {}", e.getMessage());
                return "NULL /*CLOB read error*/";
            }
        }
        // NUMBER: 따옴표 없이 출력
        if (value instanceof Number) {
            return value.toString();
        }
        // 나머지 (VARCHAR2, CHAR 등): 따옴표 + 내부 따옴표 이스케이프
        String strVal = value.toString().replace("'", "''");
        return "'" + strVal + "'";
    }

    // ============================================================
    // private — 파일 I/O
    // ============================================================

    private String saveToFile(String workOriId, String workDataPk, String content) {
        // 파일명: {WORK_ID}_{pk_safe}_{yyyyMMddHHmm}.sql
        String safePk = workDataPk.replaceAll("[^A-Za-z0-9_]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String fileName = workOriId + "_" + safePk + "_" + timestamp + ".sql";

        try {
            Path dir = Paths.get(scriptBasePath);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(fileName), content, StandardCharsets.UTF_8);
            log.info("이행스크립트 저장: {}", fileName);
        } catch (IOException e) {
            throw new InternalException("이행스크립트 파일 저장 실패: " + e.getMessage(), e);
        }
        return fileName;
    }

    private String readFile(String fileName) {
        try {
            return Files.readString(Paths.get(scriptBasePath, fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new NotFoundException("이행스크립트 파일을 찾을 수 없습니다: " + fileName);
        }
    }

    // ============================================================
    // private — 헬퍼
    // ============================================================

    private WorkListResponse getItemOrThrow(int workSeq) {
        WorkListResponse item = workListMapper.findByWorkSeq(workSeq);
        if (item == null) throw new NotFoundException("workSeq: " + workSeq);
        return item;
    }

    private ScriptConfig getConfigOrThrow(String workOriId) {
        ScriptConfig config = SCRIPT_CONFIGS.get(workOriId);
        if (config == null) {
            throw new InvalidInputException("지원하지 않는 항목 유형입니다: " + workOriId);
        }
        return config;
    }

    // ============================================================
    // 내부 설정 클래스
    // ============================================================

    @Getter
    @AllArgsConstructor
    private static class ColumnInfo {
        private final String name;
        private final String dataType;
    }

    @Getter
    @AllArgsConstructor
    private static class ScriptConfig {
        private final String tableName;
        private final List<String> pkColumns;
        private final List<ChildTableConfig> childTables;
    }

    @Getter
    @AllArgsConstructor
    private static class ChildTableConfig {
        private final String tableName;
        /** 부모 PK 컬럼과 순서가 일치하는 FK 컬럼 목록. */
        private final List<String> fkColumns;
    }

    private static Map<String, ScriptConfig> buildConfigs() {
        Map<String, ScriptConfig> m = new LinkedHashMap<>();
        m.put(
                "Message",
                new ScriptConfig(
                        "FWK_MESSAGE",
                        List.of("ORG_ID", "MESSAGE_ID"),
                        List.of(new ChildTableConfig("FWK_MESSAGE_FIELD", List.of("ORG_ID", "MESSAGE_ID")))));
        m.put("Trx", new ScriptConfig("FWK_TRX", List.of("TRX_ID"), List.of()));
        m.put(
                "P_SERVICE",
                new ScriptConfig(
                        "FWK_SERVICE",
                        List.of("SERVICE_ID"),
                        List.of(new ChildTableConfig("FWK_SERVICE_RELATION", List.of("SERVICE_ID")))));
        m.put(
                "B_Service",
                new ScriptConfig(
                        "FWK_SERVICE",
                        List.of("SERVICE_ID"),
                        List.of(new ChildTableConfig("FWK_SERVICE_RELATION", List.of("SERVICE_ID")))));
        m.put(
                "P_Component",
                new ScriptConfig(
                        "FWK_COMPONENT",
                        List.of("COMPONENT_ID"),
                        List.of(new ChildTableConfig("FWK_COMPONENT_PARAM", List.of("COMPONENT_ID")))));
        m.put(
                "B_Component",
                new ScriptConfig(
                        "FWK_COMPONENT",
                        List.of("COMPONENT_ID"),
                        List.of(new ChildTableConfig("FWK_COMPONENT_PARAM", List.of("COMPONENT_ID")))));
        m.put("SQL_QUERY", new ScriptConfig("FWK_SQL_QUERY", List.of("QUERY_ID"), List.of()));
        m.put("SQL_CONF", new ScriptConfig("FWK_SQL_CONF", List.of("DB_ID"), List.of()));
        m.put("Errorcode", new ScriptConfig("FWK_ERROR", List.of("ERROR_CODE"), List.of()));
        m.put(
                "Codegroup",
                new ScriptConfig(
                        "FWK_CODE_GROUP",
                        List.of("CODE_GROUP_ID"),
                        List.of(new ChildTableConfig("FWK_CODE", List.of("CODE_GROUP_ID")))));
        m.put("Board_Service", new ScriptConfig("FWK_BOARD", List.of("BOARD_ID"), List.of()));
        return Collections.unmodifiableMap(m);
    }
}

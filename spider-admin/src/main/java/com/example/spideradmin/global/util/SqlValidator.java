package com.example.spideradmin.global.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * <h3>SQL 쿼리 검증 유틸리티</h3>
 * <p>SQL Injection 공격을 방지하기 위한 쿼리 검증 기능을 제공합니다.</p>
 *
 * <h4>검증 규칙:</h4>
 * <ul>
 *     <li>SELECT 쿼리만 허용 (DML/DDL 명령어 차단)</li>
 *     <li>위험한 SQL 키워드 차단 (DROP, DELETE, UPDATE 등)</li>
 *     <li>SQL Injection 패턴 차단 </li>
 *     <li>주석 패턴 차단</li>
 * </ul>
 */
@Slf4j
public final class SqlValidator {

    private SqlValidator() {}

    /**
     * 위험한 SQL 키워드 목록 (대소문자 무관)
     */
    private static final List<String> DANGEROUS_KEYWORDS = Arrays.asList(
            // DDL (Data Definition Language)
            "DROP",
            "CREATE",
            "ALTER",
            "TRUNCATE",
            "RENAME",

            // DML (Data Manipulation Language) - SELECT 제외
            "INSERT",
            "UPDATE",
            "DELETE",
            "MERGE",

            // DCL (Data Control Language)
            "GRANT",
            "REVOKE",

            // TCL (Transaction Control Language)
            "COMMIT",
            "ROLLBACK",
            "SAVEPOINT",

            // 시스템 명령어
            "EXEC",
            "EXECUTE",
            "XP_",
            "SP_",

            // SQL Injection 공격 키워드
            "UNION",
            "CONCAT",
            "CHAR",
            "NCHAR",
            "INFORMATION_SCHEMA",
            "SYSOBJECTS",
            "SYSCOLUMNS");

    /**
     * SQL Injection 공격 패턴 (정규식)
     */
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            // 세미콜론 (쿼리 분리)
            Pattern.compile(";", Pattern.CASE_INSENSITIVE),

            // 주석 패턴
            Pattern.compile("--", Pattern.CASE_INSENSITIVE),
            Pattern.compile("/\\*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\*/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("#", Pattern.CASE_INSENSITIVE),

            // UNION 기반 공격
            Pattern.compile("\\bUNION\\b.*\\bSELECT\\b", Pattern.CASE_INSENSITIVE),

            // Boolean 기반 공격
            Pattern.compile("'\\s*OR\\s+'?1'?\\s*=\\s*'?1", Pattern.CASE_INSENSITIVE),
            Pattern.compile("'\\s*OR\\s+'?1'?\\s*=\\s*'?1", Pattern.CASE_INSENSITIVE),

            // Time-based 공격
            Pattern.compile("\\bSLEEP\\b\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bWAITFOR\\b\\s+\\bDELAY\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bBENCHMARK\\b\\s*\\(", Pattern.CASE_INSENSITIVE),

            // Stacked queries
            Pattern.compile(";\\s*DROP\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile(";\\s*DELETE\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile(";\\s*UPDATE\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile(";\\s*INSERT\\b", Pattern.CASE_INSENSITIVE),

            // Hex encoding 공격
            Pattern.compile("0x[0-9a-f]+", Pattern.CASE_INSENSITIVE));

    /**
     * SQL 쿼리 검증 (공백 허용)
     *
     * @param query 검증할 SQL 쿼리
     * @return 검증 결과 (true: 안전, false: 위험)
     */
    public static boolean isValidQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true; // 빈 쿼리는 허용
        }

        try {
            validateQuery(query);
            return true;
        } catch (Exception e) {
            log.warn("SQL validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * SQL 쿼리 검증 (예외 발생)
     *
     * @param query 검증할 SQL 쿼리
     * @throws IllegalArgumentException 위험한 쿼리일 경우
     */
    public static void validateQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return; // 빈 쿼리는 통과
        }

        String trimmedQuery = query.trim().toUpperCase();

        // 1. SELECT 쿼리만 허용
        if (!trimmedQuery.startsWith("SELECT")) {
            throw new IllegalArgumentException("SELECT 쿼리만 허용됩니다");
        }

        // 2. 위험한 키워드 검증
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (containsKeyword(query, keyword)) {
                throw new IllegalArgumentException("위험한 키워드 감지: " + keyword);
            }
        }

        // 3. SQL Injection 패턴 검증
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(query).find()) {
                throw new IllegalArgumentException("위험한 SQL 패턴 감지: " + pattern.pattern());
            }
        }

        // 4. 다중 쿼리 차단 (세미콜론 검증은 위에서 했지만 추가 확인)
        if (query.contains(";")) {
            throw new IllegalArgumentException("다중 쿼리는 허용되지 않습니다");
        }
    }

    /**
     * 쿼리에 특정 키워드가 포함되어 있는지 확인
     * (단어 경계를 고려하여 정확한 매칭)
     *
     * @param query SQL 쿼리
     * @param keyword 검색할 키워드
     * @return 포함 여부
     */
    private static boolean containsKeyword(String query, String keyword) {
        // 단어 경계(\b)를 사용하여 정확한 단어 매칭
        Pattern pattern = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(query).find();
    }

    /**
     * 쿼리 정규화 (공백 정리, 대소문자 통일)
     *
     * @param query SQL 쿼리
     * @return 정규화된 쿼리
     */
    public static String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }

        // 연속된 공백을 하나로
        return query.trim().replaceAll("\\s+", " ");
    }
}

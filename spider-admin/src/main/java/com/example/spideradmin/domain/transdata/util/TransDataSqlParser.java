package com.example.spideradmin.domain.transdata.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 이행 파일 파싱 유틸리티
 * <p>
 * ##섹션명## 기반 SQL 파일 구조를 섹션 단위로 분리하고,
 * FWK_TRANS_DATA_TIMES INSERT에서 TRAN_SEQ를 추출합니다.
 */
public final class TransDataSqlParser {

    private static final Pattern TRAN_SEQ_PATTERN = Pattern.compile(
            "INSERT\\s+INTO\\s+FWK_TRANS_DATA_TIMES\\s*\\([^)]*\\)\\s*VALUES\\s*\\(\\s*'([^']+)'",
            Pattern.CASE_INSENSITIVE);

    private TransDataSqlParser() {}

    /** SQL 파일을 ##섹션명## 단위로 파싱한 결과 */
    public record ParsedSection(String name, List<String> statements) {}

    /**
     * FWK_TRANS_DATA_TIMES INSERT에서 TRAN_SEQ 값을 추출합니다.
     * TRAN_SEQ는 항상 첫 번째 컬럼으로 가정합니다.
     */
    public static String extractTranSeq(String content) {
        Matcher matcher = TRAN_SEQ_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * SQL 파일 내용을 ##섹션명## 헤더 기준으로 섹션 목록으로 분리합니다.
     */
    public static List<ParsedSection> parseSections(String content) {
        List<ParsedSection> result = new ArrayList<>();
        String currentName = null;
        StringBuilder currentContent = new StringBuilder();

        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.matches("^##[^#]+##$")) {
                if (currentName != null) {
                    result.add(new ParsedSection(currentName, parseStatements(currentContent.toString())));
                }
                currentName = trimmed.replaceAll("(^##)|(##$)", "").trim();
                currentContent = new StringBuilder();
            } else {
                currentContent.append(line).append("\n");
            }
        }
        if (currentName != null) {
            result.add(new ParsedSection(currentName, parseStatements(currentContent.toString())));
        }
        return result;
    }

    /**
     * 문자열을 maxLength 자로 제한합니다. 초과 시 끝에 "..."을 붙입니다.
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * 섹션 내용을 ';' 기준으로 SQL 구문 목록으로 파싱합니다.
     * 단순 split 대신 상태 기계(State Machine) 방식으로 처리하여
     * 문자열 리터럴('...')과 라인 주석(--), 블록 주석(/* ... *&#47;) 안의 세미콜론을 무시합니다.
     * 빈 구문, COMMIT, ROLLBACK은 제외합니다.
     * <p>
     * 한계: PL/SQL BEGIN...END 블록은 지원하지 않습니다.
     * 이 파서는 순수 DML(INSERT/UPDATE/DELETE/MERGE) 위주 이행 파일을 대상으로 합니다.
     */
    // 상태 배열 인덱스
    private static final int IN_SINGLE_QUOTE = 0;

    private static final int IN_LINE_COMMENT = 1;
    private static final int IN_BLOCK_COMMENT = 2;

    private static List<String> parseStatements(String sectionContent) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int len = sectionContent.length();
        boolean[] state = new boolean[3];

        for (int i = 0; i < len; i++) {
            char c = sectionContent.charAt(i);
            char next = (i + 1 < len) ? sectionContent.charAt(i + 1) : '\0';
            i += processChar(c, next, state, current, result);
        }

        // 세미콜론 없이 끝나는 마지막 구문
        addIfValid(result, current.toString());

        return result;
    }

    private static int processChar(char c, char next, boolean[] state, StringBuilder current, List<String> result) {
        if (state[IN_LINE_COMMENT]) return handleLineComment(c, state);
        if (state[IN_BLOCK_COMMENT]) return handleBlockComment(c, next, state);
        if (state[IN_SINGLE_QUOTE]) return handleSingleQuote(c, next, state, current);
        return handleNormalChar(c, next, state, current, result);
    }

    private static int handleLineComment(char c, boolean[] state) {
        if (c == '\n') state[IN_LINE_COMMENT] = false;
        return 0;
    }

    private static int handleBlockComment(char c, char next, boolean[] state) {
        if (c == '*' && next == '/') {
            state[IN_BLOCK_COMMENT] = false;
            return 1;
        }
        return 0;
    }

    private static int handleSingleQuote(char c, char next, boolean[] state, StringBuilder current) {
        current.append(c);
        if (c == '\'' && next == '\'') {
            // Oracle 스타일 '' 이스케이프
            current.append(next);
            return 1;
        }
        if (c == '\'') {
            state[IN_SINGLE_QUOTE] = false;
        }
        return 0;
    }

    private static int handleNormalChar(
            char c, char next, boolean[] state, StringBuilder current, List<String> result) {
        if (c == '-' && next == '-') {
            state[IN_LINE_COMMENT] = true;
            return 1;
        }
        if (c == '/' && next == '*') {
            state[IN_BLOCK_COMMENT] = true;
            return 1;
        }
        if (c == '\'') {
            state[IN_SINGLE_QUOTE] = true;
            current.append(c);
            return 0;
        }
        if (c == ';') {
            addIfValid(result, current.toString());
            current.setLength(0);
            return 0;
        }
        current.append(c);
        return 0;
    }

    private static void addIfValid(List<String> result, String raw) {
        String stmt = raw.trim();
        if (!stmt.isEmpty() && !stmt.equalsIgnoreCase("COMMIT") && !stmt.equalsIgnoreCase("ROLLBACK")) {
            result.add(stmt);
        }
    }
}

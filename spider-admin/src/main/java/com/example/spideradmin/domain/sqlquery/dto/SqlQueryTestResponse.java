package com.example.spideradmin.domain.sqlquery.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SQL 쿼리 테스트 실행 결과 DTO
 *
 * <p>최대 50행을 반환하며, 실행 오류 발생 시 {@code errorMessage} 에 내용을 담고 나머지 필드는 null/비어있다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlQueryTestResponse {

    /** 결과 컬럼명 목록 (조회 순서 유지) */
    private List<String> columns;

    /** 결과 행 목록 (컬럼명 → 값, 최대 50행) */
    private List<Map<String, Object>> rows;

    /** 실제 조회된 행 수 */
    private int rowCount;

    /** 쿼리 실행 시간 (ms) */
    private long executionTimeMs;

    /** 실행 오류 메시지 (성공 시 null) */
    private String errorMessage;
}

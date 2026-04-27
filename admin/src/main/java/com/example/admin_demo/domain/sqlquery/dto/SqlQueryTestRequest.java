package com.example.admin_demo.domain.sqlquery.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SQL 쿼리 테스트 실행 요청 DTO.
 *
 * <p>파라미터가 없는 쿼리는 빈 요청 바디로 호출하면 되며,
 * NULL로 바인딩할 파라미터는 리스트 항목을 {@code null}로 전달한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlQueryTestRequest {

    /**
     * 바인딩 파라미터 값 목록 (SQL 내 등장 순서).
     * <ul>
     *   <li>JDBC {@code ?} 방식: 인덱스 순서대로 매핑</li>
     *   <li>MyBatis {@code #{varName}} 방식: 변수 등장 순서대로 매핑</li>
     *   <li>항목이 {@code null} 이면 SQL NULL로 바인딩</li>
     * </ul>
     */
    private List<String> params;
}

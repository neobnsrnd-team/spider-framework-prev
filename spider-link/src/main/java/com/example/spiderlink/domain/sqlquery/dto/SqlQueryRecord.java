package com.example.spiderlink.domain.sqlquery.dto;

import lombok.Data;

/** FWK_SQL_QUERY 테이블에서 조회한 동적 SQL 등록 정보. */
@Data
public class SqlQueryRecord {

    private String queryId;
    private String sqlGroupId;
    /** SQL 원문 (CLOB). #{} 방식 파라미터를 포함할 수 있다. */
    private String sqlQuery;
    /** SQL 유형: R(SELECT) / C(INSERT) / U(UPDATE) / D(DELETE) */
    private String sqlType;
}

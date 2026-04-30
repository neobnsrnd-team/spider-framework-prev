package com.example.spideradmin.domain.sqlquery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FWK_SQL_QUERY_HIS 테이블 이력 조회 응답 DTO.
 *
 * <p>수정 전 자동백업 내역 조회 및 복원 비교에 사용한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SqlQueryHistoryResponse {

    /** 이력 버전 ID (System.currentTimeMillis() 문자열) */
    private String versionId;

    private String queryId;
    private String queryName;
    private String sqlGroupId;
    private String dbId;
    private String sqlType;
    private String execType;
    private String cacheYn;
    private String timeOut;
    private String resultType;
    private String useYn;
    private String sqlQuery;
    private String sqlQuery2;
    private String queryDesc;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String historyReason;
}

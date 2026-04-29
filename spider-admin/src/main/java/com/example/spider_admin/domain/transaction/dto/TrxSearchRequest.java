package com.example.spider_admin.domain.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trx 검색 조건 요청 DTO
 * GET /api/trx/page 요청에 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxSearchRequest {

    /**
     * 검색 대상 필드 (trxId, trxName, messageId)
     */
    private String searchField;

    /**
     * 검색어
     */
    private String searchValue;

    /**
     * 기관ID 필터
     */
    private String orgIdFilter;

    /**
     * 거래중지여부 필터 (Y, N)
     */
    private String trxStopYnFilter;

    /**
     * 정렬 컬럼명
     */
    private String sortBy;

    /**
     * 정렬 방향 (ASC, DESC)
     */
    private String sortDirection;
}

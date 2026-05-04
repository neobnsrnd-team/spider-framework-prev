package com.example.spideradmin.domain.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 거래중지 페이지 검색 조건 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxStopSearchRequest {

    /** 검색 대상 필드 (trxId, trxName) */
    private String searchField;

    /** 검색어 */
    private String searchValue;

    /** 운영모드 필터 (T, R, D) */
    private String operModeTypeFilter;

    /** 거래중지여부 필터 (Y, N) */
    private String trxStopYnFilter;

    /** 정렬 컬럼명 */
    private String sortBy;

    /** 정렬 방향 (ASC, DESC) */
    private String sortDirection;
}

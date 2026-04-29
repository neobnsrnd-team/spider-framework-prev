package com.example.spideradmin.domain.errorcode.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류코드 검색 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorSearchRequest {

    /**
     * 검색 유형 (errorCode: 오류코드, errorTitle: 오류제목)
     */
    private String searchField;

    /**
     * 검색어
     */
    private String searchValue;

    /**
     * 거래 ID 필터
     */
    private String trxId;

    /**
     * 핸들러 APP ID 필터
     */
    private String handleAppId;
}

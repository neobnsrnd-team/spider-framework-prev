package com.example.spider_admin.domain.errorhistory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류 발생 이력 검색 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorHisSearchRequest {

    // 페이징 파라미터
    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 20;

    private String sortBy;

    private String sortDirection;

    // 검색 파라미터
    /**
     * 오류 코드 검색
     */
    private String errorCode;

    /**
     * 고객 ID 검색
     */
    private String custUserId;

    /**
     * 고객 전화번호 검색
     */
    private String custPhoneNo;

    /**
     * 오류 발생 시작 일시 (yyyyMMddHHmmss)
     */
    private String startDtime;

    /**
     * 오류 발생 종료 일시 (yyyyMMddHHmmss)
     */
    private String endDtime;
}

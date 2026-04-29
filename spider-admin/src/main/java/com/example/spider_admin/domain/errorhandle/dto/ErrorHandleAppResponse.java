package com.example.spider_admin.domain.errorhandle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류별 핸들러 APP 매핑 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorHandleAppResponse {

    private String errorCode;
    private String handleAppId;
    private String userParamValue;

    // 조회용 추가 필드
    private String handleAppName;
}

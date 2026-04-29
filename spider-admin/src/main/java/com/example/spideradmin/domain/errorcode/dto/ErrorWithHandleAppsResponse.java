package com.example.spideradmin.domain.errorcode.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류코드 + 핸들러 목록 응답 DTO (목록 조회용)
 * QueryMapper에서 직접 프로젝션하여 반환
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorWithHandleAppsResponse {

    private String errorCode;
    private String trxId;
    private String errorTitle;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String orgId;
    private String orgErrorCode;
    private String errorCauseDesc;
    private String errorLevel;
    private String errorHttpcode;
    private String finalApprovalState;
    private String finalApprovalDtime;
    private String finalApprovalUserId;
    private String finalApprovalUserName;

    // UI 표시용 추가 필드
    private String errorLevelName;
    private Integer errorCount;

    // 핸들러 목록 (콤마 구분 문자열)
    private String handleApps;
}

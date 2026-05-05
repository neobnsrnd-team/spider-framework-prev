package com.example.spideradmin.domain.errorcode.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류코드 응답 DTO (목록 조회용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private String errorCode;
    private String trxId;
    private String errorTitle;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String orgId;
    private String orgErrorCode;
    private String errorLevel;
    private String errorHttpcode;
    private String finalApprovalState;
    private String finalApprovalDtime;
    private String finalApprovalUserId;
    private String finalApprovalUserName;

    // UI 표시용 추가 필드
    private String errorLevelName;
    private Integer errorCount;
}

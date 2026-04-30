package com.example.spideradmin.domain.errorcode.dto;

import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류코드 상세 응답 DTO (상세 조회용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorDetailResponse {

    // ===== FWK_ERROR 기본 정보 =====
    private String errorCode;
    private String trxId;
    private String errorTitle;
    private String errorLevel;
    private String orgId;
    private String orgErrorCode;
    private String errorHttpcode;
    private String lastUpdateDtime;
    private String lastUpdateUserId;

    // ===== 한국어 안내용 오류 정보 (locale=KO) =====
    private String koErrorTitle;
    private String koErrorCauseDesc;
    private String koErrorGuideDesc;
    private String koHelpPageUrl;
    private String koFaqPageUrl;

    // ===== 영어 안내용 오류 정보 (locale=EN) =====
    private String enErrorTitle;
    private String enErrorCauseDesc;
    private String enErrorGuideDesc;
    private String enHelpPageUrl;
    private String enFaqPageUrl;

    // ===== 폰뱅킹 오류메세지 =====
    private String koMessage;
    private String enMessage;

    // ===== 핸들러 목록 =====
    private List<ErrorHandleAppResponse> handleApps;

    // UI 표시용
    private String errorLevelName;
    private Integer errorCount;
}

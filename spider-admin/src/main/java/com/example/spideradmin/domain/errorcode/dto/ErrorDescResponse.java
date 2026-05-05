package com.example.spideradmin.domain.errorcode.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류코드 다국어 설명 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorDescResponse {

    private String errorCode;
    private String localeCode;
    private String errorTitle;
    private String pbErrorTitle;
    private String etcErrorTitle;
    private String errorCauseDesc;
    private String errorGuideDesc;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String helpPageUrl;
    private String ibsErrorGuideDesc;
    private String cmsErrorGuideDesc;
    private String etcErrorGuideDesc;
}

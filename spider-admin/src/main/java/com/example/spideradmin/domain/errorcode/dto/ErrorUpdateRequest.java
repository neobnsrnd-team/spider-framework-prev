package com.example.spideradmin.domain.errorcode.dto;

import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류코드 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorUpdateRequest {

    @Size(max = 200, message = "트랜잭션ID는 200자 이내여야 합니다")
    private String trxId;

    @NotBlank(message = "오류제목은 필수입니다")
    @Size(max = 200, message = "오류제목은 200자 이내여야 합니다")
    private String errorTitle;

    @NotBlank(message = "오류레벨은 필수입니다")
    private String errorLevel;

    @Size(max = 50, message = "기관ID는 50자 이내여야 합니다")
    private String orgId;

    @Size(max = 40, message = "기관오류코드는 40자 이내여야 합니다")
    private String orgErrorCode;

    @Size(max = 10, message = "HTTP코드는 10자 이내여야 합니다")
    private String errorHttpcode;

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
    private List<ErrorHandleAppRequest> handleApps;
}

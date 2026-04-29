package com.example.spider_admin.domain.errorcode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류코드 다국어 설명 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorDescCreateRequest {

    @NotBlank(message = "오류코드는 필수입니다")
    @Size(max = 10, message = "오류코드는 최대 10자입니다")
    private String errorCode;

    @NotBlank(message = "로케일코드는 필수입니다")
    @Size(max = 5, message = "로케일코드는 최대 5자입니다")
    private String localeCode;

    @Size(max = 200, message = "에러 제목은 최대 200자입니다")
    private String errorTitle;

    @Size(max = 200, message = "PB 에러 제목은 최대 200자입니다")
    private String pbErrorTitle;

    @Size(max = 200, message = "기타 에러 제목은 최대 200자입니다")
    private String etcErrorTitle;

    @Size(max = 4000, message = "에러 원인 설명은 최대 4000자입니다")
    private String errorCauseDesc;

    @Size(max = 4000, message = "에러 안내 설명은 최대 4000자입니다")
    private String errorGuideDesc;

    @Size(max = 500, message = "도움말 페이지 URL은 최대 500자입니다")
    private String helpPageUrl;

    @Size(max = 4000, message = "IBS 에러 안내 설명은 최대 4000자입니다")
    private String ibsErrorGuideDesc;

    @Size(max = 4000, message = "CMS 에러 안내 설명은 최대 4000자입니다")
    private String cmsErrorGuideDesc;

    @Size(max = 4000, message = "기타 에러 안내 설명은 최대 4000자입니다")
    private String etcErrorGuideDesc;
}

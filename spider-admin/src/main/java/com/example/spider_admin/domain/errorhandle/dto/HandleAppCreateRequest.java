package com.example.spider_admin.domain.errorhandle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 핸들러 APP 등록 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandleAppCreateRequest {

    @NotBlank(message = "핸들러APP ID는 필수입니다")
    @Size(max = 50, message = "핸들러APP ID는 50자 이내여야 합니다")
    private String handleAppId;

    @NotBlank(message = "핸들러APP명은 필수입니다")
    @Size(max = 50, message = "핸들러APP명은 50자 이내여야 합니다")
    private String handleAppName;

    @NotBlank(message = "핸들러APP설명은 필수입니다")
    @Size(max = 200, message = "핸들러APP설명은 200자 이내여야 합니다")
    private String handleAppDesc;

    @Size(max = 1000, message = "시스템파라미터값은 1000자 이내여야 합니다")
    private String sysParamValue;

    @Size(max = 2000, message = "파라미터설명은 2000자 이내여야 합니다")
    private String paramDesc;

    @Size(max = 100, message = "핸들러APP파일은 100자 이내여야 합니다")
    private String handleAppFile;
}

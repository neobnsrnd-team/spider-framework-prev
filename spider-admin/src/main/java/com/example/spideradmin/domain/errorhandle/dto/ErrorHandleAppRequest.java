package com.example.spideradmin.domain.errorhandle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류별 핸들러 APP 매핑 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorHandleAppRequest {

    @NotBlank(message = "핸들러APP ID는 필수입니다")
    @Size(max = 50, message = "핸들러APP ID는 50자 이내여야 합니다")
    private String handleAppId;

    @Size(max = 1000, message = "사용자파라미터값은 1000자 이내여야 합니다")
    private String userParamValue;
}

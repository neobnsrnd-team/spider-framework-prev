package com.example.spideradmin.domain.errorhandle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류코드-처리APP 매핑 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorHandleAppCreateRequest {

    @NotBlank(message = "오류코드는 필수입니다")
    @Size(max = 10, message = "오류코드는 최대 10자입니다")
    private String errorCode;

    @NotBlank(message = "처리APP ID는 필수입니다")
    @Size(max = 20, message = "처리APP ID는 최대 20자입니다")
    private String handleAppId;

    @Size(max = 200, message = "사용자 파라미터 값은 최대 200자입니다")
    private String userParamValue;
}

package com.example.spider_admin.domain.errorhistory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류 발생 이력 등록 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorHisCreateRequest {

    @NotBlank(message = "오류코드는 필수입니다")
    @Size(max = 40, message = "오류코드는 40자 이내여야 합니다")
    private String errorCode;

    @Size(max = 20, message = "오류 일련번호는 20자 이내여야 합니다")
    private String errorSerNo;

    @Size(max = 50, message = "고객 사용자 ID는 50자 이내여야 합니다")
    private String custUserId;

    @Size(max = 4000, message = "오류 메시지는 4000자 이내여야 합니다")
    private String errorMessage;

    @Size(max = 14, message = "오류 발생 일시는 14자 이내여야 합니다")
    private String errorOccurDtime;

    @Size(max = 500, message = "오류 URL은 500자 이내여야 합니다")
    private String errorUrl;

    private String errorTrace;

    @Size(max = 50, message = "에러 인스턴스 ID는 50자 이내여야 합니다")
    private String errorInstanceId;
}

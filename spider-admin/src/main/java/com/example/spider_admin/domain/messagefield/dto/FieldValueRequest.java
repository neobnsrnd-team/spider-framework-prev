package com.example.spider_admin.domain.messagefield.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 필드 ID와 입력값을 담는 DTO
 * 클라이언트에서 서버로 필드별 입력값을 전송할 때 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldValueRequest {

    /**
     * 전문 필드 ID
     */
    @NotBlank(message = "필드 ID는 필수입니다")
    private String fieldId;

    /**
     * 사용자 입력 값 (빈 값 허용)
     */
    private String value;
}

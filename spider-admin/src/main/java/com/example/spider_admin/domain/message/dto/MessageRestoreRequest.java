package com.example.spider_admin.domain.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message 복원 요청 DTO
 * POST /api/messages/{messageId}/restore 요청에 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRestoreRequest {

    @NotBlank(message = "기관 ID는 필수입니다")
    private String orgId;

    @NotNull(message = "복원할 버전은 필수입니다")
    private Integer version;
}

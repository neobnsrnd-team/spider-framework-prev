package com.example.spideradmin.domain.message.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message 백업 요청 DTO
 * POST /api/messages/{messageId}/backup 요청에 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageBackupRequest {

    @NotBlank(message = "기관 ID는 필수입니다")
    private String orgId;

    private String historyReason;
}

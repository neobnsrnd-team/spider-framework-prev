package com.example.spideradmin.domain.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message 버전 목록 응답 DTO
 * 버전 드롭다운 표시용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageVersionResponse {

    private Integer version;

    private String historyReason;

    private String lastUpdateDtime;

    private String lastUpdateUserId;
}

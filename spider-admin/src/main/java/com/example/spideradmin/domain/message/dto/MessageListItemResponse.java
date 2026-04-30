package com.example.spideradmin.domain.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message 목록 조회 응답 DTO
 * GET /api/messages/page 응답에 사용
 * 목록 표시에 필요한 주요 필드만 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageListItemResponse {

    private String orgId;
    private String messageId;
    private String messageName;
    private String messageDesc;
    private String messageType;
    private String parentMessageId;
    private String headerYn;
    private String requestYn;
    private String lockYn;
    private Integer curVersion;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}

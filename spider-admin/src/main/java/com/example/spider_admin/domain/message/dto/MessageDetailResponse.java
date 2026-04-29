package com.example.spider_admin.domain.message.dto;

import com.example.spider_admin.domain.messagefield.dto.FieldListResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message 상세 조회 응답 DTO
 * GET /api/messages/{messageId}/detail 응답에 사용
 * 전문 정보 + 필드 목록 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDetailResponse {

    private String orgId;
    private String parentMessageId;
    private String messageId;
    private String messageName;
    private String messageDesc;
    private String logLevel;
    private String headerYn;
    private String messageType;
    private String requestYn;
    private String preLoadYn;
    private String bizDomain;

    private List<FieldListResponse> fields;
}

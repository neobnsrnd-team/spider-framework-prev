package com.example.spider_admin.domain.messageparsing.dto;

import com.example.spider_admin.domain.messagefield.dto.MessageFieldResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message 응답 DTO
 * GET /api/messages/{messageId}?include=fields 응답에 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private String orgId;
    private String messageId;
    private String messageName;
    private String messageDesc;
    private String messageType;
    private String parentMessageId;
    private String headerYn;
    private String requestYn;
    private String trxType;
    private String preLoadYn;
    private String logLevel;
    private String bizDomain;
    private String validationUseYn;
    private String lockYn;
    private Integer curVersion;
    private String lastUpdateDtime;
    private String lastUpdateUserId;

    /**
     * includeFields=true일 때만 포함되는 필드 목록
     */
    private List<MessageFieldResponse> fields;
}

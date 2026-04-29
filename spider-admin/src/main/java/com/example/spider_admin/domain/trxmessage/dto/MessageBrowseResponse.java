package com.example.spider_admin.domain.trxmessage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전문 조회(Browse) Response DTO
 * 전문 조회 모달에서 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageBrowseResponse {

    private String orgId;
    private String messageId;
    private String messageName;
    private String messageType;
    private String messageDesc;
    private String headerYn;
}

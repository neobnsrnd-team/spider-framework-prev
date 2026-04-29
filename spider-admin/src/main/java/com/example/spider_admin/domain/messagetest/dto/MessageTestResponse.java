package com.example.spider_admin.domain.messagetest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전문 테스트 케이스 Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageTestResponse {

    private Long testSno;
    private String userId;
    private String orgId;
    private String messageId;
    private String headerYn;
    private String xmlYn;
    private String testName;
    private String testDesc;
    private String testData;
    private String testData1;
    private String testData2;
    private String testData3;
    private String testData4;
    private String testGroupId;
    private String trxId;
    private String defaultProxyYn;
    private String testHeaderData;
    private String testHeaderUseYn;
    private String proxyField;
    private String proxyValue;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String testHeaderData1;
}

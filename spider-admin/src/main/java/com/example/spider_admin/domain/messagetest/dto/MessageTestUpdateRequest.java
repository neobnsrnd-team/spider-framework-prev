package com.example.spider_admin.domain.messagetest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전문 테스트 케이스 수정 Request DTO
 * PUT /api/message-test/{testSno}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageTestUpdateRequest {

    @NotNull(message = "테스트 일련번호는 필수입니다")
    private Long testSno;

    @Size(max = 20, message = "기관 ID는 20자 이내여야 합니다")
    private String orgId;

    @Size(max = 30, message = "메시지 ID는 30자 이내여야 합니다")
    private String messageId;

    @Size(max = 40, message = "거래 ID는 40자 이내여야 합니다")
    private String trxId;

    @Size(max = 50, message = "테스트 이름은 50자 이내여야 합니다")
    private String testName;

    @Size(max = 200, message = "테스트 설명은 200자 이내여야 합니다")
    private String testDesc;

    @Size(max = 1, message = "헤더 여부는 1자 이내여야 합니다")
    private String headerYn;

    @Size(max = 1, message = "XML 여부는 1자 이내여야 합니다")
    private String xmlYn;

    private String testData;

    @Size(max = 4000, message = "테스트 데이터1은 4000자 이내여야 합니다")
    private String testData1;

    @Size(max = 4000, message = "테스트 데이터2는 4000자 이내여야 합니다")
    private String testData2;

    @Size(max = 4000, message = "테스트 데이터3은 4000자 이내여야 합니다")
    private String testData3;

    @Size(max = 4000, message = "테스트 데이터4는 4000자 이내여야 합니다")
    private String testData4;

    @Size(max = 20, message = "테스트 그룹 ID는 20자 이내여야 합니다")
    private String testGroupId;

    @Size(max = 1, message = "기본 Proxy 여부는 1자 이내여야 합니다")
    private String defaultProxyYn;

    private String testHeaderData;

    @Size(max = 1, message = "테스트 헤더 사용 여부는 1자 이내여야 합니다")
    private String testHeaderUseYn;

    @Size(max = 50, message = "Proxy 필드는 50자 이내여야 합니다")
    private String proxyField;

    @Size(max = 100, message = "Proxy 값은 100자 이내여야 합니다")
    private String proxyValue;

    private String testHeaderData1;
}

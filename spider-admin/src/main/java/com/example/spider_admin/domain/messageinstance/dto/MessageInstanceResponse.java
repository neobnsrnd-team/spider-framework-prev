package com.example.spider_admin.domain.messageinstance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전문 내역 응답 DTO
 * FWK_MESSAGE_INSTANCE 테이블의 조회 결과를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageInstanceResponse {

    /**
     * 메시지 일련번호 (PK)
     */
    private String messageSno;

    /**
     * 거래 ID
     */
    private String trxId;

    /**
     * 기관 ID
     */
    private String orgId;

    /**
     * I/O 타입 코드 (I: 수동, O: 기동, Q: 요구, S: 응답)
     */
    private String ioType;

    /**
     * 요청/응답 유형
     */
    private String reqResType;

    /**
     * 메시지 ID
     */
    private String messageId;

    /**
     * 거래 추적 번호
     */
    private String trxTrackingNo;

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 로그 일시
     */
    private String logDtime;

    /**
     * 최종 로그 일시
     */
    private String lastLogDtime;

    /**
     * 최종 결과 코드
     */
    private String lastRtCode;

    /**
     * 인스턴스 ID
     */
    private String instanceId;

    /**
     * 재시도 거래 여부
     */
    private String retryTrxYn;

    /**
     * 메시지 데이터
     */
    private String messageData;

    /**
     * 거래 일시
     */
    private String trxDtime;

    /**
     * 채널 유형
     */
    private String channelType;

    /**
     * URI
     */
    private String uri;

    /**
     * 로그 데이터 1
     */
    private String logData;

    /**
     * 로그 데이터 2
     */
    private String logData2;

    /**
     * 로그 데이터 3
     */
    private String logData3;

    /**
     * 로그 데이터 4
     */
    private String logData4;

    /**
     * 테스트케이스 ID
     */
    private String testcaseId;

    /**
     * 테스트케이스 일련번호
     */
    private Long testcaseSno;

    /**
     * 성공 여부
     */
    private String successYn;
}

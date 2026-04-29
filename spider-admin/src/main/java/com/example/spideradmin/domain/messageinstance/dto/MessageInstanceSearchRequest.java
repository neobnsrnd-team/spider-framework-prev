package com.example.spideradmin.domain.messageinstance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전문 내역(거래추적로그) 검색 요청 DTO
 * 전문 내역 목록 조회 시 검색 조건을 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageInstanceSearchRequest {

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 거래 추적 번호
     */
    private String trxTrackingNo;

    /**
     * 글로벌 ID (인스턴스 ID)
     */
    private String globalId;

    /**
     * 기관 ID
     */
    private String orgId;

    /**
     * 기관 전문 ID (메시지 ID)
     */
    private String orgMessageId;

    /**
     * 거래 시작 일자 (YYYYMMDD)
     */
    private String trxDateFrom;

    /**
     * 거래 종료 일자 (YYYYMMDD)
     */
    private String trxDateTo;

    /**
     * 거래 시작 시간 (HHMM)
     */
    private String trxTimeFrom;

    /**
     * 거래 종료 시간 (HHMM)
     */
    private String trxTimeTo;
}

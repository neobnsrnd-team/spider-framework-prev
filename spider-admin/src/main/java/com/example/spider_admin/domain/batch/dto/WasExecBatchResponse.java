package com.example.spider_admin.domain.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WasExecBatch Response DTO
 * Represents WAS instance assignment for a batch app
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasExecBatchResponse {

    /**
     * 배치 애플리케이션 ID
     */
    private String batchAppId;

    /**
     * 인스턴스 ID
     */
    private String instanceId;

    /**
     * 사용 여부 (Y/N)
     */
    private String useYn;

    private String lastUpdateDtime;
    private String lastUpdateUserId;

    // 조인 필드 (조회용)
    /**
     * 인스턴스명
     */
    private String instanceName;

    /**
     * 인스턴스 설명
     */
    private String instanceDesc;

    /**
     * IP 주소
     */
    private String ip;

    /**
     * 포트 번호
     */
    private String port;

    /**
     * 인스턴스 타입
     */
    private String instanceType;
}

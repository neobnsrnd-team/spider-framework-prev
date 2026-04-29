package com.example.spideradmin.domain.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file BatchRunningResponse.java
 * @description batch-was 인스턴스에서 조회한 실행 중 배치 정보를 담는 응답 DTO.
 *     <p>batch-was의 GET /api/batch/running 응답을 역직렬화하여 Admin 측 필드를 추가한다.
 *     <p>connected=false 인 경우 WAS와 통신 불가 상태를 나타내며, instanceId 이외의 필드는 null 이다.
 *     <p>connected=true 이고 batchAppId 가 null 인 경우 WAS는 정상이나 실행 중인 배치가 없는 "대기 중" 상태다.
 * @example connected=true, 배치 실행 중:
 *     BatchRunningResponse.builder()
 *         .instanceId("PT11")
 *         .connected(true)
 *         .jobExecutionId(42L)
 *         .batchAppId("BATCH_001")
 *         .status("STARTED")
 *         .build();
 * @example connected=true, 대기 중 (실행 중인 배치 없음):
 *     BatchRunningResponse.builder()
 *         .instanceId("PT11")
 *         .connected(true)
 *         .build();
 * @example connected=false (통신 불가):
 *     BatchRunningResponse.builder()
 *         .instanceId("PT11")
 *         .connected(false)
 *         .build();
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRunningResponse {

    // ── WAS 응답 필드 (batch-was /api/batch/running 에서 역직렬화) ──

    /** 배치 실행 ID (Spring Batch JobExecution ID) */
    private Long jobExecutionId;

    /** 배치 앱 ID */
    private String batchAppId;

    /** 배치 앱 이름 */
    private String batchAppName;

    /** 배치 앱 파일명 */
    private String batchAppFileName;

    /** 크론 텍스트 (스케줄 표현식) */
    private String cronText;

    /** 배치 기준 일자 (yyyyMMdd) */
    private String batchDate;

    /** 배치 시작 시각 (ISO-8601 또는 HH:mm:ss 형식) */
    private String startTime;

    /** 배치 실행 상태 (STARTED, STOPPING 등 Spring Batch BatchStatus 값) */
    private String status;

    // ── Admin 추가 필드 ──

    /**
     * 응답을 반환한 WAS 인스턴스 ID.
     * connected=false 인 경우에도 어느 인스턴스와 통신 실패했는지 식별하기 위해 항상 설정된다.
     */
    private String instanceId;

    /**
     * WAS 통신 성공 여부.
     * true: WAS 응답 정상 / false: WAS 통신 불가 (나머지 WAS 필드는 null)
     */
    private Boolean connected;
}

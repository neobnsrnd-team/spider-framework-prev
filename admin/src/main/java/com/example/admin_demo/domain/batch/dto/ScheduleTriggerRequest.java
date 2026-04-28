package com.example.admin_demo.domain.batch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 스케줄 즉시 실행 요청 DTO.
 * Admin에서 특정 WAS 인스턴스의 Quartz Job을 즉시 트리거할 때 사용한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleTriggerRequest {

    /** 실행 대상 WAS 인스턴스 ID */
    @NotBlank(message = "WAS 인스턴스 ID는 필수입니다")
    private String instanceId;

    /** 배치 기준일 (YYYYMMDD). null이면 WAS에서 당일로 자동 설정 */
    private String batchDate;
}

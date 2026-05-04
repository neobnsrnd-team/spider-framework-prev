package com.example.spideradmin.domain.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file BatchStopRequest.java
 * @description 실행 중 배치 강제 종료 요청 DTO.
 *     <p>Admin이 특정 WAS 인스턴스의 배치를 강제 종료할 때 사용한다.
 *     instanceId로 대상 WAS를 식별하고, jobExecutionId로 종료할 배치 실행을 특정한다.
 * @param instanceId 강제 종료 요청을 보낼 WAS 인스턴스 ID (필수)
 * @param jobExecutionId 종료 대상 Spring Batch JobExecution ID (필수)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStopRequest {

    /** 강제 종료 요청을 보낼 WAS 인스턴스 ID */
    @NotBlank(message = "instanceId는 필수입니다.")
    private String instanceId;

    /** 종료 대상 Spring Batch JobExecution ID */
    @NotNull(message = "jobExecutionId는 필수입니다.")
    private Long jobExecutionId;
}

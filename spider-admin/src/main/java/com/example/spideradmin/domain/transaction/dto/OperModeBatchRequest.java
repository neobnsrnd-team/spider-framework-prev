package com.example.spideradmin.domain.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 운영모드 일괄 수정 요청 DTO
 * - 모든 거래에 대해 일괄 적용
 * - operModeType: D(개발), R(리얼), T(테스트), null(전체/초기화)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperModeBatchRequest {

    private String operModeType;
}

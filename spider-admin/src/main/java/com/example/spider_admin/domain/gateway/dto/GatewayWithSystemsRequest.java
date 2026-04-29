package com.example.spider_admin.domain.gateway.dto;

import com.example.spider_admin.domain.gwsystem.dto.SystemBatchRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Gateway와 System 정보를 함께 저장하기 위한 통합 Request DTO
 * 원자성 보장을 위해 단일 트랜잭션으로 처리
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayWithSystemsRequest {

    @NotNull(message = "Gateway 정보는 필수입니다")
    @Valid
    private GatewayUpsertRequest gateway;

    @Valid
    private SystemBatchRequest systems;
}

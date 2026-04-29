package com.example.spideradmin.domain.listener.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 검색 셀렉트 옵션 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasGatewayStatusOptionsResponse {
    private List<SimpleResponse> instances;
    private List<SimpleResponse> gateways;
    private List<SimpleResponse> operModes;
}

package com.example.spider_admin.domain.gateway.dto;

import com.example.spider_admin.domain.gwsystem.dto.SystemResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayDetailResponse {

    private GatewayResponse gateway;
    private List<SystemResponse> systems;
}

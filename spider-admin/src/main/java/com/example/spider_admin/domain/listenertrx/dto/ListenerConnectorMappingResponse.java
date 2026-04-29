package com.example.spider_admin.domain.listenertrx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListenerConnectorMappingResponse {

    private String listenerGwId;
    private String listenerSystemId;
    private String identifier;
    private String connectorGwId;
    private String connectorSystemId;
    private String description;
}

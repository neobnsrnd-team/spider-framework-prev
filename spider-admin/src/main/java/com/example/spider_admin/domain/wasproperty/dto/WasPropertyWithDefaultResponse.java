package com.example.spider_admin.domain.wasproperty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasPropertyWithDefaultResponse {

    private String instanceId;
    private String propertyGroupId;
    private String propertyId;
    private String propertyValue;
    private String propertyDesc;
    private Integer curVersion;
    private String defaultValue;
    private String propertyName;
    private String dataType;
}

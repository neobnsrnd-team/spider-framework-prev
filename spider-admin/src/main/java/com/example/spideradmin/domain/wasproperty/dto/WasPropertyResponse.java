package com.example.spideradmin.domain.wasproperty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasPropertyResponse {

    private String instanceId;
    private String propertyGroupId;
    private String propertyId;
    private String propertyValue;
    private String propertyDesc;
    private Integer curVersion;
}

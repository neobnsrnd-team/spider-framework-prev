package com.example.spider_admin.domain.wasproperty.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WasPropertyCompareResponse {

    private String instanceId1;
    private String instanceId2;

    private List<PropertyCompareItem> items;
    private int differentCount;
    private int totalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyCompareItem {
        private String propertyGroupId;
        private String propertyId;
        private String defaultValue;
        private String compareResult; // "SAME", "DIFFERENT"
        private String value1;
        private String value2;
    }
}

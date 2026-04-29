package com.example.spider_admin.domain.org.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgResponse {

    private String orgId;
    private String orgName;
    private String orgDesc;
    private String startTime;
    private String endTime;
    private String xmlRootTag;
}

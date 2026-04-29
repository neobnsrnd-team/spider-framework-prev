package com.example.spider_admin.domain.bizapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Biz App 응답 DTO */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BizAppResponse {

    private String bizAppId;
    private String bizAppName;
    private String bizAppDesc;
    private String dupCheckYn;
    private String queName;
    private String queNameDisplay;
    private String logYn;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}

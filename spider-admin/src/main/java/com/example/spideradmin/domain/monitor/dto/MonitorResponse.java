package com.example.spideradmin.domain.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitorResponse {
    private String monitorId;
    private String monitorName;
    private String monitorQuery;
    private String alertCondition;
    private String alertMessage;
    private String refreshTerm;
    private String detailQuery;
    private String useYn;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}

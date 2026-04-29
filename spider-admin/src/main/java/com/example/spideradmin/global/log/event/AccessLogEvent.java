package com.example.spideradmin.global.log.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessLogEvent {

    private String traceId;
    private String phase;
    private String httpMethod;
    private String accessUrl;
    private String userId;
    private String accessIp;
    private String accessDtime;
    private String data;
    private int status;
    private long durationMs;
    private String resultMessage;
    private String errorMessage;
}

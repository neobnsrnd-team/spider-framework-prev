package com.example.spider_admin.global.log.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.logging.LogLevel;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogEvent {

    private String traceId;
    private String errorCode;
    private String errorMessage;
    private String errorTrace;
    private String userId;
    private String accessUrl;
    private String httpMethod;
    private String accessIp;
    private String errorOccurDtime;
    private LogLevel logLevel;
}

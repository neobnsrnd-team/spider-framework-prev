package com.example.spideradmin.global.log.listener;

import com.example.spideradmin.global.log.event.AccessLogEvent;
import com.example.spideradmin.global.log.event.ErrorLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Slf4jLogListener {

    @EventListener
    public void onAccessLog(AccessLogEvent event) {
        if ("REQ".equals(event.getPhase())) {
            log.info(
                    "[{}] → REQ {} {} | {} | {}",
                    event.getTraceId(),
                    event.getHttpMethod(),
                    event.getAccessUrl(),
                    event.getUserId(),
                    event.getAccessIp());
        } else if ("ERROR".equals(event.getResultMessage())) {
            log.warn(
                    "[{}] ← RES {} {} | {} | {}ms | ERROR | {}",
                    event.getTraceId(),
                    event.getHttpMethod(),
                    event.getAccessUrl(),
                    event.getStatus(),
                    event.getDurationMs(),
                    event.getErrorMessage());
        } else {
            log.info(
                    "[{}] ← RES {} {} | {} | {}ms | SUCCESS | {}",
                    event.getTraceId(),
                    event.getHttpMethod(),
                    event.getAccessUrl(),
                    event.getStatus(),
                    event.getDurationMs(),
                    event.getData());
        }
    }

    @EventListener
    public void onErrorLog(ErrorLogEvent event) {
        String code = event.getErrorCode() != null ? event.getErrorCode() : "UNHANDLED";
        String message = "[{}] ⚠ {} {} | {} | {}";
        Object[] args = {event.getTraceId(), code, event.getAccessUrl(), event.getErrorMessage(), event.getUserId()};

        switch (event.getLogLevel()) {
            case DEBUG -> log.debug(message, args);
            case INFO -> log.info(message, args);
            case WARN -> log.warn(message, args);
            default -> log.error(message, args);
        }
    }
}

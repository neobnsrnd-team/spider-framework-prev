package com.example.spideradmin.global.log.listener;

import com.example.spideradmin.domain.adminhistory.mapper.AdminActionLogMapper;
import com.example.spideradmin.global.log.event.AccessLogEvent;
import com.example.spideradmin.global.util.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "log.dest.rdb.access", havingValue = "true", matchIfMissing = true)
public class RdbAccessLogListener {

    private static final int MAX_INPUT_DATA_BYTES = 4000;
    private final AdminActionLogMapper adminActionLogMapper;
    private final ObjectMapper objectMapper;

    @Async("logExecutor")
    @EventListener
    public void onAccessLog(AccessLogEvent event) {
        try {
            String inputData = buildInputData(event);
            String accessUrl = "[" + event.getHttpMethod() + "] " + event.getAccessUrl();

            adminActionLogMapper.insert(
                    event.getUserId(),
                    event.getAccessDtime(),
                    event.getAccessIp(),
                    accessUrl,
                    inputData,
                    event.getResultMessage());
        } catch (Exception e) {
            log.error("Failed to save access log: userId={}, url={}", event.getUserId(), event.getAccessUrl(), e);
        }
    }

    private String buildInputData(AccessLogEvent event) throws JsonProcessingException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("traceId", event.getTraceId());
        map.put("phase", event.getPhase());

        if ("RES".equals(event.getPhase())) {
            map.put("status", event.getStatus());
            map.put("duration", event.getDurationMs());
        }

        if (event.getData() != null && !event.getData().isEmpty()) {
            map.put("data", event.getData());
        }
        if (event.getErrorMessage() != null) {
            map.put("errorMessage", event.getErrorMessage());
        }

        // 직렬화 후 최종 바이트 기준으로 잘라낸다.
        // 사전 예산 계산 방식은 JSON 직렬화 시 특수문자 이스케이핑(" → \")으로
        // 최종 바이트가 예산을 초과하는 ORA-01461 버그가 있었다.
        String json = objectMapper.writeValueAsString(map);
        return StringUtil.truncateBytesWithMarker(json, MAX_INPUT_DATA_BYTES);
    }
}

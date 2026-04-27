package com.example.admin_demo.global.client;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * spider-link 로그 레벨 동기화 클라이언트.
 *
 * <p>Admin에서 로그 레벨·Additivity 변경 후 spider-link에 동일한 변경을 실시간 반영하기 위해 호출한다.
 * 호출 실패 시 Admin 응답에는 영향을 주지 않고 경고 로그만 출력한다 (트랜잭션 분리).</p>
 *
 * <p>설정: {@code spiderlink.log-level-url}
 * (기본값: {@code http://localhost:8082/api/internal/log})</p>
 */
@Slf4j
@Component
public class SpiderLogLevelClient {

    private final RestClient restClient;
    private final String logLevelUrl;

    public SpiderLogLevelClient(
            @Value("${spiderlink.log-level-url:http://localhost:8082/api/internal/log}") String logLevelUrl) {
        this.logLevelUrl = logLevelUrl;
        this.restClient = RestClient.create();
    }

    /**
     * spider-link의 로그 레벨을 변경한다.
     *
     * <p>{@code level}이 null이면 상속(명시적 레벨 제거)으로 처리된다.
     * 실패 시 경고 로그만 출력하고 예외를 전파하지 않는다.</p>
     *
     * @param logName 변경 대상 로거 이름
     * @param level   변경할 레벨 (ERROR/WARN/INFO/DEBUG/TRACE/OFF), null이면 상속
     */
    public void syncLevel(String logName, String level) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("logName", logName);
            if (level != null) {
                body.put("level", level);
            }

            restClient
                    .post()
                    .uri(logLevelUrl + "/level")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[SpiderLogLevelClient] 레벨 동기화 성공: logName={}, level={}", logName, level);
        } catch (Exception e) {
            // 동기화 실패는 Admin 응답에 영향 없음 — 경고 로그만 출력
            log.warn("[SpiderLogLevelClient] 레벨 동기화 실패 (spider-link 미기동 또는 네트워크 오류): logName={}, error={}",
                    logName, e.getMessage());
        }
    }

    /**
     * spider-link의 Additivity를 변경한다.
     *
     * <p>실패 시 경고 로그만 출력하고 예외를 전파하지 않는다.</p>
     *
     * @param logName    변경 대상 로거 이름
     * @param additivity "Y" 또는 "N"
     */
    public void syncAdditivity(String logName, String additivity) {
        try {
            restClient
                    .post()
                    .uri(logLevelUrl + "/additivity")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("logName", logName, "additivity", additivity))
                    .retrieve()
                    .toBodilessEntity();

            log.info("[SpiderLogLevelClient] Additivity 동기화 성공: logName={}, additivity={}", logName, additivity);
        } catch (Exception e) {
            log.warn("[SpiderLogLevelClient] Additivity 동기화 실패 (spider-link 미기동 또는 네트워크 오류): logName={}, error={}",
                    logName, e.getMessage());
        }
    }
}

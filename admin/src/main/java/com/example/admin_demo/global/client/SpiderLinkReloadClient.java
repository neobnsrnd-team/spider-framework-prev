package com.example.admin_demo.global.client;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * spider-link SQL Reload API 호출 클라이언트.
 *
 * <p>admin에서 SQL 저장·수정 후 spider-link에 변경을 실시간 반영하기 위해 호출한다.
 * 호출 실패 시 저장 결과에는 영향을 주지 않고 경고 로그만 출력한다 (트랜잭션 분리).</p>
 *
 * <p>설정: {@code spiderlink.reload-url} (기본값: {@code http://localhost:8082/api/internal/sql/reload})</p>
 */
@Slf4j
@Component
public class SpiderLinkReloadClient {

    private final RestClient restClient;
    private final String reloadUrl;

    public SpiderLinkReloadClient(
            @Value("${spiderlink.reload-url:http://localhost:8082/api/internal/sql/reload}") String reloadUrl) {
        this.reloadUrl = reloadUrl;
        this.restClient = RestClient.create();
    }

    /**
     * 단건 SQL을 spider-link에 리로드한다.
     *
     * <p>USE_YN이 'N'이면 해당 statement를 제거하고, 그 외에는 DB에서 재조회해 재등록한다.
     * 실패 시 경고 로그만 출력하고 예외를 전파하지 않는다.</p>
     *
     * @param queryId 리로드할 QUERY_ID
     * @param useYn   현재 USE_YN 값 ('Y' 또는 'N')
     */
    public void reload(String queryId, String useYn) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("queryId", queryId);
            if ("N".equalsIgnoreCase(useYn)) {
                body.put("useYn", "N");
            }

            restClient.post()
                    .uri(reloadUrl)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[SpiderLinkReloadClient] 리로드 성공: queryId={}, useYn={}", queryId, useYn);
        } catch (Exception e) {
            // Reload 실패는 저장 결과에 영향 없음 — 경고 로그만 출력
            log.warn("[SpiderLinkReloadClient] 리로드 실패 (spider-link 미기동 또는 네트워크 오류): queryId={}, error={}",
                    queryId, e.getMessage());
        }
    }

    /**
     * FWK_SQL_QUERY 전체를 spider-link에 리로드한다.
     *
     * <p>실패 시 경고 로그만 출력하고 예외를 전파하지 않는다.</p>
     */
    public void reloadAll() {
        try {
            restClient.post()
                    .uri(reloadUrl)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .toBodilessEntity();

            log.info("[SpiderLinkReloadClient] 전체 리로드 성공");
        } catch (Exception e) {
            log.warn("[SpiderLinkReloadClient] 전체 리로드 실패: {}", e.getMessage());
        }
    }
}

package com.example.spiderlink.domain.sqlquery;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FWK_SQL_QUERY 동적 SQL 리로드 내부 API.
 *
 * <p>어드민에서 SQL 수정 저장 후 WAS 재시작 없이 실시간 반영하기 위해 호출한다.
 * 외부 노출 차단을 위해 {@code /api/internal/**} 경로는 localhost 전용
 * {@link InternalApiInterceptor}가 보호한다.</p>
 *
 * <pre>{@code
 * // 단건 리로드
 * POST /api/internal/sql/reload
 * { "queryId": "SELECT_USER" }
 *
 * // 전체 리로드
 * POST /api/internal/sql/reload
 * {}
 * }</pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/sql")
@RequiredArgsConstructor
public class SqlQueryReloadController {

    private final SqlQueryLoader sqlQueryLoader;

    /**
     * SQL 리로드 엔드포인트.
     *
     * <p>{@code queryId}가 있으면 단건 리로드, 없으면 전체 리로드를 수행한다.
     * {@code useYn = "N"}이 함께 전달되면 해당 statement를 제거한다.</p>
     *
     * @param body {@code queryId} (선택), {@code useYn} (선택)
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reload(
            @RequestBody(required = false) Map<String, String> body) {

        String queryId = body != null ? body.get("queryId") : null;
        String useYn   = body != null ? body.get("useYn")   : null;

        try {
            if (queryId != null && !queryId.isBlank()) {
                if ("N".equalsIgnoreCase(useYn)) {
                    // USE_YN='N' 변경 — statement 제거
                    sqlQueryLoader.removeByQueryId(queryId);
                    log.info("[SqlQueryReloadController] statement 제거 요청: {}", queryId);
                    return ok("removed", queryId);
                } else {
                    // 단건 리로드
                    sqlQueryLoader.reloadById(queryId);
                    return ok("reloaded", queryId);
                }
            } else {
                // 전체 리로드
                sqlQueryLoader.reloadAll();
                return ok("reloaded-all", null);
            }
        } catch (IllegalArgumentException e) {
            log.warn("[SqlQueryReloadController] 리로드 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[SqlQueryReloadController] 리로드 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, Object>> ok(String action, String queryId) {
        Map<String, Object> body = queryId != null
                ? Map.of("success", true, "action", action, "queryId", queryId)
                : Map.of("success", true, "action", action);
        return ResponseEntity.ok(body);
    }
}

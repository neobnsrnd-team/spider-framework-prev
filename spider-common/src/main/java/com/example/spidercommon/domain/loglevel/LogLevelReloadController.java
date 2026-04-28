package com.example.spidercommon.domain.loglevel;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Logback 로거 레벨·Additivity 런타임 변경 내부 API.
 *
 * <p>Admin에서 로그 레벨 변경 후 HTTP WAS에 실시간 반영하기 위해 호출한다.
 * 실제 변경 로직은 {@link LogLevelApplier}에 위임한다.
 * {@code /api/internal/**} 경로는 {@link com.example.spidercommon.config.InternalApiInterceptor}가
 * 허용된 IP에서만 접근 가능하도록 보호한다.</p>
 *
 * <pre>{@code
 * // 레벨 변경 (level=null 이면 상속으로 초기화)
 * POST /api/internal/log/level
 * { "logName": "com.example", "level": "DEBUG" }
 *
 * // Additivity 변경
 * POST /api/internal/log/additivity
 * { "logName": "com.example", "additivity": "N" }
 * }</pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/log")
@RequiredArgsConstructor
public class LogLevelReloadController {

    private final LogLevelApplier logLevelApplier;

    /**
     * 로그 레벨 변경 엔드포인트.
     *
     * @param body {@code logName} (필수), {@code level} (선택 — 없으면 상속)
     */
    @PostMapping("/level")
    public ResponseEntity<Map<String, Object>> updateLevel(
            @RequestBody(required = false) Map<String, String> body) {

        String logName = body != null ? body.get("logName") : null;
        String levelStr = body != null ? body.get("level") : null;

        if (logName == null || logName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "logName은 필수입니다"));
        }

        try {
            logLevelApplier.applyLevel(logName, levelStr);
            String appliedLevel = (levelStr == null || levelStr.isBlank()) ? "inherited" : levelStr;
            return ResponseEntity.ok(Map.of("success", true, "logName", logName, "level", appliedLevel));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[LogLevelReloadController] 레벨 변경 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Additivity 변경 엔드포인트.
     *
     * @param body {@code logName} (필수), {@code additivity} — "Y" 또는 "N" (필수)
     */
    @PostMapping("/additivity")
    public ResponseEntity<Map<String, Object>> updateAdditivity(
            @RequestBody(required = false) Map<String, String> body) {

        String logName = body != null ? body.get("logName") : null;
        String additivity = body != null ? body.get("additivity") : null;

        if (logName == null || logName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "logName은 필수입니다"));
        }
        if (!"Y".equals(additivity) && !"N".equals(additivity)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "additivity는 Y 또는 N이어야 합니다"));
        }

        try {
            logLevelApplier.applyAdditivity(logName, additivity);
            return ResponseEntity.ok(Map.of("success", true, "logName", logName, "additivity", additivity));
        } catch (Exception e) {
            log.error("[LogLevelReloadController] Additivity 변경 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}

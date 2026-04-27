package com.example.spiderlink.domain.loglevel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Logback 로거 레벨·Additivity 런타임 변경 내부 API.
 *
 * <p>Admin에서 로그 레벨 변경 후 spider-link에 실시간 반영하기 위해 호출한다.
 * {@code /api/internal/**} 경로는 {@link com.example.spiderlink.config.InternalApiInterceptor}가
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
public class LogLevelReloadController {

    /**
     * 로그 레벨 변경 엔드포인트.
     *
     * <p>{@code level}이 null 또는 빈 문자열이면 명시적 레벨을 제거하여 부모 로거 레벨을 상속한다.
     * 알 수 없는 레벨 문자열은 400을 반환한다.</p>
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
            Logger logger = getOrCreateLogger(logName);

            if (levelStr == null || levelStr.isBlank()) {
                // 빈 값 = 상속 → 명시적 레벨 제거
                logger.setLevel(null);
                log.info("[LogLevelReloadController] 로그 레벨 변경: {} → 상속 (null)", logName);
                return ResponseEntity.ok(Map.of("success", true, "logName", logName, "level", "inherited"));
            }

            // Level.toLevel(str, null): null 반환 시 유효하지 않은 레벨
            Level level = Level.toLevel(levelStr, null);
            if (level == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "유효하지 않은 로그 레벨: " + levelStr));
            }

            logger.setLevel(level);
            log.info("[LogLevelReloadController] 로그 레벨 변경: {} → {}", logName, levelStr);
            return ResponseEntity.ok(Map.of("success", true, "logName", logName, "level", levelStr));

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
            Logger logger = getOrCreateLogger(logName);
            logger.setAdditive("Y".equals(additivity));
            log.info("[LogLevelReloadController] Additivity 변경: {} → {}", logName, additivity);
            return ResponseEntity.ok(Map.of("success", true, "logName", logName, "additivity", additivity));

        } catch (Exception e) {
            log.error("[LogLevelReloadController] Additivity 변경 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private Logger getOrCreateLogger(String logName) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger existing = ctx.exists(logName);
        // ctx.exists()는 이미 생성된 로거만 반환 → 없으면 getLogger()로 Logback에 생성 요청
        if (existing != null) {
            return existing;
        }
        return (Logger) LoggerFactory.getLogger(logName);
    }
}

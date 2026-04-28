package com.example.spiderlink.domain.management;

import com.example.spiderlink.domain.management.executor.ManagementExecutor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP WAS 관리 명령 수신 컨트롤러.
 *
 * <p>Admin에서 HTTP WAS에 Reload 명령을 전송할 때 사용하는 내부 엔드포인트다.
 * TCP WAS의 {@link ManagementReloadCommandHandler}와 동일한 {@link ManagementExecutor} 패턴으로
 * {@code gubun} 값에 따라 적합한 실행기를 선택하여 처리한다.</p>
 *
 * <p>{@code /api/management/**} 경로는 {@link com.example.spiderlink.config.InternalApiInterceptor}가
 * 허용된 IP에서만 접근 가능하도록 보호한다.</p>
 *
 * <pre>{@code
 * POST /api/management/reload
 * {
 *   "gubun": "log_config_level",
 *   "logName": "com.example.service",
 *   "level": "DEBUG"
 * }
 * }</pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
public class ManagementReloadHttpController {

    private final List<ManagementExecutor> executors;

    /**
     * 관리 명령 수신 엔드포인트.
     *
     * @param body {@code gubun} (필수) + 명령별 추가 파라미터
     * @return 처리 결과 (success, message)
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reload(
            @RequestBody(required = false) Map<String, String> body) {

        if (body == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "요청 바디가 없습니다"));
        }

        String gubun = body.get("gubun");
        if (gubun == null || gubun.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "gubun은 필수입니다"));
        }

        ManagementExecutor executor = executors.stream()
                .filter(e -> e.supports(gubun))
                .findFirst()
                .orElse(null);

        if (executor == null) {
            log.warn("[ManagementReloadHttpController] 지원하지 않는 gubun: {}", gubun);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "지원하지 않는 gubun: " + gubun));
        }

        try {
            // ManagementExecutor.execute()는 Map<String, Object>를 받으므로 변환
            Map<String, Object> params = new HashMap<>(body);
            Map<String, Object> result = executor.execute(params);
            log.info("[ManagementReloadHttpController] 관리 명령 완료: gubun={}", gubun);
            return ResponseEntity.ok(Map.of("success", true, "message", gubun + " 처리 완료", "data", result));
        } catch (IllegalArgumentException e) {
            log.warn("[ManagementReloadHttpController] 입력 오류: gubun={}, error={}", gubun, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[ManagementReloadHttpController] 오류: gubun={}, error={}", gubun, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}

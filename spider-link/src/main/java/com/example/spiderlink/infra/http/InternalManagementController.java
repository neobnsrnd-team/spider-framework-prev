package com.example.spiderlink.infra.http;

import com.example.spiderlink.infra.tcp.handler.MetaDrivenCommandHandler;
import com.example.spiderlink.infra.tcp.parser.MessageStructurePool;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin → WAS 운영정보 리로드 내부 API.
 *
 * <p>Admin의 ReloadService가 {@code POST /api/management/reload} 로 호출하며,
 * 요청 본문의 {@code gubun} 값에 따라 캐시를 무효화한다.</p>
 *
 * <pre>{@code
 * gubun=request_app_mapping → MetaDrivenCommandHandler 커맨드 캐시 갱신
 * gubun=message             → MessageStructurePool 전문 구조 캐시 초기화
 * (기타 gubun)              → 이 WAS에서 처리 불필요 → 성공 응답(no-op)
 * }</pre>
 *
 * <p>보안: {@link com.example.spiderlink.config.WebMvcConfig}의 인터셉터가
 * {@code /api/management/**} 경로를 허용 IP(기본: localhost)로만 제한한다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
@ConditionalOnBean(MetaDrivenCommandHandler.class)
public class InternalManagementController {

    private final MetaDrivenCommandHandler metaDrivenCommandHandler;

    /** 전문 구조 캐시 풀 — 없으면 고정길이 전문 미사용으로 간주, message 리로드 생략 */
    @Nullable
    private final MessageStructurePool messageStructurePool;

    /**
     * 운영정보 리로드 엔드포인트.
     *
     * @param body {@code gubun} 필드를 포함한 요청 본문
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reload(
            @RequestBody(required = false) Map<String, String> body) {

        String gubun = body != null ? body.get("gubun") : null;
        log.info("[InternalManagementController] reload 요청: gubun={}", gubun);

        try {
            if ("request_app_mapping".equals(gubun)) {
                // FWK_LISTENER_TRX_MESSAGE 변경 후 커맨드 캐시 갱신
                metaDrivenCommandHandler.refreshCommands();
                return ok("request_app_mapping");

            } else if ("message".equals(gubun)) {
                // FWK_MESSAGE / FWK_MESSAGE_FIELD 변경 후 전문 구조 캐시 초기화
                if (messageStructurePool != null) {
                    messageStructurePool.clear();
                    return ok("message");
                } else {
                    log.debug("[InternalManagementController] MessageStructurePool 빈 없음 — message 리로드 생략");
                    return ok("message(skipped-no-pool)");
                }

            } else {
                // 이 WAS에서 처리할 필요 없는 gubun (예: trx, code 등)
                log.debug("[InternalManagementController] 처리 불필요 gubun: {}", gubun);
                return ok("no-op");
            }
        } catch (Exception e) {
            log.error("[InternalManagementController] reload 처리 중 오류: gubun={}, error={}", gubun, e.getMessage(), e);
            // e.getMessage()가 null일 수 있으므로(NPE 등) 고정 메시지 반환 — 상세는 서버 로그로만 확인
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Internal Server Error"));
        }
    }

    private ResponseEntity<Map<String, Object>> ok(String action) {
        return ResponseEntity.ok(Map.of("success", true, "action", action));
    }
}

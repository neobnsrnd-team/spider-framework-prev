package com.example.bizchannel.web.controller;

import com.example.bizchannel.domain.notice.NoticeManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 공지사항 REST/SSE 컨트롤러.
 *
 * <p>{@code /api/notices/*} 경로의 요청을 처리한다.</p>
 *
 * <ul>
 *   <li>{@code GET /api/notices/sse} — 클라이언트 SSE 연결 등록 (인증 불필요)</li>
 *   <li>{@code POST /api/notices/sync} — 공지 업데이트 및 브로드캐스트 (X-Admin-Secret 필요)</li>
 *   <li>{@code POST /api/notices/end} — 공지 종료 (X-Admin-Secret 필요)</li>
 *   <li>{@code GET /api/notices/preview} — 현재 공지 조회 (인증 불필요)</li>
 * </ul>
 *
 * <p>SSE 상태(클라이언트 목록, 현재 공지)는 {@link NoticeManager} 가 관리한다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeManager noticeManager;

    /** 공지 관리 API 접근 제어용 어드민 시크릿 키 */
    @Value("${admin.secret:admin-secret}")
    private String adminSecret;

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/notices/sse
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * SSE 스트림 연결 엔드포인트.
     *
     * <p>클라이언트가 연결하면 {@link NoticeManager} 에 이미터를 등록하고,
     * 현재 활성 공지가 있으면 즉시 전송한다.
     * 연결 유지는 {@link SseEmitter}(Long.MAX_VALUE 타임아웃)가 담당한다.</p>
     *
     * @return SSE 이미터 (Spring MVC 가 응답 스트림으로 처리)
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeSse() {
        log.debug("[NoticeController] SSE client connect");
        return noticeManager.addClient();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/notices/sync
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 공지사항 동기화 및 실시간 브로드캐스트.
     *
     * <p>어드민 서버(또는 관리자 도구)가 호출하는 엔드포인트로,
     * {@code X-Admin-Secret} 헤더로 접근을 제한한다.</p>
     *
     * @param adminSecretHeader 요청 헤더의 어드민 시크릿 값
     * @param body              공지 데이터 ({@code notices, displayType, closeableYn, hideTodayYn})
     * @return 브로드캐스트 성공 여부
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncNotice(
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecretHeader,
            @RequestBody Map<String, Object> body) {

        if (!adminSecret.equals(adminSecretHeader)) {
            log.warn("[NoticeController] Notice sync rejected — invalid X-Admin-Secret");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "어드민 시크릿이 올바르지 않습니다."));
        }

        log.info("[NoticeController] Notice sync request: displayType={}", body.get("displayType"));
        noticeManager.broadcast(body);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/notices/end
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 공지사항 종료 처리.
     *
     * <p>현재 공지를 null 로 초기화하고, 모든 SSE 클라이언트에게
     * {@code "notice-end"} 이벤트를 브로드캐스트한다.</p>
     *
     * @param adminSecretHeader 요청 헤더의 어드민 시크릿 값
     * @return 종료 처리 결과
     */
    @PostMapping("/end")
    public ResponseEntity<Map<String, Object>> endNotice(
            @RequestHeader(value = "X-Admin-Secret", required = false) String adminSecretHeader) {

        if (!adminSecret.equals(adminSecretHeader)) {
            log.warn("[NoticeController] Notice end rejected — invalid X-Admin-Secret");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "어드민 시크릿이 올바르지 않습니다."));
        }

        log.info("[NoticeController] Notice end");
        // null 브로드캐스트 → NoticeManager 가 notice-end 이벤트 전송
        noticeManager.broadcast(null);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/notices/preview
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 현재 활성 공지사항 미리보기.
     *
     * <p>인증 없이 접근 가능하며, 현재 인메모리에 저장된 공지사항을 반환한다.
     * 공지가 없으면 빈 객체를 반환한다.</p>
     *
     * @return 현재 공지사항 Map, 없으면 빈 Map
     */
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewNotice() {
        Map<String, Object> currentNotice = noticeManager.getCurrentNotice();
        if (currentNotice == null) {
            // 빈 Map 대신 프론트엔드 NoticePayload 구조에 맞는 기본값을 반환한다.
            // 빈 Map을 반환하면 notices 필드가 undefined가 되어 EmergencyNoticeBanner에서 TypeError 발생
            return ResponseEntity.ok(Map.of(
                    "notices", List.of(),
                    "displayType", "N",
                    "closeableYn", "Y",
                    "hideTodayYn", "Y"
            ));
        }
        return ResponseEntity.ok(currentNotice);
    }
}

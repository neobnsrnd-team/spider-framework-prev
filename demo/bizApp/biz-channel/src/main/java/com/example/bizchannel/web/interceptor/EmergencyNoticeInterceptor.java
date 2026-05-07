package com.example.bizchannel.web.interceptor;

import com.example.bizchannel.domain.notice.NoticeManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;

/**
 * 긴급 점검 중 API 전면 차단 인터셉터.
 *
 * <p>공지의 {@code closeableYn = "N"}(닫기 버튼 미표시 = 크리티컬 장애)인 경우
 * SSE·미리보기 엔드포인트를 제외한 모든 {@code /api/**} 요청에 대해
 * HTTP 503 을 반환하고 처리를 중단한다.</p>
 *
 * <p>제외 경로는 {@link com.example.bizchannel.config.BizChannelConfig}에서 설정한다:
 * <ul>
 *   <li>{@code /api/notices/sse} — SSE 연결 유지 (공지 해제 이벤트 수신)</li>
 *   <li>{@code /api/notices/preview} — 브라우저 초기 로드 시 공지 상태 조회</li>
 *   <li>{@code /api/notices/sync} — Admin 공지 배포 (X-Admin-Secret 보호)</li>
 *   <li>{@code /api/notices/end} — Admin 공지 종료 (X-Admin-Secret 보호)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencyNoticeInterceptor implements HandlerInterceptor {

    private final NoticeManager noticeManager;
    private final ObjectMapper objectMapper;

    /**
     * 요청 전처리: 크리티컬 긴급공지 활성 여부를 확인하고, 활성이면 503으로 즉시 응답한다.
     *
     * @return 공지가 없거나 {@code closeableYn != "N"}이면 {@code true}(처리 계속),
     *         {@code closeableYn = "N"}이면 {@code false}(처리 중단)
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        Map<String, Object> notice = noticeManager.getCurrentNotice();
        if (notice == null) {
            return true;
        }

        Object rawCloseable = notice.get("closeableYn");
        Object rawDisplay   = notice.get("displayType");
        String closeableYn  = rawCloseable instanceof String s ? s : null;
        String displayType  = rawDisplay   instanceof String s ? s : null;

        // displayType=N(사용안함)이면 공지 비활성 상태 — 차단하지 않음
        if (!"N".equals(closeableYn) || "N".equals(displayType)) {
            return true;
        }

        // 크리티컬 긴급공지 활성 — 모든 API 차단
        log.warn("[EmergencyNoticeInterceptor] API blocked: closeableYn=N, uri={}", request.getRequestURI());

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "error", "긴급 점검 중입니다.",
                "closeableYn", "N"
        )));
        // preHandle에서 직접 응답을 작성할 때 버퍼를 즉시 클라이언트로 전송
        response.getWriter().flush();
        return false;
    }
}

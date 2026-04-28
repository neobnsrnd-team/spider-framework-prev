package com.example.bizchannel.web.interceptor;

import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * HTTP 거래 로그 기록 인터셉터.
 *
 * <p>Front → biz-channel HTTP 수신 시점에 UUID를 생성하여 {@code requestId} request 속성으로 저장하고,
 * {@link MessageInstanceRecorder}를 통해 {@code FWK_MESSAGE_INSTANCE}에 REQ 로그를 INSERT한다.
 * 응답 완료 후 {@code afterCompletion}에서 RES 로그를 INSERT한다.</p>
 *
 * <p>생성된 {@code requestId}는 Controller → {@link com.example.bizchannel.client.BizClient}까지
 * 전달되어 후속 TCP 구간과 동일한 {@code TRX_TRACKING_NO}로 연결된다.</p>
 *
 * <p>userId 결정 규칙:
 * <ul>
 *   <li>JWT 인증 요청: {@link com.example.bizchannel.global.auth.JwtAuthFilter}가
 *       설정한 {@code userId} request 속성 사용</li>
 *   <li>로그인 요청(JWT 없음) REQ: {@code "GUEST"}</li>
 *   <li>로그인 요청 RES(성공): {@link com.example.bizchannel.web.controller.AuthController}가
 *       설정한 {@code loginUserId} request 속성 사용</li>
 * </ul>
 * </p>
 *
 * <p>{@link MessageInstanceRecorder}가 없는 경우(JdbcTemplate 미존재) UUID 생성만 수행하고 로그 기록은 생략한다.</p>
 */
@Slf4j
@Component
public class HttpLoggingInterceptor implements HandlerInterceptor {

    /** JdbcTemplate 빈이 없으면 null — 로그 기록 생략 */
    @Nullable
    @Autowired(required = false)
    private MessageInstanceRecorder recorder;

    @Value("${server.port:18080}")
    private int serverPort;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = UUID.randomUUID().toString();
        // Controller에서 BizClient로 전달할 수 있도록 request 속성에 저장
        request.setAttribute("requestId", requestId);

        String uri = request.getRequestURI();
        String userId = resolveUserId(request, false);
        log.debug("[HttpLoggingInterceptor] HTTP REQ: requestId={} uri={} userId={}", requestId, uri, userId);
        if (recorder != null) {
            recorder.recordHttpRequest(requestId, uri, null, serverPort, userId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String requestId = (String) request.getAttribute("requestId");
        if (requestId == null) return;

        String uri = request.getRequestURI();
        boolean success = response.getStatus() < 400;
        // 로그인 성공 시 AuthController가 loginUserId를 설정하므로 RES에서는 이를 우선 사용
        String userId = resolveUserId(request, true);
        log.debug("[HttpLoggingInterceptor] HTTP RES: requestId={} uri={} status={} userId={}", requestId, uri, response.getStatus(), userId);
        if (recorder != null) {
            recorder.recordHttpResponse(requestId, uri, null, success, serverPort, userId);
        }
    }

    /**
     * request 속성에서 userId를 결정한다.
     *
     * @param includeLoginUserId true이면 loginUserId 폴백 포함 (afterCompletion 전용)
     * @return userId 문자열. JWT 미인증이면 "GUEST"
     */
    private String resolveUserId(HttpServletRequest request, boolean includeLoginUserId) {
        // JWT 인증된 요청: JwtAuthFilter가 설정한 userId
        String userId = (String) request.getAttribute("userId");
        if (userId != null && !userId.isBlank()) return userId;

        // 로그인 성공 RES: AuthController가 설정한 loginUserId (afterCompletion 시점에만 유효)
        if (includeLoginUserId) {
            String loginUserId = (String) request.getAttribute("loginUserId");
            if (loginUserId != null && !loginUserId.isBlank()) return loginUserId;
        }

        return "GUEST";
    }
}

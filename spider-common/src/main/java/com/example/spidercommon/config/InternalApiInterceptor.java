package com.example.spidercommon.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 내부 API 보안 인터셉터 — {@code /api/internal/**} 경로를 허용된 IP에서만 접근 가능하도록 제한한다.
 *
 * <p>허용 IP 목록은 {@code internal-api.allowed-ips} 프로퍼티로 관리한다.
 * Admin과 spider-common을 embed한 WAS가 서로 다른 서버에 배포된 경우
 * Admin 서버 IP를 목록에 추가하면 된다.</p>
 *
 * <pre>{@code
 * # application.yml
 * internal-api:
 *   allowed-ips:
 *     - 127.0.0.1
 *     - 192.168.1.10   # Admin 서버 내부망 IP
 * }</pre>
 */
@Slf4j
@Component
public class InternalApiInterceptor implements HandlerInterceptor {

    /** application.yml의 internal-api.allowed-ips 목록. 기본값은 loopback 3종. */
    private final List<String> allowedIps;

    public InternalApiInterceptor(
            @Value("${internal-api.allowed-ips:127.0.0.1,0:0:0:0:0:0:0:1,::1}")
            List<String> allowedIps) {
        this.allowedIps = allowedIps;
        log.info("[InternalApiInterceptor] 허용 IP 목록: {}", allowedIps);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String remoteAddr = request.getRemoteAddr();

        if (!allowedIps.contains(remoteAddr)) {
            log.warn("[InternalApiInterceptor] 외부 접근 차단: {} → {}", remoteAddr, request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Internal API only");
            return false;
        }

        return true;
    }
}

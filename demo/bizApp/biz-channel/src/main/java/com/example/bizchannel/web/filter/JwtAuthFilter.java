package com.example.bizchannel.web.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * JWT 액세스 토큰 인증 서블릿 필터.
 *
 * <p>{@code /api/*} 경로에서 JWT 검증이 필요 없는 공개 경로(로그인, 리프레시, SSE, 공지 등)는
 * 필터링을 건너뛰고, 나머지 보호된 경로에서는 {@code Authorization: Bearer <token>} 헤더를
 * 추출하여 JJWT 로 서명·만료 검증을 수행한다.</p>
 *
 * <p>검증 성공 시 {@code userId} 를 request 속성으로 설정하여
 * 이후 컨트롤러에서 {@code request.getAttribute("userId")} 로 참조할 수 있게 한다.</p>
 */
@Slf4j
@Component
public class JwtAuthFilter implements Filter {

    /** JWT 서명·검증에 사용할 비밀 키 */
    private final SecretKey accessKey;

    /**
     * JWT 인증을 건너뛸 공개 경로 집합.
     * 이 경로들은 토큰 없이 접근 가능하다.
     */
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/notices/sse",
            "/api/notices/sync",
            "/api/notices/end",
            "/api/notices/preview"
    );

    /**
     * JWT 인증을 건너뛸 공개 경로 prefix 목록.
     * 해당 prefix로 시작하는 모든 하위 경로에 대해 인증을 면제한다.
     */
    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/api/management/"   // Admin 내부 관리 API (IP 기반 접근 제어로 보호)
    );

    /**
     * @param jwtSecret application.yml 의 jwt.secret 값 (HMAC-SHA256 서명 키)
     */
    public JwtAuthFilter(@Value("${jwt.secret}") String jwtSecret) {
        // 문자열 비밀 키를 UTF-8 바이트로 변환하여 HMAC 키 생성
        this.accessKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 요청 경로에 따라 JWT 검증을 수행하거나 건너뛴다.
     *
     * @param request  HTTP 서블릿 요청
     * @param response HTTP 서블릿 응답
     * @param chain    필터 체인
     * @throws IOException      I/O 오류
     * @throws ServletException 서블릿 처리 오류
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        String path = httpReq.getRequestURI();

        // 공개 경로는 JWT 검증 없이 통과
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Authorization 헤더에서 Bearer 토큰 추출
        String authHeader = httpReq.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(httpRes, "인증 토큰이 없습니다.");
            return;
        }

        String token = authHeader.substring(7); // "Bearer " 이후 토큰 문자열

        try {
            // JJWT 0.12.x API: Jwts.parser() (parserBuilder() 는 deprecated)
            Claims claims = Jwts.parser()
                    .verifyWith(accessKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            // 이후 컨트롤러에서 request.getAttribute("userId") 로 참조
            httpReq.setAttribute("userId", userId);

            log.debug("[JwtAuthFilter] Auth success: userId={}, path={}", userId, path);
            chain.doFilter(request, response);

        } catch (JwtException e) {
            // 서명 불일치, 토큰 만료, 형식 오류 등 모든 JWT 관련 예외
            log.warn("[JwtAuthFilter] Token validation failed: path={}, error={}", path, e.getMessage());
            sendUnauthorized(httpRes, "유효하지 않거나 만료된 토큰입니다.");
        }
    }

    /**
     * 요청 경로가 JWT 검증 면제 공개 경로에 해당하는지 확인한다.
     *
     * @param path 요청 URI 경로
     * @return 공개 경로이면 {@code true}
     */
    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * 401 Unauthorized 응답을 JSON 형식으로 반환한다.
     *
     * @param response HTTP 응답
     * @param message  오류 메시지
     * @throws IOException 응답 쓰기 실패 시
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 메시지에 큰따옴표가 포함될 경우 JSON이 깨지지 않도록 이스케이프 처리
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message.replace("\"", "\\\"")));
    }
}

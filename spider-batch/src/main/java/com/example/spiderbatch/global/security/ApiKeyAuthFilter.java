package com.example.spiderbatch.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * HTTP 요청의 {@code X-API-Key} 헤더를 검증하는 API Key 인증 필터.
 *
 * <p>환경변수 {@code BATCH_WAS_API_KEY}가 설정된 경우에만 인증을 활성화한다.
 * 값이 비어있으면 필터를 무조건 통과시켜 개발 환경에서의 번거로움을 줄인다.
 * {@code /actuator/**}, {@code /mock/**} 경로는 인증 대상에서 제외한다.</p>
 *
 * <p>인증 실패 시 HTTP 401과 JSON 오류 본문을 반환한다.</p>
 *
 * // TODO: 운영 환경에서는 Spring Security + JWT 또는 mTLS로 교체 권장.
 * //       API Key는 평문 전송이므로 반드시 HTTPS와 함께 사용해야 한다.
 */
@Slf4j
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    /** 요청 헤더에서 API Key를 읽어오는 헤더 이름 */
    private static final String API_KEY_HEADER = "X-API-Key";

    /** 인증을 건너뛸 경로 패턴 목록 */
    private static final String[] EXCLUDE_PATTERNS = {"/actuator/**", "/mock/**"};

    private final String expectedApiKey;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher;

    public ApiKeyAuthFilter(
            @Value("${BATCH_WAS_API_KEY:}") String expectedApiKey,
            ObjectMapper objectMapper) {
        this.expectedApiKey = expectedApiKey;
        this.objectMapper = objectMapper;
        this.pathMatcher = new AntPathMatcher();
    }

    /**
     * {@code BATCH_WAS_API_KEY}가 비어있으면 필터 자체를 비활성화한다.
     * 설정되지 않은 개발 환경에서 불필요한 인증 실패를 방지하기 위한 단락 조건이다.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // API Key가 설정되지 않은 경우 인증 자체를 비활성화 (개발 환경 대응)
        if (!StringUtils.hasText(expectedApiKey)) {
            return true;
        }

        String requestPath = request.getRequestURI();
        for (String pattern : EXCLUDE_PATTERNS) {
            if (pathMatcher.match(pattern, requestPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestApiKey = request.getHeader(API_KEY_HEADER);

        // 타이밍 공격(Timing Attack) 방지: equals() 대신 상수 시간 비교 사용
        if (requestApiKey == null || !MessageDigest.isEqual(
                expectedApiKey.getBytes(StandardCharsets.UTF_8),
                requestApiKey.getBytes(StandardCharsets.UTF_8))) {
            log.warn("[ApiKeyAuthFilter] 유효하지 않은 API Key 요청: uri={}, remoteAddr={}",
                    request.getRequestURI(), request.getRemoteAddr());
            sendUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 401 상태와 JSON 오류 본문을 응답에 기록한다.
     */
    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, String> body = Map.of(
                "error", "Unauthorized",
                "message", "유효하지 않은 API Key입니다"
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

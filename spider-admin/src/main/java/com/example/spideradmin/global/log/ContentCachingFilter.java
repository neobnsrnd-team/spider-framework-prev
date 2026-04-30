package com.example.spideradmin.global.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Request Body를 캐싱하여 여러 번 읽을 수 있도록 하는 Filter
 * Interceptor에서 request body를 읽기 위해 필요
 * SecurityConfig에서 Spring Security Filter 앞에 등록됨
 */
@Slf4j
public class ContentCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // API 요청만 캐싱 (정적 리소스 제외)
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            log.debug("ContentCachingFilter applied for: {} {}", request.getMethod(), uri);
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}

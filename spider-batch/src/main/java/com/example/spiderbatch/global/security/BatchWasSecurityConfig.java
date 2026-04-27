package com.example.spiderbatch.global.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Batch WAS HTTP 엔드포인트 보안 설정 — {@link ApiKeyAuthFilter}를 서블릿 필터로 등록한다.
 *
 * <p>{@code batch.security.enabled=false}로 설정하면 API Key 인증 필터가 비활성화된다.
 * 개발·테스트 환경에서 인증 없이 엔드포인트에 접근할 때 사용한다.</p>
 */
@Configuration
@ConditionalOnProperty(name = "batch.security.enabled", matchIfMissing = true)
public class BatchWasSecurityConfig {

    /**
     * {@link ApiKeyAuthFilter}를 가장 높은 우선순위로 서블릿 필터 체인에 등록한다.
     *
     * <p>URL 패턴은 {@code /*}(전체)로 설정하고, 인증 제외 경로는
     * {@link ApiKeyAuthFilter#shouldNotFilter}에서 처리한다.</p>
     */
    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilterRegistration(
            ApiKeyAuthFilter apiKeyAuthFilter) {

        FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(apiKeyAuthFilter);
        // 전체 경로에 등록하고, 제외 경로는 필터 내부에서 처리한다
        registration.addUrlPatterns("/*");
        // 다른 필터보다 먼저 실행되어 인증되지 않은 요청을 조기에 차단한다
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}

package com.example.spiderbatch.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("ApiKeyAuthFilter 테스트")
class ApiKeyAuthFilterTest {

    private static final String VALID_KEY = "test-api-key";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("올바른 API Key → 필터 통과")
    void correctApiKey_passesThrough() throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/batch/execute");
        request.addHeader("X-API-Key", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("API Key 헤더 없음 → 401 반환, 체인 미호출")
    void missingApiKey_returns401() throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/batch/execute");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("Unauthorized");
        verifyNoInteractions(chain);
    }

    @Test
    @DisplayName("잘못된 API Key → 401 반환, 체인 미호출")
    void wrongApiKey_returns401() throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/batch/execute");
        request.addHeader("X-API-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verifyNoInteractions(chain);
    }

    @Test
    @DisplayName("BATCH_WAS_API_KEY 미설정(빈 값) → 필터 비활성화, API Key 없어도 통과")
    void emptyApiKeyConfig_disablesFilter() throws Exception {
        // 환경변수 미설정 시 빈 문자열로 주입됨 — 개발 환경 대응
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("", objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/batch/execute");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // shouldNotFilter() returns true → 체인 직접 호출
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("/actuator 경로 → 인증 제외, API Key 없어도 통과")
    void actuatorPath_excludedFromAuth() throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("/mock 경로 → 인증 제외, API Key 없어도 통과")
    void mockPath_excludedFromAuth() throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/batch/status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("401 응답 본문은 JSON 형식")
    void unauthorizedResponse_isJson() throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(VALID_KEY, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/batch/execute");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        String body = response.getContentAsString();
        // JSON 파싱 가능 여부 확인
        objectMapper.readTree(body);
        assertThat(body).contains("\"error\"").contains("\"message\"");
    }
}

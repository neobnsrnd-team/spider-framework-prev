package com.example.spideradmin.global.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

@DisplayName("CustomAuthenticationEntryPoint 테스트")
class CustomAuthenticationEntryPointTest {

    private final CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();

    @Test
    @DisplayName("API 요청 시 401 JSON 응답을 반환한다")
    void commence_apiRequest_returns401Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Unauthorized"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("인증이 필요합니다");
        assertThat(response.getContentAsString()).contains("401");
    }

    @Test
    @DisplayName("페이지 요청 시 /login으로 리다이렉트한다")
    void commence_pageRequest_redirectsToLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Unauthorized"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?expired=true");
    }
}

package com.example.spideradmin.global.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {

        String userId = request.getParameter("userId");
        String errorMessage;

        if (exception instanceof LockedException) {
            log.warn("Login blocked - locked account: {}", userId);
            errorMessage = "계정이 잠겨있습니다. 관리자에게 문의하세요.";
        } else {
            log.warn("Login failed - userId: {}, Reason: {}", userId, exception.getMessage());

            if (userId != null && !userId.isBlank()) {
                loginAttemptService.handleLoginFailure(userId);
            }

            // 사용자 열거 방지: 모든 인증 실패에 동일 메시지 반환 (S5804)
            errorMessage = "아이디 또는 비밀번호가 올바르지 않습니다.";
        }

        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        setDefaultFailureUrl("/login?error=true&message=" + encodedMessage);

        super.onAuthenticationFailure(request, response, exception);
    }
}

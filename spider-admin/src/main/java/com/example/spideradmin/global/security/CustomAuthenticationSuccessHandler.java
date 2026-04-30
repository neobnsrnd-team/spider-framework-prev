package com.example.spideradmin.global.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws ServletException, IOException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 로그인 실패 횟수 초기화
        loginAttemptService.handleLoginSuccess(userDetails.getUserId());

        // 세션에 사용자 정보 저장
        HttpSession session = request.getSession();
        session.setAttribute("userId", userDetails.getUserId());
        session.setAttribute("userName", userDetails.getDisplayName());

        // 로그인 성공 후 기본 페이지로 리다이렉트
        setDefaultTargetUrl("/home");

        super.onAuthenticationSuccess(request, response, authentication);
    }
}

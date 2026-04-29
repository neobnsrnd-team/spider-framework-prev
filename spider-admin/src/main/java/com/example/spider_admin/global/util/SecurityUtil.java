package com.example.spider_admin.global.util;

import com.example.spider_admin.global.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security Utility Class
 * Provides helper methods for accessing current user information
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    /**
     * 인증된 사용자가 없을 때 사용되는 기본 시스템 사용자 ID
     * <p>배치 작업, 스케줄러 등 비인증 컨텍스트에서 사용</p>
     */
    public static final String SYSTEM_USER_ID = "SYSTEM";

    /**
     * Get current authenticated user details
     * @return CustomUserDetails or null if not authenticated
     */
    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }

        return null;
    }

    /**
     * Get current user ID
     * @return User ID or null if not authenticated
     */
    public static String getCurrentUserId() {
        CustomUserDetails userDetails = getCurrentUser();
        return userDetails != null ? userDetails.getUserId() : null;
    }

    /**
     * Get current username (= userId, Spring Security principal identifier)
     * @return Username (userId) or null if not authenticated
     */
    public static String getCurrentUsername() {
        CustomUserDetails userDetails = getCurrentUser();
        return userDetails != null ? userDetails.getUsername() : null;
    }

    /**
     * 현재 사용자 표시명 반환 (USER_NAME, application-specific display name)
     * @return 표시명 또는 null (비인증 시)
     */
    public static String getCurrentUserDisplayName() {
        CustomUserDetails userDetails = getCurrentUser();
        return userDetails != null ? userDetails.getDisplayName() : null;
    }

    /**
     * 현재 사용자 ID 또는 ANONYMOUS 반환
     * <p>로깅 컨텍스트에서 사용. 인증 안 된 요청도 추적 가능하도록 "ANONYMOUS" 반환.</p>
     */
    public static String getCurrentUserIdOrAnonymous() {
        String userId = getCurrentUserId();
        return userId != null ? userId : "ANONYMOUS";
    }

    /**
     * 현재 사용자 ID 또는 시스템 사용자 ID 반환
     * <p>인증된 사용자가 없으면 SYSTEM_USER_ID를 반환합니다.</p>
     *
     * @return 현재 사용자 ID 또는 SYSTEM_USER_ID
     */
    public static String getCurrentUserIdOrSystem() {
        String userId = getCurrentUserId();
        return userId != null ? userId : SYSTEM_USER_ID;
    }

    /**
     * 현재 사용자명 또는 시스템 사용자 ID 반환
     * <p>인증된 사용자가 없거나 사용자명이 null이면 SYSTEM_USER_ID를 반환합니다.</p>
     *
     * @return 현재 사용자명 또는 SYSTEM_USER_ID
     */
    public static String getCurrentUsernameOrSystem() {
        String username = getCurrentUsername();
        return username != null ? username : SYSTEM_USER_ID;
    }
}

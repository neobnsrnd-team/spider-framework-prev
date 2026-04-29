package com.example.spider_admin.global.security;

import com.example.spider_admin.domain.user.mapper.UserMapper;
import com.example.spider_admin.global.security.config.SecurityAccessProperties;
import com.example.spider_admin.global.util.AuditUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String SYSTEM_USER_ID = "SYSTEM";

    private final UserMapper userMapper;
    private final SecurityAccessProperties securityAccessProperties;

    @Transactional
    public void handleLoginFailure(String userId) {
        String now = AuditUtil.now();
        userMapper.incrementLoginFailCount(userId, now, SYSTEM_USER_ID);

        Integer failCount = userMapper.selectLoginFailCount(userId);
        if (failCount != null && failCount >= securityAccessProperties.getMaxLoginFailCount()) {
            userMapper.lockUser(userId, now, SYSTEM_USER_ID);
            log.warn("계정 잠금 처리 - userId: {}, 실패 횟수: {}", userId, failCount);
        }
    }

    @Transactional
    public void handleLoginSuccess(String userId) {
        String now = AuditUtil.now();
        userMapper.resetLoginFailCount(userId, now, SYSTEM_USER_ID);
    }
}

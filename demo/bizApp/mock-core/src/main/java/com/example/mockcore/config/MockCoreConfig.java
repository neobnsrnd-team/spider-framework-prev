package com.example.mockcore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 계정계 Mock 서버 설정 클래스.
 *
 * <p>TCP 서버({@link com.example.mockcore.infra.LegacyTcpServer})는 {@code @Component}로
 * 자동 등록되므로 이 Config에서는 비밀번호 인코더만 선언한다.</p>
 */
@Configuration
public class MockCoreConfig {

    /**
     * BCrypt 비밀번호 인코더 빈.
     *
     * <p>{@link com.example.mockcore.repository.AccountRepository}에서 사용자 비밀번호 검증에 사용한다.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

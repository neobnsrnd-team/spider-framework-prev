package com.example.spiderlink.config;

import com.example.spiderlink.infra.http.SocketPoolStatusController;
import com.example.spiderlink.infra.tcp.client.pool.SocketPoolRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * spider-link Servlet WAS 전용 자동 설정.
 *
 * <p>{@link SpiderLinkAutoConfiguration}에서 분리된 설정으로, {@code spring-webmvc}가 클래스패스에 없는
 * TCP 전용 WAS에서 {@code NoClassDefFoundError}가 발생하는 문제를 방지한다.</p>
 *
 * <p>{@link SpiderLinkAutoConfiguration} 이후에 실행되어 {@link SocketPoolRegistry} 빈이
 * 이미 등록된 상태에서 Servlet 컨트롤러를 안전하게 등록한다.</p>
 *
 * <p>등록되는 빈 목록:</p>
 * <ul>
 *   <li>{@link SocketPoolStatusController} — {@code GET /api/internal/pool/status} 핸들러</li>
 * </ul>
 */
@AutoConfiguration(after = SpiderLinkAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
public class SpiderLinkServletAutoConfiguration {

    /**
     * 소켓 풀 상태 조회 내부 API 컨트롤러 빈.
     *
     * <p>standalone 실행 시 컴포넌트 스캔으로 이미 등록될 수 있으므로
     * {@code @ConditionalOnMissingBean}으로 중복 등록을 방지한다.</p>
     *
     * @param socketPoolRegistry 소켓 커넥션 풀 레지스트리
     */
    @Bean
    @ConditionalOnBean(SocketPoolRegistry.class)
    @ConditionalOnMissingBean
    public SocketPoolStatusController socketPoolStatusController(SocketPoolRegistry socketPoolRegistry) {
        return new SocketPoolStatusController(socketPoolRegistry);
    }
}

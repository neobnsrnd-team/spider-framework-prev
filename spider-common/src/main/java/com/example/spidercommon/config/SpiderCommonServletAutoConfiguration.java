package com.example.spidercommon.config;

import com.example.spidercommon.domain.loglevel.LogLevelReloadController;
import com.example.spidercommon.domain.management.ManagementReloadHttpController;
import com.example.spidercommon.domain.management.executor.ManagementExecutor;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * spider-common Servlet WAS 전용 자동 설정.
 *
 * <p>{@link SpiderCommonAutoConfiguration}에서 분리된 설정으로, {@code spring-webmvc}가 클래스패스에 없는
 * TCP 전용 WAS에서 {@code NoClassDefFoundError}가 발생하는 문제를 방지한다.</p>
 *
 * <p>클래스 레벨 {@code @ConditionalOnClass(WebMvcConfigurer.class)}로 보호되어 있어
 * {@code spring-webmvc}가 없는 환경에서는 이 클래스 자체가 로드되지 않는다.</p>
 *
 * <p>등록되는 빈 목록:</p>
 * <ul>
 *   <li>{@link LogLevelReloadController} — HTTP {@code POST /api/internal/log/*} 핸들러</li>
 *   <li>{@link ManagementReloadHttpController} — HTTP {@code POST /api/management/reload} 핸들러</li>
 *   <li>{@link InternalApiInterceptor} — {@code /api/internal/**}, {@code /api/management/**} IP 접근 제어</li>
 *   <li>{@link WebMvcConfig} — {@link InternalApiInterceptor} MVC 인터셉터 등록</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
// spring-webmvc가 없는 TCP 전용 WAS에서 이 클래스 자체가 로드되지 않도록 클래스 레벨에서 차단
@ConditionalOnClass(WebMvcConfigurer.class)
public class SpiderCommonServletAutoConfiguration {

    /**
     * 로그 레벨 변경 내부 API 컨트롤러 빈.
     *
     * <p>standalone 실행 시에는 컴포넌트 스캔으로 이미 등록될 수 있으므로
     * {@code @ConditionalOnMissingBean}으로 중복 등록을 방지한다.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public LogLevelReloadController logLevelReloadController(
            com.example.spidercommon.domain.loglevel.LogLevelApplier logLevelApplier) {
        return new LogLevelReloadController(logLevelApplier);
    }

    /**
     * HTTP 관리 명령 수신 컨트롤러 빈.
     *
     * <p>Admin → HTTP WAS 경로의 Reload 요청을 처리한다.
     * TCP의 {@link ManagementReloadHttpController}와
     * 동일한 {@link ManagementExecutor} 패턴을 사용한다.</p>
     *
     * @param executors Spring 컨텍스트에 등록된 모든 ManagementExecutor 구현체 목록
     */
    @Bean
    @ConditionalOnBean(ManagementExecutor.class)
    @ConditionalOnMissingBean
    public ManagementReloadHttpController managementReloadHttpController(
            List<ManagementExecutor> executors) {
        return new ManagementReloadHttpController(executors);
    }

    /**
     * 내부 API IP 접근 제어 인터셉터 빈.
     *
     * <p>standalone 실행 시에는 컴포넌트 스캔으로 이미 등록되므로 {@code @ConditionalOnMissingBean}으로
     * 중복 등록을 방지한다. 라이브러리 JAR로 포함되는 경우에만 이 빈이 실제로 생성된다.</p>
     *
     * @param allowedIps {@code internal-api.allowed-ips} 프로퍼티 (기본값: loopback 3종)
     */
    @Bean
    @ConditionalOnMissingBean
    public InternalApiInterceptor internalApiInterceptor(
            @Value("${internal-api.allowed-ips:127.0.0.1,0:0:0:0:0:0:0:1,::1}") List<String> allowedIps) {
        return new InternalApiInterceptor(allowedIps);
    }

    /**
     * MVC 인터셉터 등록 설정 빈.
     *
     * <p>{@link InternalApiInterceptor}를 {@code /api/internal/**}, {@code /api/management/**} 경로에
     * 등록한다. standalone 실행 시에는 컴포넌트 스캔으로 이미 등록되므로 중복 등록을 방지한다.</p>
     */
    @Bean
    @ConditionalOnMissingBean(WebMvcConfig.class)
    public WebMvcConfig spiderCommonWebMvcConfig(InternalApiInterceptor internalApiInterceptor) {
        return new WebMvcConfig(internalApiInterceptor);
    }
}

package com.example.spiderlink.config;

import com.example.spiderlink.config.InternalApiInterceptor;
import com.example.spiderlink.config.WebMvcConfig;
import com.example.spiderlink.domain.loglevel.LogLevelApplier;
import com.example.spiderlink.domain.management.ManagementReloadCommandHandler;
import com.example.spiderlink.domain.management.ManagementReloadHttpController;
import com.example.spiderlink.domain.management.executor.LogAdditivityExecutor;
import com.example.spiderlink.domain.management.executor.LogLevelExecutor;
import com.example.spiderlink.domain.management.executor.ManagementExecutor;
import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * spider-link 공통 자동 설정.
 *
 * <p>이 클래스는 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}에
 * 등록되어 Spring Boot 자동 설정 메커니즘으로 로드되며, spider-link를 의존하는 모든 WAS에 자동 적용된다.</p>
 *
 * <p>등록되는 빈 목록:</p>
 * <ul>
 *   <li>{@link MessageInstanceRecorder} — JdbcTemplate이 존재하는 경우에만 등록</li>
 *   <li>{@link LogLevelApplier} — Logback 런타임 조작 공통 로직</li>
 *   <li>{@link LogLevelExecutor} — {@code gubun=log_config_level} 관리 명령 처리기</li>
 *   <li>{@link LogAdditivityExecutor} — {@code gubun=log_config_additivity} 관리 명령 처리기</li>
 *   <li>{@link ManagementReloadCommandHandler} — TCP {@code MANAGEMENT_RELOAD} 명령 핸들러</li>
 *   <li>{@link ManagementReloadHttpController} — HTTP {@code POST /api/management/reload} 핸들러</li>
 *   <li>{@link InternalApiInterceptor} — {@code /api/internal/**}, {@code /api/management/**} IP 접근 제어 (Servlet WAS 전용)</li>
 *   <li>{@link WebMvcConfig} — {@link InternalApiInterceptor} MVC 인터셉터 등록 (Servlet WAS 전용)</li>
 * </ul>
 */
// JdbcTemplateAutoConfiguration 이후에 실행해야 JdbcTemplate 빈이 이미 등록된 상태에서
// @ConditionalOnBean(JdbcTemplate.class) 조건을 올바르게 평가할 수 있다
@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
public class SpiderLinkAutoConfiguration {

    /**
     * 전문 거래 이력 기록기 빈.
     *
     * <p>JdbcTemplate 빈이 존재하는 경우에만 생성된다.
     * {@code spring.application.name}을 ORG_ID·INSTANCE_ID에 활용한다.</p>
     *
     * @param jdbcTemplate DB 연결 (소비 모듈의 datasource 사용)
     * @param objectMapper JSON 직렬화용 ObjectMapper
     * @param appName      spring.application.name
     * @return MessageInstanceRecorder 빈
     */
    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean
    public MessageInstanceRecorder messageInstanceRecorder(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.application.name:unknown}") String appName) {
        return new MessageInstanceRecorder(jdbcTemplate, objectMapper, appName);
    }

    /**
     * Logback 런타임 조작 공통 로직 빈.
     *
     * <p>HTTP 컨트롤러와 TCP 실행기 양쪽에서 공유한다.
     * WAS가 별도 구현체를 등록한 경우 이 빈은 생성되지 않는다.</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public LogLevelApplier logLevelApplier() {
        return new LogLevelApplier();
    }

    /**
     * 로그 레벨 변경 관리 실행기 빈 ({@code gubun=log_config_level}).
     */
    @Bean
    @ConditionalOnMissingBean
    public LogLevelExecutor logLevelExecutor(LogLevelApplier logLevelApplier) {
        return new LogLevelExecutor(logLevelApplier);
    }

    /**
     * 로거 Additivity 변경 관리 실행기 빈 ({@code gubun=log_config_additivity}).
     */
    @Bean
    @ConditionalOnMissingBean
    public LogAdditivityExecutor logAdditivityExecutor(LogLevelApplier logLevelApplier) {
        return new LogAdditivityExecutor(logLevelApplier);
    }

    /**
     * TCP 관리 명령 핸들러 빈.
     *
     * <p>ManagementExecutor 빈이 하나 이상 존재할 때만 등록된다.
     * TCP 서버가 없는 HTTP 전용 WAS에서도 빈이 생성되지만, CommandDispatcher에 등록되지 않으면
     * 실제로 호출되지 않으므로 불필요한 부작용은 없다.</p>
     *
     * @param executors Spring 컨텍스트에 등록된 모든 ManagementExecutor 구현체 목록
     */
    @Bean
    @ConditionalOnBean(ManagementExecutor.class)
    @ConditionalOnMissingBean
    public ManagementReloadCommandHandler managementReloadCommandHandler(
            List<ManagementExecutor> executors) {
        return new ManagementReloadCommandHandler(executors);
    }

    /**
     * HTTP 관리 명령 수신 컨트롤러 빈.
     *
     * <p>Admin → HTTP WAS 경로의 Reload 요청을 처리한다.
     * TCP의 {@link ManagementReloadCommandHandler}와 동일한 {@link ManagementExecutor} 패턴을 사용한다.
     * web-application-type=none 인 TCP 전용 WAS에서는 빈이 생성되더라도 Spring MVC가 없으므로
     * HTTP 엔드포인트로 노출되지 않아 부작용이 없다.</p>
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
     * 내부 API IP 접근 제어 인터셉터 빈 (Servlet WAS 전용).
     *
     * <p>standalone 실행 시에는 컴포넌트 스캔으로 이미 등록되므로 {@code @ConditionalOnMissingBean}으로
     * 중복 등록을 방지한다. 라이브러리 JAR로 포함되는 경우에만 이 빈이 실제로 생성된다.</p>
     *
     * @param allowedIps {@code internal-api.allowed-ips} 프로퍼티 (기본값: loopback 3종)
     */
    @Bean
    @ConditionalOnWebApplication(type = Type.SERVLET)
    @ConditionalOnMissingBean
    public InternalApiInterceptor internalApiInterceptor(
            @Value("${internal-api.allowed-ips:127.0.0.1,0:0:0:0:0:0:0:1,::1}") List<String> allowedIps) {
        return new InternalApiInterceptor(allowedIps);
    }

    /**
     * MVC 인터셉터 등록 설정 빈 (Servlet WAS 전용).
     *
     * <p>{@link InternalApiInterceptor}를 {@code /api/internal/**}, {@code /api/management/**} 경로에
     * 등록한다. standalone 실행 시에는 컴포넌트 스캔으로 이미 등록되므로 중복 등록을 방지한다.</p>
     */
    @Bean
    @ConditionalOnWebApplication(type = Type.SERVLET)
    @ConditionalOnMissingBean(WebMvcConfig.class)
    public WebMvcConfig spiderLinkWebMvcConfig(InternalApiInterceptor internalApiInterceptor) {
        return new WebMvcConfig(internalApiInterceptor);
    }
}

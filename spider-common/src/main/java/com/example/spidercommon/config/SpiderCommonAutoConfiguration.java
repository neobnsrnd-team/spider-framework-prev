package com.example.spidercommon.config;

import com.example.spidercommon.domain.loglevel.LogLevelApplier;
import com.example.spidercommon.domain.management.ManagementReloadCommandHandler;
import com.example.spidercommon.domain.management.executor.LogAdditivityExecutor;
import com.example.spidercommon.domain.management.executor.LogLevelExecutor;
import com.example.spidercommon.domain.management.executor.ManagementExecutor;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * spider-common 공통 자동 설정.
 *
 * <p>이 클래스는 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}에
 * 등록되어 Spring Boot 자동 설정 메커니즘으로 로드되며, spider-common을 의존하는 모든 WAS에 자동 적용된다.</p>
 *
 * <p>등록되는 빈 목록:</p>
 * <ul>
 *   <li>{@link LogLevelApplier} — Logback 런타임 조작 공통 로직</li>
 *   <li>{@link LogLevelExecutor} — {@code gubun=log_config_level} 관리 명령 처리기</li>
 *   <li>{@link LogAdditivityExecutor} — {@code gubun=log_config_additivity} 관리 명령 처리기</li>
 *   <li>{@link ManagementReloadCommandHandler} — TCP {@code MANAGEMENT_RELOAD} 명령 핸들러</li>
 * </ul>
 *
 * <p>Servlet WAS 전용 빈({@code ManagementReloadHttpController}, {@code InternalApiInterceptor},
 * {@code WebMvcConfig})은 {@link SpiderCommonServletAutoConfiguration}에서 별도 관리된다.
 * spring-webmvc가 없는 TCP 전용 WAS에서 {@code NoClassDefFoundError}가 발생하지 않도록 분리되었다.</p>
 */
@AutoConfiguration
public class SpiderCommonAutoConfiguration {

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
}

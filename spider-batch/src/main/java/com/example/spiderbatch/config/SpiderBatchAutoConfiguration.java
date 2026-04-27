package com.example.spiderbatch.config;

import com.example.spiderbatch.domain.batch.controller.BatchExecuteController;
import com.example.spiderbatch.domain.batch.controller.BatchMonitorController;
import com.example.spiderbatch.domain.batch.mapper.BatchHisMapper;
import com.example.spiderbatch.domain.batch.service.BatchExecuteService;
import com.example.spiderbatch.domain.batch.service.BatchMonitorService;
import com.example.spiderbatch.domain.batch.service.DefaultBatchHistoryRecorder;
import com.example.spiderbatch.global.log.BatchAuditLogger;
import com.example.spiderbatch.global.security.ApiKeyAuthFilter;
import com.example.spiderbatch.global.security.BatchWasSecurityConfig;
import com.example.spiderbatch.spi.BatchHistoryRecorder;
import com.example.spiderbatch.tcp.BatchExecCommandHandler;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @file SpiderBatchAutoConfiguration.java
 * @description spider-batch 라이브러리 자동 설정.
 *
 * <p>소비자 프로젝트가 spider-batch를 의존성으로 추가하면
 * Spring Boot AutoConfiguration 메커니즘에 의해 이 설정이 자동 적용된다.
 * {@code JobLauncher}가 클래스패스에 있을 때만(= spring-boot-starter-batch 포함 시) 활성화된다.</p>
 *
 * <p>{@code @ComponentScan} 대신 {@code @Import}로 Bean을 명시 등록한다.
 * ComponentScan은 소비자 프로젝트의 같은 패키지 내 Bean을 의도치 않게 등록하거나
 * Bean 충돌을 유발할 수 있기 때문이다.</p>
 *
 * <p>등록 위치: {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}</p>
 */
@AutoConfiguration
@ConditionalOnClass(JobLauncher.class)
@EnableConfigurationProperties(BatchConfigurationProperties.class)
@Import({
        // Core Batch config
        BatchConfig.class,
        // TCP 서버 (@ConditionalOnProperty: batch.tcp.enabled)
        TcpServerConfig.class,
        // HTTP 보안 필터 및 등록 (@ConditionalOnProperty: batch.security.enabled)
        BatchWasSecurityConfig.class,
        ApiKeyAuthFilter.class,
        // 감사 로거, TCP 커맨드 핸들러
        BatchAuditLogger.class,
        BatchExecCommandHandler.class,
        // 배치 실행 서비스 및 REST 컨트롤러
        BatchExecuteService.class,
        BatchMonitorService.class,
        BatchExecuteController.class,
        BatchMonitorController.class,
})
public class SpiderBatchAutoConfiguration {

    /**
     * {@link BatchHistoryRecorder} 기본 구현체를 등록한다.
     * 소비자가 별도 {@link BatchHistoryRecorder} Bean을 등록하면 이 Bean은 생성되지 않는다.
     */
    @Bean
    @ConditionalOnMissingBean(BatchHistoryRecorder.class)
    public BatchHistoryRecorder batchHistoryRecorder(BatchHisMapper batchHisMapper) {
        return new DefaultBatchHistoryRecorder(batchHisMapper);
    }
}

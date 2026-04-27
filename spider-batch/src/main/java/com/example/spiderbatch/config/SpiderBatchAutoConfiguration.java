package com.example.spiderbatch.config;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * @file SpiderBatchAutoConfiguration.java
 * @description spider-batch-core 라이브러리 자동 설정.
 *
 * <p>소비자 프로젝트가 spider-batch-core를 의존성으로 추가하면
 * Spring Boot AutoConfiguration 메커니즘에 의해 이 설정이 자동 적용된다.
 * {@code JobLauncher}가 클래스패스에 있을 때만(= spring-boot-starter-batch 포함 시) 활성화된다.</p>
 *
 * <p>소비자의 컴포넌트 스캔 패키지와 무관하게 {@code com.example.spiderbatch} 패키지 전체를
 * 스캔하여 core의 모든 서비스·컨트롤러·핸들러 빈을 등록한다.</p>
 *
 * <p>등록 위치: {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}</p>
 */
@AutoConfiguration
@ConditionalOnClass(JobLauncher.class)
@EnableConfigurationProperties(BatchConfigurationProperties.class)
@ComponentScan(basePackages = "com.example.spiderbatch")
public class SpiderBatchAutoConfiguration {
}

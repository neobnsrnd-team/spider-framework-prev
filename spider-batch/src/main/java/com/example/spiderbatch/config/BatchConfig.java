package com.example.spiderbatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Batch 공통 설정.
 *
 * <p>Spring Batch 5.x는 JobRegistrySmartInitializingSingleton이 자동으로
 * 모든 Job Bean을 JobRegistry에 등록한다.
 * JobRegistryBeanPostProcessor를 추가하면 이중 등록으로 DuplicateJobException이 발생하므로 제거.</p>
 *
 * <p>{@code @EnableAsync}: 알림 서비스(Slack·Email)의 {@code @Async} 메서드를 활성화한다.
 * 배치 실행 스레드가 외부 HTTP·SMTP 호출에 의해 블로킹되지 않도록 비동기 처리한다.</p>
 */
@Configuration
@EnableAsync
public class BatchConfig {
}

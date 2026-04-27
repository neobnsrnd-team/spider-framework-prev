package com.example.spiderbatch.spi;

import java.util.List;
import org.springframework.batch.core.Job;

/**
 * @file JobProvider.java
 * @description spider-batch 소비자 프로젝트가 배치 Job을 등록하는 표준 확장점.
 *
 * <p>소비자는 이 인터페이스를 구현하고 Spring Bean으로 등록한다.
 * Spring Batch 5.x의 {@code JobRegistrySmartInitializingSingleton}이
 * 컨텍스트 내 모든 {@link Job} Bean을 {@code JobRegistry}에 자동 등록하므로,
 * 구현체의 Job Bean들은 선언만 해도 {@code BatchExecuteService}에서 이름으로 조회 가능하다.</p>
 *
 * @example
 * <pre>{@code
 * @Configuration
 * public class MyJobProvider implements JobProvider {
 *     @Bean
 *     public Job myJob(JobRepository jobRepository, ...) { ... }
 *
 *     @Override
 *     public List<String> getJobNames() {
 *         return List.of("myJob");
 *     }
 * }
 * }</pre>
 */
public interface JobProvider {

    /**
     * 이 프로바이더가 제공하는 Job Bean 이름 목록.
     * FWK_BATCH_APP.BATCH_APP_FILE_NAME 값과 일치해야 한다.
     *
     * @return Job 이름 목록
     */
    List<String> getJobNames();
}

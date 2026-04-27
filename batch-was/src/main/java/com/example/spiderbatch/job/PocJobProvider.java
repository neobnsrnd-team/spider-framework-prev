package com.example.spiderbatch.job;

import com.example.spiderbatch.spi.JobProvider;
import java.util.List;
import org.springframework.context.annotation.Configuration;

/**
 * @file PocJobProvider.java
 * @description POC 전용 Job 목록을 {@link JobProvider}로 등록하는 구현체.
 *
 * <p>FWK_BATCH_APP.BATCH_APP_FILE_NAME에 등록된 Job 이름 목록을 반환한다.
 * 실제 Job Bean은 각 {@code *JobConfig} 클래스에서 {@code @Bean}으로 선언된다.
 * Spring Batch 5.x의 {@code JobRegistrySmartInitializingSingleton}이
 * 컨텍스트 내 모든 Job Bean을 {@code JobRegistry}에 자동 등록한다.</p>
 */
@Configuration
public class PocJobProvider implements JobProvider {

    @Override
    public List<String> getJobNames() {
        return List.of("db2db", "file2db", "fixedLengthFile2db", "db2foreign");
    }
}

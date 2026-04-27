package com.example.spiderbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @file BatchWasApplication.java
 * @description Batch AP 서버 메인 애플리케이션.
 *
 * <p>spider-batch 라이브러리를 내장하여 Admin 콘솔의 배치 실행 요청을 처리한다.
 * POST /api/batch/execute 요청을 수신하거나 TCP(BATCH_EXEC 커맨드)로 실행 요청을 받아
 * Spring Batch Job을 실행하고 FWK_BATCH_HIS에 이력을 기록한다.</p>
 *
 * <p>Mapper 인터페이스는 {@code @Mapper} 애노테이션으로 자동 탐지된다.</p>
 */
@SpringBootApplication
public class BatchWasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchWasApplication.class, args);
    }
}

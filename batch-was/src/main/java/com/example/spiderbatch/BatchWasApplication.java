package com.example.spiderbatch;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
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
 * <p>@MapperScan: spider-batch 라이브러리 JAR 내 @Mapper 인터페이스가 DevTools
 * RestartClassLoader 환경에서 자동 스캔 누락되는 것을 방지하기 위해 명시 지정한다.</p>
 */
@SpringBootApplication
@MapperScan(basePackages = "com.example.spiderbatch", annotationClass = Mapper.class)
public class BatchWasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchWasApplication.class, args);
    }
}

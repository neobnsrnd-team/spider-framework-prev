package com.example.spiderlink;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * spider-link 연계엔진 실행 진입점.
 *
 * <p>Admin ↔ AP 서버 간 TCP 중계 및 전문 처리를 담당하는 standalone 프로세스.
 * 각 AP 서버(biz-channel, biz-auth, biz-transfer)는 spider-link를 라이브러리로 내장하여 직접 운영하므로,
 * 이 standalone 프로세스는 레거시 호환 용도로만 유지된다.</p>
 */
@SpringBootApplication
// Spring DevTools RestartClassLoader 환경에서 @Mapper 인터페이스가 스캔에서 누락되는 현상 방지
@MapperScan(annotationClass = Mapper.class)
public class SpiderLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpiderLinkApplication.class, args);
    }
}

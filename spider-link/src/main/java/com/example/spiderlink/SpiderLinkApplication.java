package com.example.spiderlink;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * spider-link 연계엔진 실행 진입점.
 *
 * <p>demo/backend 전문 처리 TCP 서버(port 9995)를 내장하여 기동한다.
 * demo/backend가 Spring Boot으로 전환되면 이 standalone 프로세스는 제거 예정.</p>
 */
@SpringBootApplication
// Spring DevTools RestartClassLoader 환경에서 @Mapper 인터페이스가 스캔에서 누락되는 현상 방지
@MapperScan(annotationClass = Mapper.class)
public class SpiderLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpiderLinkApplication.class, args);
    }
}

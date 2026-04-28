package com.example.mockcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 계정계 Mock 애플리케이션 진입점.
 *
 * <p>LegacyTcpServer가 포트 19300에서 고정길이 바이너리 프로토콜로 TCP 요청을 수신하며,
 * Oracle DB(D_SPIDERLINK 스키마)에서 사용자·카드·거래 데이터를 조회/처리한다.</p>
 */
@SpringBootApplication
public class MockCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockCoreApplication.class, args);
    }
}

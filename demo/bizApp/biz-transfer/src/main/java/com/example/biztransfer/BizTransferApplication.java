package com.example.biztransfer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 이체AP(biz-transfer) 메인 진입점.
 *
 * <p>SpiderTcpServer를 포트 19200에서 기동하고, TRANSFER_* 커맨드를 수신한다.
 * {@link com.example.spiderlink.infra.tcp.handler.MetaDrivenServiceOrchestrator} 가
 * FWK 메타테이블 기반으로 커맨드를 처리하며, PIN 검증이 필요한 TRANSFER_IMMEDIATE_PAY 만
 * {@link com.example.biztransfer.handler.TransferImmediatePayHandler} 가 별도 처리한다.</p>
 *
 * <p>scanBasePackages 는 infra/domain 하위만 포함하여 SpiderLinkApplication·WebMvcConfig 등
 * 불필요한 spider-link 설정 클래스가 스캔되는 것을 방지한다.
 * @MapperScan 으로 spider-link domain 패키지의 @Mapper 인터페이스를 명시적으로 탐색한다.</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.biztransfer",
        "com.example.spiderlink.infra",
        "com.example.spiderlink.domain"
})
@MapperScan("com.example.spiderlink.domain")
public class BizTransferApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizTransferApplication.class, args);
    }
}

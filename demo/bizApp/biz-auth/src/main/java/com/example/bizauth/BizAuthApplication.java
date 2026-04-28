package com.example.bizauth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 인증AP (biz-auth) 애플리케이션 진입점.
 *
 * <p>SpiderTcpServer 를 통해 TCP port 19100 에서 채널AP(biz-channel) 요청을 수신한다.
 * {@link com.example.spiderlink.infra.tcp.handler.MetaDrivenCommandHandler} 가
 * FWK 메타테이블을 참조하여 커맨드별 서비스를 동적으로 실행하고,
 * {@link com.example.spiderlink.infra.tcp.biz.TcpCallBiz} 를 통해 mock-core(TCP 19300)로 중계한다.</p>
 *
 * <p>HTTP 서버를 사용하지 않는 비Web 스탠드얼론 애플리케이션이다.
 * {@code spring.main.web-application-type=none} 설정으로 내장 서블릿 컨테이너를 비활성화한다.</p>
 *
 * <p>scanBasePackages 는 infra/domain 하위만 포함하여 SpiderLinkApplication·WebMvcConfig 등
 * 불필요한 spider-link 설정 클래스가 스캔되는 것을 방지한다.
 * @MapperScan 으로 spider-link domain 패키지의 @Mapper 인터페이스를 명시적으로 탐색한다.</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.bizauth",
        "com.example.spiderlink.infra",
        "com.example.spiderlink.domain"
})
@MapperScan("com.example.spiderlink.domain")
public class BizAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizAuthApplication.class, args);
    }
}

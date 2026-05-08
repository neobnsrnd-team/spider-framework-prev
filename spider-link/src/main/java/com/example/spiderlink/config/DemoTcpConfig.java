package com.example.spiderlink.config;

import com.example.spiderlink.infra.tcp.codec.JsonMessageCodec;
import com.example.spidercommon.infra.tcp.handler.CommandDispatcher;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.handler.MetaDrivenServiceOrchestrator;
import com.example.spiderlink.infra.tcp.server.SpiderTcpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AP 서버 전문 처리 TCP 서버 설정 (레거시).
 *
 * <p>spider-link의 {@link SpiderTcpServer}에 {@link JsonMessageCodec}과
 * {@link MetaDrivenServiceOrchestrator}를 주입하여 JSON TCP 서버를 구성한다.</p>
 *
 * <p>biz-channel, biz-auth, biz-transfer는 이미 spider-link를 내장하여 직접 TCP 서버를 운영한다.
 * 이 설정은 {@code tcp.demo-server.enabled=true}일 때만 활성화되며 standalone 모드에서만 사용된다.</p>
 */
@Configuration
// biz-auth/biz-transfer가 spiderlink 패키지를 스캔할 때 불필요한 TCP 서버 기동을 방지
@ConditionalOnProperty(name = "tcp.demo-server.enabled", havingValue = "true", matchIfMissing = false)
public class DemoTcpConfig {

    /**
     * JSON TCP 프로토콜로 통신하는 전문 처리 서버 Bean (standalone 모드용).
     *
     * <p>Spring이 ApplicationRunner를 구현한 Bean을 자동으로 실행하므로
     * 애플리케이션 기동 시 서버가 자동 시작된다.</p>
     */
    @Bean
    public SpiderTcpServer<JsonCommandRequest, JsonCommandResponse> demoTcpServer(
            @Value("${tcp.demo-server.port:9995}") int port,
            @Value("${tcp.demo-server.handler-pool-size:20}") int handlerPoolSize,
            MetaDrivenServiceOrchestrator handler,
            ObjectMapper objectMapper) {

        CommandDispatcher<JsonCommandRequest, JsonCommandResponse> dispatcher =
                new CommandDispatcher<>(List.of(handler));

        return new SpiderTcpServer<>(port, handlerPoolSize, 100, new JsonMessageCodec(objectMapper), dispatcher);
    }
}

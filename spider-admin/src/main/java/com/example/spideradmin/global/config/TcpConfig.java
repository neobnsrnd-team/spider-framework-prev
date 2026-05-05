package com.example.spideradmin.global.config;

import com.example.spidercommon.infra.tcp.handler.CommandDispatcher;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.infra.tcp.client.TcpClient;
import com.example.spiderlink.infra.tcp.codec.JsonMessageCodec;
import com.example.spiderlink.infra.tcp.server.SpiderTcpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Admin TCP 인프라 설정.
 *
 * <p>spider-link {@link TcpClient}와 {@link SpiderTcpServer}를 스프링 빈으로 등록한다.
 * 두 클래스 모두 {@code com.example.spiderlink} 패키지에 선언되어 있어
 * 컴포넌트 스캔 범위 밖이므로 이 클래스에서 명시적으로 등록한다.</p>
 *
 * <pre>{@code
 *   // 수신 흐름: biz-channel / 외부 → [TCP 9999] → SpiderTcpServer → CommandDispatcher → Handler
 *   // 발신 흐름: Admin → TcpClient.sendJson()  → biz-channel (19400)
 *              Admin → TcpClient.sendObject() → batch-was   (9998)
 * }</pre>
 */
@Configuration
public class TcpConfig {

    /** Admin 인바운드 TCP 포트 (기본값: 9999) */
    @Value("${tcp.server.port:9999}")
    private int tcpServerPort;

    /** 요청 처리 스레드 풀 크기 (기본값: 20) */
    @Value("${tcp.server.handler-pool-size:20}")
    private int handlerPoolSize;

    /** 요청 대기 큐 최대 크기 (기본값: 50) */
    @Value("${tcp.server.queue-capacity:50}")
    private int queueCapacity;

    /**
     * spider-link TcpClient 빈 등록.
     *
     * <p>Admin은 JSON(biz-channel)과 ObjectStream(batch-was) 두 프로토콜을 사용하므로
     * {@link TcpClient#sendJson}과 {@link TcpClient#sendObject} 모두 활용한다.
     * FixedLength 프로토콜은 Admin 구간에서 미사용이므로 structurePool/fixedLengthParser 미주입.</p>
     */
    @Bean
    public TcpClient tcpClient(ObjectMapper objectMapper, Optional<MessageInstanceRecorder> recorder) {
        return new TcpClient(objectMapper, recorder.orElse(null));
    }

    /**
     * Admin 인바운드 TCP 서버 빈.
     *
     * <p>수신 프로토콜은 {@link JsonMessageCodec} (4바이트 길이 프리픽스 + UTF-8 JSON).
     * {@link CommandHandler} 구현체 목록을 스프링이 자동 수집하여 {@link CommandDispatcher}에 등록한다.</p>
     */
    @Bean
    public SpiderTcpServer<JsonCommandRequest, JsonCommandResponse> adminTcpServer(
            ObjectMapper objectMapper,
            List<CommandHandler<JsonCommandRequest, JsonCommandResponse>> handlers,
            Optional<MessageInstanceRecorder> recorder) {

        CommandDispatcher<JsonCommandRequest, JsonCommandResponse> dispatcher = new CommandDispatcher<>(handlers);

        return new SpiderTcpServer<>(
                tcpServerPort,
                handlerPoolSize,
                queueCapacity,
                new JsonMessageCodec(objectMapper),
                dispatcher,
                recorder.orElse(null));
    }
}

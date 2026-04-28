package com.example.mockcore.config;

import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.infra.tcp.codec.JsonMessageCodec;
import com.example.spidercommon.infra.tcp.handler.CommandDispatcher;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.server.SpiderTcpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

/**
 * 계정계 Mock TCP 서버 설정 클래스.
 *
 * <p>SpiderTcpServer를 포트 19300에서 시작하며, Spring 컨텍스트에 등록된
 * 모든 {@link CommandHandler} 빈을 {@link CommandDispatcher}에 자동 주입한다.</p>
 *
 * <pre>{@code
 * // SpiderTcpServer는 ApplicationRunner를 구현하여 자동 시작되며,
 * // @PreDestroy 로 애플리케이션 종료 시 자동 정지된다.
 * }</pre>
 */
@Configuration
public class MockCoreConfig {

    /** 인바운드 TCP 포트 (기본값: 19300) */
    @Value("${tcp.server.port:19300}")
    private int tcpServerPort;

    /** 요청 처리 스레드 풀 크기 (기본값: 10) */
    @Value("${tcp.server.handler-pool-size:10}")
    private int handlerPoolSize;

    /** 요청 대기 큐 최대 크기 (기본값: 50) */
    @Value("${tcp.server.queue-capacity:50}")
    private int queueCapacity;

    /**
     * BCrypt 비밀번호 인코더 빈.
     *
     * <p>{@link com.example.mockcore.repository.AccountRepository} 에서 사용자 비밀번호 검증에 사용한다.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 계정계 Mock용 TCP 서버 빈.
     *
     * <p>mock-core는 datasource가 구성되어 있으므로 JdbcTemplate 빈이 항상 존재하며,
     * {@link MessageInstanceRecorder} 를 통해 전문 이력이 자동 기록된다.</p>
     *
     * @param objectMapper JSON 직렬화에 사용할 ObjectMapper (Spring 자동 주입)
     * @param handlers     컨텍스트에 등록된 모든 CommandHandler 목록 (Spring 자동 수집)
     * @param recorder     전문 이력 기록기 (JdbcTemplate 빈이 없으면 empty)
     * @return {@code tcp.server.port} 에서 동작하는 SpiderTcpServer 인스턴스
     */
    @Bean
    public SpiderTcpServer<JsonCommandRequest, JsonCommandResponse> mockCoreTcpServer(
            ObjectMapper objectMapper,
            List<CommandHandler<JsonCommandRequest, JsonCommandResponse>> handlers,
            Optional<MessageInstanceRecorder> recorder) {

        CommandDispatcher<JsonCommandRequest, JsonCommandResponse> dispatcher =
                new CommandDispatcher<>(handlers);

        return new SpiderTcpServer<>(tcpServerPort, handlerPoolSize, queueCapacity,
                new JsonMessageCodec(objectMapper), dispatcher, recorder.orElse(null));
    }
}

package com.example.bizauth.config;

import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.infra.tcp.client.TcpClient;
import com.example.spiderlink.infra.tcp.codec.JsonMessageCodec;
import com.example.spiderlink.infra.tcp.handler.CommandDispatcher;
import com.example.spiderlink.infra.tcp.handler.CommandHandler;
import com.example.spiderlink.infra.tcp.model.JsonCommandRequest;
import com.example.spiderlink.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.client.pool.SocketPoolManager;
import com.example.spiderlink.infra.tcp.parser.FixedLengthParser;
import com.example.spiderlink.infra.tcp.parser.MessageStructurePool;
import com.example.spiderlink.infra.tcp.server.SpiderTcpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

/**
 * 인증AP TCP 서버 설정 클래스.
 *
 * <p>SpiderTcpServer 를 스프링 빈으로 등록한다.
 * {@link SpiderTcpServer} 는 {@code ApplicationRunner} 를 구현하므로
 * 컨텍스트 로드 완료 후 자동으로 서버를 시작하고,
 * {@code @PreDestroy} 훅을 통해 애플리케이션 종료 시 자동으로 정지한다.</p>
 *
 * <pre>{@code
 *   // 수신 흐름: biz-channel → [TCP 19100] → SpiderTcpServer → CommandDispatcher → AuthLoginHandler / AuthMeHandler
 *   // 발신 흐름: AuthService → TcpClient → [TCP 19300] → mock-core
 * }</pre>
 */
@Configuration
public class BizAuthConfig {

    /** 인바운드 TCP 포트 (기본값: 19100) */
    @Value("${tcp.server.port:19100}")
    private int tcpServerPort;

    /** 요청 처리 스레드 풀 크기 (기본값: 5) */
    @Value("${tcp.server.handler-pool-size:5}")
    private int handlerPoolSize;

    /** 요청 대기 큐 최대 크기 (기본값: 20) */
    @Value("${tcp.server.queue-capacity:20}")
    private int queueCapacity;

    /**
     * spider-link TcpClient 빈 등록.
     *
     * <p>{@link TcpClient} 는 {@code com.example.spiderlink} 패키지에 선언되어 있어
     * 컴포넌트 스캔 범위에 포함되지 않으므로 명시적으로 등록한다.
     * {@link MessageInstanceRecorder} 가 존재하면 주입하여 전문 이력을 기록한다.
     * {@link MessageStructurePool} / {@link FixedLengthParser} 를 주입하면 mock-core 고정길이 프로토콜을 사용한다.</p>
     *
     * @param objectMapper     Jackson ObjectMapper
     * @param recorder         전문 이력 기록기 (JdbcTemplate 빈이 없으면 empty)
     * @param structurePool    전문 구조 캐시 (빈 없으면 empty — JSON fallback)
     * @param fixedLengthParser 고정길이 파서 (빈 없으면 empty — JSON fallback)
     * @param poolManager      소켓 커넥션 풀 매니저 (빈 없으면 empty — 요청마다 신규 소켓)
     * @return TcpClient 인스턴스
     */
    @Bean
    public TcpClient tcpClient(ObjectMapper objectMapper,
                                Optional<MessageInstanceRecorder> recorder,
                                Optional<MessageStructurePool> structurePool,
                                Optional<FixedLengthParser> fixedLengthParser,
                                Optional<SocketPoolManager> poolManager) {
        return new TcpClient(objectMapper, recorder.orElse(null),
                structurePool.orElse(null), fixedLengthParser.orElse(null),
                poolManager.orElse(null));
    }

    /**
     * 인증AP 인바운드 TCP 서버 빈.
     *
     * <p>스프링이 수집한 {@link CommandHandler} 구현체 목록을
     * {@link CommandDispatcher} 에 등록하여 커맨드별 라우팅을 수행한다.
     * {@link MessageInstanceRecorder} 가 존재하면 수신 요청·응답을 DB에 기록한다.</p>
     *
     * @param objectMapper JSON 직렬화·역직렬화에 사용할 ObjectMapper
     * @param handlers     스프링 컨텍스트에 등록된 모든 CommandHandler 구현체 목록
     * @param recorder     전문 이력 기록기 (JdbcTemplate 빈이 없으면 empty)
     * @return {@code tcp.server.port} 에서 수신 대기하는 SpiderTcpServer 인스턴스
     */
    @Bean
    public SpiderTcpServer<JsonCommandRequest, JsonCommandResponse> bizAuthTcpServer(
            ObjectMapper objectMapper,
            List<CommandHandler<JsonCommandRequest, JsonCommandResponse>> handlers,
            Optional<MessageInstanceRecorder> recorder) {

        CommandDispatcher<JsonCommandRequest, JsonCommandResponse> dispatcher =
                new CommandDispatcher<>(handlers);

        return new SpiderTcpServer<>(tcpServerPort, handlerPoolSize, queueCapacity,
                new JsonMessageCodec(objectMapper), dispatcher, recorder.orElse(null));
    }
}

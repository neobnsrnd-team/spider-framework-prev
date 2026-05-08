package com.example.biztransfer.config;

import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.infra.tcp.client.TcpClient;
import com.example.spiderlink.infra.tcp.codec.JsonMessageCodec;
import com.example.spidercommon.infra.tcp.handler.CommandDispatcher;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.client.pool.SocketPoolManager;
import com.example.spiderlink.infra.tcp.parser.FixedLengthParser;
import com.example.spiderlink.infra.tcp.parser.MessageStructurePool;
import com.example.spiderlink.infra.tcp.server.SpiderTcpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

/**
 * biz-transfer TCP 서버 설정 클래스.
 *
 * <p>SpiderTcpServer를 포트 19200에서 기동하며,
 * Spring 컨텍스트에 등록된 모든 {@link CommandHandler} 구현체를
 * {@link CommandDispatcher}에 자동으로 위임한다.</p>
 *
 * <pre>{@code
 * // 핸들러를 추가하려면 @Component를 붙인 CommandHandler 구현체를 작성하면 된다.
 * // Spring이 List<CommandHandler<...>>로 자동 주입한다.
 * }</pre>
 */
@Configuration
public class BizTransferConfig {

    /** 인바운드 TCP 포트 (기본값: 19200) */
    @Value("${tcp.server.port:19200}")
    private int tcpServerPort;

    /** 요청 처리 스레드 풀 크기 (기본값: 10) */
    @Value("${tcp.server.handler-pool-size:10}")
    private int handlerPoolSize;

    /** 요청 대기 큐 최대 크기 (기본값: 50) */
    @Value("${tcp.server.queue-capacity:50}")
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
     * 이체AP용 SpiderTcpServer 빈 등록.
     *
     * <p>포트 19200, 코어 스레드 10개, 최대 스레드 50개로 설정한다.
     * {@link MessageInstanceRecorder} 가 존재하면 수신 요청·응답을 DB에 기록한다.</p>
     *
     * @param objectMapper Spring이 제공하는 Jackson ObjectMapper
     * @param handlers     컨텍스트에 등록된 모든 커맨드 핸들러 목록
     * @param recorder     전문 이력 기록기 (JdbcTemplate 빈이 없으면 empty)
     * @return 구성된 SpiderTcpServer 인스턴스
     */
    @Bean
    public CommandDispatcher<JsonCommandRequest, JsonCommandResponse> commandDispatcher(
            List<CommandHandler<JsonCommandRequest, JsonCommandResponse>> handlers) {
        return new CommandDispatcher<>(handlers);
    }

    /** GatewayLoader(spider.gateway.dynamic.enabled=true) 활성 시 비활성화 — 포트 중복 방지 */
    @Bean
    @ConditionalOnProperty(name = "spider.gateway.dynamic.enabled", havingValue = "false", matchIfMissing = true)
    public SpiderTcpServer<JsonCommandRequest, JsonCommandResponse> bizTransferTcpServer(
            ObjectMapper objectMapper,
            CommandDispatcher<JsonCommandRequest, JsonCommandResponse> dispatcher,
            Optional<MessageInstanceRecorder> recorder) {

        return new SpiderTcpServer<>(tcpServerPort, handlerPoolSize, queueCapacity,
                new JsonMessageCodec(objectMapper), dispatcher, recorder.orElse(null));
    }
}

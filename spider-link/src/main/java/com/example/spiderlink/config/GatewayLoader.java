package com.example.spiderlink.config;

import com.example.spiderlink.domain.gateway.dto.GatewayConfig;
import com.example.spiderlink.domain.gateway.mapper.GatewayMapper;
import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.infra.tcp.codec.BankingProtocolMessageCodec;
import com.example.spiderlink.infra.tcp.codec.HeaderBasedMessageCodec;
import com.example.spiderlink.infra.tcp.codec.JsonMessageCodec;
import com.example.spiderlink.infra.tcp.codec.MessageCodec;
import com.example.spiderlink.infra.tcp.parser.FixedLengthParser;
import com.example.spiderlink.infra.tcp.parser.HeaderFieldExtractor;
import com.example.spiderlink.infra.tcp.parser.MessageStructureCache;
import com.example.spidercommon.infra.tcp.handler.CommandDispatcher;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.server.SpiderTcpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * FWK_GATEWAY DB 데이터 기반 TCP 서버 동적 생성기.
 *
 * <p>{@code spider.gateway.dynamic.enabled=true} 일 때 활성화된다.
 * 기동 시 FWK_GATEWAY 테이블에서 게이트웨이 설정(포트·코덱·스레드 수 등)을 읽어
 * SpiderTcpServer를 동적으로 생성한다.</p>
 *
 * <p>기존 @Bean 정적 선언(BizAuthConfig 등) 대비 장점:
 * <ul>
 *   <li>포트·코덱 변경 시 application.yml 수정 없이 DB UPDATE + reload API만으로 반영 가능</li>
 *   <li>Admin에서 FWK_GATEWAY CRUD로 게이트웨이 설정 중앙 관리</li>
 * </ul>
 * </p>
 *
 * <pre>{@code
 * # application.yml 설정 예시
 * spider:
 *   gateway:
 *     dynamic:
 *       enabled: true
 *     id: BIZ_AUTH_GW
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spider.gateway.dynamic.enabled", havingValue = "true")
public class GatewayLoader implements ApplicationRunner {

    /** 이 bizApp이 담당하는 FWK_GATEWAY.GW_ID */
    @Value("${spider.gateway.id}")
    private String gwId;

    private final GatewayMapper gatewayMapper;
    private final ObjectMapper objectMapper;
    private final HeaderFieldExtractor headerFieldExtractor;
    private final MessageStructureCache messageStructureCache;
    private final FixedLengthParser fixedLengthParser;
    private final List<CommandHandler<JsonCommandRequest, JsonCommandResponse>> handlers;
    private final Optional<MessageInstanceRecorder> recorder;

    /** shutdown() 에서 종료할 서버 인스턴스 */
    private SpiderTcpServer<JsonCommandRequest, JsonCommandResponse> server;

    @Override
    public void run(ApplicationArguments args) {
        GatewayConfig config = gatewayMapper.selectGateway(gwId);
        if (config == null) {
            log.error("[GatewayLoader] FWK_GATEWAY 미등록 gwId={} — TCP 서버 기동 중단", gwId);
            return;
        }

        int port        = config.getPort(9995);
        int poolSize    = config.getPoolSize(5);
        int queue       = config.getQueueCapacity(20);
        log.info("[GatewayLoader] gwId={} 설정 로드: port={}, codec={}, pool-size={}, queue={}",
                gwId, port, config.getCodec(), poolSize, queue);

        MessageCodec<JsonCommandRequest, JsonCommandResponse> messageCodec = resolveCodec(config);
        CommandDispatcher<JsonCommandRequest, JsonCommandResponse> dispatcher =
                new CommandDispatcher<>(handlers);

        server = new SpiderTcpServer<>(port, poolSize, queue, messageCodec, dispatcher,
                        recorder.orElse(null));

        // Spring Bean이 아닌 직접 생성 인스턴스이므로 수동으로 run() 호출
        try {
            server.run(args);
        } catch (Exception e) {
            log.error("[GatewayLoader] gwId={} TCP 서버 기동 실패: {}", gwId, e.getMessage(), e);
        }
    }

    /** Spring 종료 시 TCP 서버 소켓·스레드 풀을 정리한다. */
    @PreDestroy
    public void shutdown() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * GW_PROPERTIES에서 코덱 인스턴스를 결정하여 반환한다.
     *
     * <p>우선순위:</p>
     * <ol>
     *   <li>{@code header-length} 설정 → {@link BankingProtocolMessageCodec}
     *       (실제 뱅킹 프로토콜 — 헤더 내 ASCII 길이 필드)</li>
     *   <li>{@code header-msg-id}만 설정 → {@link HeaderBasedMessageCodec}
     *       (POC 내부 프로토콜 — 4byte binary prefix)</li>
     *   <li>미설정 → {@link JsonMessageCodec} (JSON command 필드 방식)</li>
     * </ol>
     */
    private MessageCodec<JsonCommandRequest, JsonCommandResponse> resolveCodec(GatewayConfig config) {
        String orgId = config.getOrgId();
        String headerMessageId = config.getHeaderMessageId();
        Integer headerLength = config.getHeaderLength();

        if (headerLength != null) {
            // 실제 뱅킹 프로토콜: 헤더 내 ASCII 문자열 길이 필드 방식 (참고소스 OrgMessageReader 방식)
            log.info("[GatewayLoader] gwId={} 뱅킹 헤더 코덱 사용: orgId={}, headerMsgId={}, headerLength={}",
                    config.getGwId(), orgId, headerMessageId, headerLength);
            return new BankingProtocolMessageCodec(objectMapper, headerFieldExtractor, orgId, headerMessageId,
                    messageStructureCache, fixedLengthParser,
                    headerLength, config.getLengthFieldOffset(),
                    config.getLengthFieldLength(), config.isTotalLength());
        }

        if (headerMessageId != null && !headerMessageId.isBlank()) {
            // POC 내부 프로토콜: 4byte binary prefix + 고정헤더 + 바디
            log.info("[GatewayLoader] gwId={} 헤더 오프셋 파싱 코덱 사용: orgId={}, headerMsgId={}",
                    config.getGwId(), orgId, headerMessageId);
            return new HeaderBasedMessageCodec(objectMapper, headerFieldExtractor, orgId, headerMessageId,
                    messageStructureCache, fixedLengthParser);
        }

        log.info("[GatewayLoader] gwId={} JSON 코덱 사용 (header-msg-id 미설정)", config.getGwId());
        return new JsonMessageCodec(objectMapper);
    }
}

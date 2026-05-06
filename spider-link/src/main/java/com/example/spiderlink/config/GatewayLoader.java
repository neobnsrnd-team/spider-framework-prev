package com.example.spiderlink.config;

import com.example.spiderlink.domain.gateway.dto.GatewayConfig;
import com.example.spiderlink.domain.gateway.mapper.GatewayMapper;
import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.infra.tcp.codec.HeaderBasedMessageCodec;
import com.example.spiderlink.infra.tcp.codec.JsonMessageCodec;
import com.example.spiderlink.infra.tcp.codec.MessageCodec;
import com.example.spiderlink.infra.tcp.parser.FixedLengthParser;
import com.example.spiderlink.infra.tcp.parser.HeaderOffsetParser;
import com.example.spiderlink.infra.tcp.parser.MessageStructurePool;
import com.example.spidercommon.infra.tcp.handler.CommandDispatcher;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.server.SpiderTcpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final HeaderOffsetParser headerOffsetParser;
    private final MessageStructurePool messageStructurePool;
    private final FixedLengthParser fixedLengthParser;
    private final List<CommandHandler<JsonCommandRequest, JsonCommandResponse>> handlers;
    private final Optional<MessageInstanceRecorder> recorder;

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

        SpiderTcpServer<JsonCommandRequest, JsonCommandResponse> server =
                new SpiderTcpServer<>(port, poolSize, queue, messageCodec, dispatcher,
                        recorder.orElse(null));

        // ApplicationRunner를 직접 호출하여 서버 기동 (Spring이 자동 실행하지 않으므로 수동 실행)
        try {
            server.run(args);
        } catch (Exception e) {
            log.error("[GatewayLoader] gwId={} TCP 서버 기동 실패: {}", gwId, e.getMessage(), e);
        }
    }

    /**
     * GW_PROPERTIES에서 코덱 인스턴스를 결정하여 반환한다.
     *
     * <p>{@code header-msg-id}가 설정되어 있으면 {@link HeaderBasedMessageCodec}을 사용하여
     * 고정길이 헤더에서 REQ_ID_CODE를 추출한다.
     * 미설정이면 기존 {@link JsonMessageCodec}(JSON command 필드 방식)을 사용한다.</p>
     */
    private MessageCodec<JsonCommandRequest, JsonCommandResponse> resolveCodec(GatewayConfig config) {
        String headerMessageId = config.getHeaderMessageId();
        if (headerMessageId != null && !headerMessageId.isBlank()) {
            String orgId = config.getOrgId();
            log.info("[GatewayLoader] gwId={} 헤더 오프셋 파싱 코덱 사용: orgId={}, headerMsgId={}",
                    config.getGwId(), orgId, headerMessageId);
            return new HeaderBasedMessageCodec(objectMapper, headerOffsetParser, orgId, headerMessageId,
                    messageStructurePool, fixedLengthParser);
        }
        log.info("[GatewayLoader] gwId={} JSON 코덱 사용 (header-msg-id 미설정)", config.getGwId());
        return new JsonMessageCodec(objectMapper);
    }
}

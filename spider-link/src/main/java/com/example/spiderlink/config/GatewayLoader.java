package com.example.spiderlink.config;

import com.example.spiderlink.domain.gateway.dto.GatewayConfig;
import com.example.spiderlink.domain.gateway.mapper.GatewayMapper;
import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.infra.tcp.codec.JsonMessageCodec;
import com.example.spiderlink.infra.tcp.codec.MessageCodec;
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
        String codec    = config.getCodec();

        log.info("[GatewayLoader] gwId={} 설정 로드: port={}, codec={}, pool-size={}, queue={}",
                gwId, port, codec, poolSize, queue);

        MessageCodec<JsonCommandRequest, JsonCommandResponse> messageCodec = resolveCodec(codec);
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

    /** GW_PROPERTIES.codec 값으로 MessageCodec 인스턴스를 반환한다. 현재는 JSON만 지원. */
    private MessageCodec<JsonCommandRequest, JsonCommandResponse> resolveCodec(String codec) {
        // 향후 FIXED, XML 등 코덱 추가 시 여기서 분기
        if (!"JSON".equalsIgnoreCase(codec)) {
            log.warn("[GatewayLoader] 미지원 codec={}, JSON으로 fallback", codec);
        }
        return new JsonMessageCodec(objectMapper);
    }
}

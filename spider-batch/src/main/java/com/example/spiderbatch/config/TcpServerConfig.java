package com.example.spiderbatch.config;

import com.example.spiderlink.infra.tcp.codec.ObjectStreamMessageCodec;
import com.example.spiderlink.infra.tcp.handler.CommandDispatcher;
import com.example.spiderlink.infra.tcp.handler.CommandHandler;
import com.example.spiderlink.infra.tcp.model.ManagementContext;
import com.example.spiderlink.infra.tcp.server.SpiderTcpServer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * spider-batch TCP 서버 설정.
 *
 * <p>spider-link의 {@link SpiderTcpServer}에 {@link ObjectStreamMessageCodec}과
 * {@link BatchExecCommandHandler}를 주입하여 Admin ↔ spider-batch 구간 TCP 서버를 구성한다.</p>
 *
 * <p>{@code batch.tcp.enabled=false}로 설정하면 TCP 서버가 비활성화된다.
 * HTTP 전용 환경(REST API만 사용)에서 불필요한 소켓 포트를 열지 않을 때 사용한다.</p>
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.tcp.enabled", matchIfMissing = true)
public class TcpServerConfig {

    private final BatchConfigurationProperties batchProps;

    /**
     * Admin과 ObjectStream 프로토콜로 통신하는 배치 TCP 서버 Bean.
     *
     * <p>Spring이 ApplicationRunner를 구현한 Bean을 자동으로 실행하므로
     * 별도의 시작 코드 없이 애플리케이션 기동 시 서버가 자동 시작된다.</p>
     *
     * <p>{@code CommandHandler<ManagementContext, ManagementContext>} 타입 빈을 모두 수집하므로
     * batch-was에서 추가 핸들러(ScheduleCommandHandler 등)를 등록하면 자동으로 포함된다.</p>
     */
    @Bean
    public SpiderTcpServer<ManagementContext, ManagementContext> batchTcpServer(
            List<CommandHandler<ManagementContext, ManagementContext>> handlers) {

        BatchConfigurationProperties.Tcp tcp = batchProps.getTcp();
        CommandDispatcher<ManagementContext, ManagementContext> dispatcher =
                new CommandDispatcher<>(handlers);

        return new SpiderTcpServer<>(
                tcp.getPort(), tcp.getHandlerPoolSize(), tcp.getQueueCapacity(),
                new ObjectStreamMessageCodec(), dispatcher);
    }
}

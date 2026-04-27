package com.example.spiderbatch.tcp;

import com.example.spiderbatch.domain.batch.dto.BatchExecuteRequest;
import com.example.spiderbatch.domain.batch.dto.BatchExecuteResponse;
import com.example.spiderbatch.domain.batch.service.BatchExecuteService;
import com.example.spiderlink.infra.tcp.handler.CommandHandler;
import com.example.spiderlink.infra.tcp.model.ManagementContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * BATCH_EXEC / PING 커맨드 핸들러.
 *
 * <p>{@link com.example.spiderlink.infra.tcp.server.SpiderTcpServer}에 등록되어
 * Admin으로부터 수신된 배치 실행 요청을 {@link BatchExecuteService}에 위임한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchExecCommandHandler implements CommandHandler<ManagementContext, ManagementContext> {

    private final BatchExecuteService batchExecuteService;

    @Override
    public boolean supports(String command) {
        return "BATCH_EXEC".equals(command) || "PING".equals(command);
    }

    @Override
    public ManagementContext handle(String command, ManagementContext ctx) {
        if ("PING".equals(command)) {
            return ManagementContext.builder()
                    .command(command)
                    .resultCode("PONG")
                    .build();
        }
        return executeBatch(ctx);
    }

    /**
     * BATCH_EXEC 커맨드를 BatchExecuteService에 위임한다.
     * ManagementContext 필드를 BatchExecuteRequest로 매핑하고,
     * 결과를 ManagementContext로 래핑해 반환한다.
     */
    private ManagementContext executeBatch(ManagementContext ctx) {
        try {
            BatchExecuteRequest request = BatchExecuteRequest.builder()
                    .batchAppId(ctx.getBatchAppId())
                    .batchDate(ctx.getBatchDate())
                    .userId(ctx.getUserId())
                    .parameters(ctx.getParameters())
                    .build();

            // TCP 채널은 내부망 신뢰 기반 — 소켓 원격 주소 대신 "TCP_INTERNAL"로 감사 로그 기록
            BatchExecuteResponse response = batchExecuteService.execute(request, "TCP_INTERNAL");

            return ManagementContext.builder()
                    .command(ctx.getCommand())
                    .instanceId(ctx.getInstanceId())
                    .batchAppId(ctx.getBatchAppId())
                    .resultCode(response.getResRtCode())
                    // BatchExecuteResponse.batchExecuteSeq는 int 타입이므로 Integer로 자동 박싱된다
                    .executeSeq(response.getBatchExecuteSeq())
                    .build();

        } catch (Exception e) {
            log.error("[BatchExecCommandHandler] 배치 실행 실패: batchAppId={}, error={}",
                    ctx.getBatchAppId(), e.getMessage(), e);
            return ManagementContext.builder()
                    .command(ctx.getCommand())
                    .instanceId(ctx.getInstanceId())
                    .batchAppId(ctx.getBatchAppId())
                    .resultCode("ERROR")
                    // Exception 직렬화 대신 클래스명 + 메시지를 문자열로 전달 (ObjectStream 호환성/보안)
                    .errorMessage(e.getClass().getName() + ": " + e.getMessage())
                    .build();
        }
    }
}

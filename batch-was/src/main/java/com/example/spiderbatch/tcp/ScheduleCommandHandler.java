package com.example.spiderbatch.tcp;

import com.example.spiderbatch.scheduler.SchedulerManagementService;
import com.example.spiderlink.infra.tcp.handler.CommandHandler;
import com.example.spiderlink.infra.tcp.model.ManagementContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SCHEDULE_TRIGGER / SCHEDULE_CRON_UPDATE 커맨드 핸들러.
 *
 * <p>Admin에서 TCP로 전달된 스케줄 관리 커맨드를 {@link SchedulerManagementService}에 위임한다.
 * {@link com.example.spiderbatch.config.TcpServerConfig}가 {@code List<CommandHandler>}로 빈을 수집하므로
 * 이 컴포넌트는 자동으로 TCP 서버에 등록된다.</p>
 *
 * <ul>
 *   <li>{@code SCHEDULE_TRIGGER}: 특정 배치 Job을 즉시 실행</li>
 *   <li>{@code SCHEDULE_CRON_UPDATE}: 특정 배치의 Cron 표현식 변경 (ManagementContext.cronText 사용)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleCommandHandler implements CommandHandler<ManagementContext, ManagementContext> {

    private final SchedulerManagementService schedulerManagementService;

    @Override
    public boolean supports(String command) {
        return "SCHEDULE_TRIGGER".equals(command) || "SCHEDULE_CRON_UPDATE".equals(command);
    }

    @Override
    public ManagementContext handle(String command, ManagementContext ctx) {
        log.info("[ScheduleCommandHandler] 수신: command={}, batchAppId={}", command, ctx.getBatchAppId());

        try {
            if ("SCHEDULE_TRIGGER".equals(command)) {
                schedulerManagementService.triggerNow(ctx.getBatchAppId());
            } else {
                // SCHEDULE_CRON_UPDATE — cronText가 없으면 Job 삭제 (스케줄 비활성화)
                if (ctx.getCronText() == null || ctx.getCronText().isBlank()) {
                    schedulerManagementService.deleteJob(ctx.getBatchAppId());
                    log.info("[ScheduleCommandHandler] cronText 없음 — Job 삭제: batchAppId={}", ctx.getBatchAppId());
                } else {
                    schedulerManagementService.reschedule(ctx.getBatchAppId(), ctx.getCronText());
                }
            }

            return ManagementContext.builder()
                    .command(command)
                    .instanceId(ctx.getInstanceId())
                    .batchAppId(ctx.getBatchAppId())
                    .resultCode("SUCCESS")
                    .build();

        } catch (Exception e) {
            log.error("[ScheduleCommandHandler] 처리 실패: command={}, batchAppId={}", command, ctx.getBatchAppId(), e);
            return ManagementContext.builder()
                    .command(command)
                    .instanceId(ctx.getInstanceId())
                    .batchAppId(ctx.getBatchAppId())
                    .resultCode("ERROR")
                    .errorMessage(e.getClass().getName() + ": " + e.getMessage())
                    .build();
        }
    }
}

package com.example.bizchannel.domain.notice;

import com.example.spiderlink.infra.tcp.handler.CommandHandler;
import com.example.spiderlink.infra.tcp.model.JsonCommandRequest;
import com.example.spiderlink.infra.tcp.model.JsonCommandResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TCP NOTICE_SYNC / NOTICE_END 커맨드 핸들러.
 *
 * <p>Admin → biz-channel 내장 TCP 서버로 수신된 공지 커맨드를
 * {@link NoticeManager}를 통해 SSE 브로드캐스트로 처리한다.</p>
 *
 * <p>수신 흐름: Admin(8080) → TCP:19400 → SpiderTcpServer → NoticeSyncCommandHandler → NoticeManager → SSE</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeSyncCommandHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final NoticeManager noticeManager;

    @Override
    public boolean supports(String command) {
        return "NOTICE_SYNC".equals(command) || "NOTICE_END".equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        try {
            if ("NOTICE_SYNC".equals(command)) {
                Map<String, Object> payload = request.getPayload();
                noticeManager.broadcast(payload);
                log.info("[NoticeSyncCommandHandler] Notice broadcast done: payload keys={}", payload != null ? payload.keySet() : "null");
            } else {
                // NOTICE_END: null broadcast → NoticeManager sends notice-end event
                noticeManager.broadcast(null);
                log.info("[NoticeSyncCommandHandler] Notice end broadcast done");
            }
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(true)
                    .message("OK")
                    .build();
        } catch (Exception e) {
            log.error("[NoticeSyncCommandHandler] Failed to handle command={}, error={}", command, e.getMessage(), e);
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }
}

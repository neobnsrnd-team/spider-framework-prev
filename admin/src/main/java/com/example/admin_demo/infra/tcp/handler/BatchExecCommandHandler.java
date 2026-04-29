package com.example.admin_demo.infra.tcp.handler;

import com.example.admin_demo.infra.tcp.adapter.BatchManagementAdapter;
import com.example.admin_demo.infra.tcp.model.JsonCommandRequest;
import com.example.admin_demo.infra.tcp.model.JsonCommandResponse;
import com.example.spidercommon.infra.tcp.model.ManagementContext;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * BATCH_EXEC 커맨드 핸들러.
 *
 * <p>Admin TCP 서버(9999)에 수신된 BATCH_EXEC 커맨드를
 * BatchManagementAdapter를 통해 batch-was TCP 서버(9998)로 중계한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchExecCommandHandler implements CommandHandler {

    private final BatchManagementAdapter batchManagementAdapter;

    @Override
    public boolean supports(String command) {
        return "BATCH_EXEC".equals(command);
    }

    @Override
    public Object handle(String command, JsonCommandRequest request) {
        Map<String, Object> payload = request.getPayload();
        if (payload == null) {
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(false)
                    .error("payload 없음")
                    .build();
        }

        ManagementContext ctx = ManagementContext.builder()
                .command(command)
                .instanceId(String.valueOf(payload.getOrDefault("instanceId", "")))
                .batchAppId(String.valueOf(payload.getOrDefault("batchAppId", "")))
                .batchDate(String.valueOf(payload.getOrDefault("batchDate", "")))
                .userId(String.valueOf(payload.getOrDefault("userId", "SYSTEM")))
                .parameters(payload.containsKey("parameters") ? String.valueOf(payload.get("parameters")) : null)
                .build();

        ManagementContext result = batchManagementAdapter.doProcess(command, ctx);

        // 성공 조건: 결과가 존재하고, resultCode가 ERROR가 아니며, errorMessage가 비어 있음
        boolean success = result != null
                && !"ERROR".equals(result.getResultCode())
                && (result.getErrorMessage() == null || result.getErrorMessage().isBlank());

        return JsonCommandResponse.builder()
                .command(command)
                .success(success)
                .message(result != null ? result.getResultCode() : null)
                .error(result != null ? result.getErrorMessage() : null)
                .build();
    }
}

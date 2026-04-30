package com.example.spideradmin.infra.tcp.handler;

import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * USER_CREATE 커맨드 핸들러. TCP 다채널 통신 데모용.
 *
 * <p>실제 DB 저장 로직은 포함하지 않으며 TCP 통신 구조 시연을 목적으로 한다.</p>
 */
@Slf4j
@Component
public class UserCreateCommandHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    @Override
    public boolean supports(String command) {
        return "USER_CREATE".equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        Map<String, Object> payload = request.getPayload();
        String name = payload != null ? String.valueOf(payload.getOrDefault("name", "unknown")) : "unknown";
        String role = payload != null ? String.valueOf(payload.getOrDefault("role", "user")) : "user";

        log.info("[UserCreateCommandHandler] USER_CREATE: name={}, role={}", name, role);

        return JsonCommandResponse.builder()
                .command(command)
                .success(true)
                .message("USER_CREATED:" + name)
                .build();
    }
}

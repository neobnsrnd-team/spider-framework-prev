package com.example.spideradmin.infra.tcp.handler;

import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import org.springframework.stereotype.Component;

/**
 * PING 커맨드 핸들러. 연결 상태 확인용.
 */
@Component
public class PingCommandHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    @Override
    public boolean supports(String command) {
        return "PING".equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        return JsonCommandResponse.builder()
                .command(command)
                .success(true)
                .message("PONG")
                .build();
    }
}

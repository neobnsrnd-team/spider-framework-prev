package com.example.spideradmin.infra.tcp.handler;

import com.example.spideradmin.infra.tcp.model.JsonCommandRequest;
import com.example.spideradmin.infra.tcp.model.JsonCommandResponse;
import org.springframework.stereotype.Component;

/**
 * PING 커맨드 핸들러. 연결 상태 확인용.
 */
@Component
public class PingCommandHandler implements CommandHandler {

    @Override
    public boolean supports(String command) {
        return "PING".equals(command);
    }

    @Override
    public Object handle(String command, JsonCommandRequest request) {
        return JsonCommandResponse.builder()
                .command(command)
                .success(true)
                .message("PONG")
                .build();
    }
}

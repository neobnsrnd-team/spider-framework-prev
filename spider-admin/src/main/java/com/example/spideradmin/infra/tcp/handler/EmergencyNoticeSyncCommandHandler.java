package com.example.spideradmin.infra.tcp.handler;

import com.example.spideradmin.infra.tcp.adapter.BizChannelAdapter;
import com.example.spideradmin.infra.tcp.model.JsonCommandRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * NOTICE_SYNC / NOTICE_END 커맨드 핸들러.
 *
 * <p>Admin TCP 서버(9999)에 수신된 긴급공지 커맨드를
 * BizChannelAdapter를 통해 biz-channel 내장 TCP 서버(19400)로 중계한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencyNoticeSyncCommandHandler implements CommandHandler {

    private final BizChannelAdapter bizChannelAdapter;

    @Override
    public boolean supports(String command) {
        return "NOTICE_SYNC".equals(command) || "NOTICE_END".equals(command);
    }

    @Override
    public Object handle(String command, JsonCommandRequest request) {
        return bizChannelAdapter.doProcess(command, request);
    }
}

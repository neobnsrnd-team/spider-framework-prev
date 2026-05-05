package com.example.spideradmin.infra.tcp.handler;

import com.example.spideradmin.infra.tcp.adapter.BizChannelAdapter;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * NOTICE_SYNC / NOTICE_END 커맨드 핸들러.
 *
 * <p>Admin TCP 서버(9999)에 수신된 긴급공지 커맨드를
 * BizChannelAdapter를 통해 biz-channel 내장 TCP 서버(19400)로 중계한다.</p>
 *
 * <p><b>이 핸들러가 Admin TCP 서버에 등록된 이유:</b>
 * 배치 서버 등 외부 시스템이 Admin TCP 9999로 긴급공지 커맨드를 보낼 경우
 * Admin이 biz-channel로 중계하는 프록시 경로를 제공한다.
 * Admin UI 배포 경로(EmergencyNoticeDeployService → BizChannelAdapter)와는 별개로 동작한다.</p>
 *
 * <p><b>주의:</b> Admin이 biz-channel에 NOTICE_SYNC를 직접 전송하는 경로와 순환 참조가 발생하지 않도록
 * 외부 시스템은 Admin TCP(9999)로만 전송하고, Admin이 biz-channel TCP(19400)로 중계하는 단방향 흐름을 유지해야 한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencyNoticeSyncCommandHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final BizChannelAdapter bizChannelAdapter;

    @Override
    public boolean supports(String command) {
        return "NOTICE_SYNC".equals(command) || "NOTICE_END".equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        return bizChannelAdapter.doProcess(command, request);
    }
}

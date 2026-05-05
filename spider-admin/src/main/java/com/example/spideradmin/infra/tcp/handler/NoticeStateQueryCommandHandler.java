package com.example.spideradmin.infra.tcp.handler;

import com.example.spideradmin.infra.tcp.adapter.EmergencyNoticeQueryAdapter;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * NOTICE_STATE_QUERY 커맨드 핸들러.
 *
 * <p>biz-channel 기동 시 Admin TCP 서버(9999)로 현재 긴급공지 배포 상태를 조회하는 커맨드를 처리한다.
 * 응답 payload는 NOTICE_SYNC 페이로드와 동일한 구조에 deployStatus 필드가 추가된다.</p>
 *
 * <p>수신 흐름: biz-channel 기동 → AdminClient → Admin TCP:9999 → NoticeStateQueryCommandHandler → EmergencyNoticeQueryAdapter</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeStateQueryCommandHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final EmergencyNoticeQueryAdapter emergencyNoticeQueryAdapter;

    @Override
    public boolean supports(String command) {
        return "NOTICE_STATE_QUERY".equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        try {
            Map<String, Object> payload = emergencyNoticeQueryAdapter.buildNoticeStatePayload();
            log.info("[NoticeStateQueryCommandHandler] 공지 상태 조회 완료: deployStatus={}", payload.get("deployStatus"));
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(true)
                    .payload(payload)
                    .build();
        } catch (Exception e) {
            log.error("[NoticeStateQueryCommandHandler] 공지 상태 조회 실패: {}", e.getMessage(), e);
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }
}

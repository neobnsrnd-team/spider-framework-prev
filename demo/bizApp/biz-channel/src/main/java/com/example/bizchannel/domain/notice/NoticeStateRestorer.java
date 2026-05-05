package com.example.bizchannel.domain.notice;

import com.example.bizchannel.client.AdminClient;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * biz-channel 기동 시 Admin TCP 서버에서 긴급공지 배포 상태를 복원하는 컴포넌트.
 *
 * <p>Admin이 biz-channel보다 먼저 기동되어 공지를 배포한 경우,
 * biz-channel의 인메모리 상태가 비어 있어 프론트엔드에 공지가 표시되지 않는
 * 기동 순서 의존 싱크 문제를 해결한다.</p>
 *
 * <p>Admin TCP 연결 실패 시 warn 로그만 출력하고 기동을 계속 진행한다 (비치명적).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeStateRestorer implements ApplicationRunner {

    private final AdminClient adminClient;
    private final NoticeManager noticeManager;

    @Override
    public void run(ApplicationArguments args) {
        try {
            JsonCommandResponse response = adminClient.queryNoticeState();

            if (!response.isSuccess()) {
                log.warn("[NoticeStateRestorer] Admin TCP 공지 상태 조회 실패 — 기동 시 공지 미복원 "
                        + "(Admin이 기동 중인지 확인): error={}", response.getError());
                return;
            }

            Map<String, Object> payload = response.getPayload();
            if (payload == null) {
                log.warn("[NoticeStateRestorer] Admin 응답 payload 없음 — 공지 미복원");
                return;
            }

            String deployStatus = (String) payload.get("deployStatus");
            if ("DEPLOYED".equals(deployStatus)) {
                // deployStatus는 SSE payload에 그대로 포함해도 무방하다 — 프론트엔드 NoticePayload 인터페이스에
                // 정의되지 않은 추가 필드이므로 TypeScript가 무시하고, NoticeManager는 받은 Map을 그대로 전달한다.
                noticeManager.broadcast(payload);
                log.info("[NoticeStateRestorer] 긴급공지 상태 복원 완료 (DEPLOYED)");
            } else {
                log.info("[NoticeStateRestorer] 현재 배포 중인 공지 없음 (deployStatus={}), 복원 건너뜀", deployStatus);
            }
        } catch (Exception e) {
            // 공지 복원 실패가 서버 기동을 막지 않도록 모든 예외를 catch
            log.warn("[NoticeStateRestorer] 공지 상태 복원 중 예외 발생 (비치명적) — 기동 계속 진행", e);
        }
    }
}

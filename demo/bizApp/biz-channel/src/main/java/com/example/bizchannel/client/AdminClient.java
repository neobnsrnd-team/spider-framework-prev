package com.example.bizchannel.client;

import com.example.bizchannel.config.BizChannelConfig;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.client.TcpClient;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Admin TCP 서버(기본 9999) 통신 클라이언트.
 *
 * <p>biz-channel → Admin 방향의 TCP 요청을 단일 진입점으로 제공한다.
 * 현재는 기동 시 긴급공지 배포 상태 조회(NOTICE_STATE_QUERY)에 사용된다.
 * 접속 정보는 {@link BizChannelConfig}에서 일괄 관리한다.</p>
 *
 * <pre>{@code
 *   // 공지 상태 조회 예시
 *   JsonCommandResponse resp = adminClient.queryNoticeState();
 *   if (resp.isSuccess()) { ... }
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminClient {

    private final TcpClient tcpClient;
    private final BizChannelConfig config;

    /**
     * Admin TCP 서버에서 현재 긴급공지 배포 상태를 조회한다.
     *
     * <p>Admin이 기동 중이지 않거나 TCP 연결에 실패하면
     * {@code success=false} 응답을 반환하며 예외를 전파하지 않는다.</p>
     *
     * @return 공지 상태 조회 응답 — {@code success=false}이면 연결 실패
     */
    public JsonCommandResponse queryNoticeState() {
        JsonCommandRequest request = JsonCommandRequest.builder()
                .command("NOTICE_STATE_QUERY")
                .requestId(UUID.randomUUID().toString())
                .build();
        try {
            log.debug("[AdminClient] → Admin TCP 공지 상태 조회: host={}:{}",
                    config.getAdminTcpHost(), config.getAdminTcpPort());
            return tcpClient.sendJson(config.getAdminTcpHost(), config.getAdminTcpPort(), request);
        } catch (IOException e) {
            log.warn("[AdminClient] Admin TCP 연결 실패 (공지 상태 복원 건너뜀): {}", e.getMessage());
            return JsonCommandResponse.builder()
                    .command("NOTICE_STATE_QUERY")
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }
}

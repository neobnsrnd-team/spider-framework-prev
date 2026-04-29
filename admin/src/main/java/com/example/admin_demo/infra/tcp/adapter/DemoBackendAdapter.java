package com.example.admin_demo.infra.tcp.adapter;

import com.example.admin_demo.infra.tcp.client.TcpClient;
import com.example.admin_demo.infra.tcp.model.JsonCommandRequest;
import com.example.admin_demo.infra.tcp.model.JsonCommandResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Admin → biz-channel 내장 TCP 서버 통신 어댑터.
 *
 * <p>JsonCommandRequest를 4바이트 길이 프리픽스 + UTF-8 JSON 형식으로 전송한다.</p>
 *
 * <p>biz-channel은 spider-link를 내장하여 TCP 서버(기본 19400)를 직접 운영한다.
 * standalone spider-link 프로세스 없이 biz-channel 기동만으로 통신이 가능하다.</p>
 *
 * <p>설정값 {@code tcp.demo-backend.host/port}는 biz-channel 내장 TCP 서버 주소를 가리킨다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoBackendAdapter implements ManagementAdapter<JsonCommandRequest, JsonCommandResponse> {

    private final TcpClient tcpClient;

    @Value("${tcp.demo-backend.host:localhost}")
    private String demoBackendHost;

    @Value("${tcp.demo-backend.port:19400}")
    private int demoBackendPort;

    /** biz-channel은 별도 프로세스이므로 로컬 실행 없음 */
    @Override
    public boolean isLocal() {
        return false;
    }

    /**
     * biz-channel 내장 TCP 서버(19400)에 JsonCommandRequest를 전송한다.
     *
     * @param command 실행 커맨드 (NOTICE_SYNC, NOTICE_END, PING 등)
     * @param req     JsonCommandRequest 인스턴스
     * @return 응답 JsonCommandResponse
     */
    @Override
    public JsonCommandResponse doProcess(String command, JsonCommandRequest req) {
        try {
            log.info(
                    "[DemoBackendAdapter] JSON TCP 전송: host={}, port={}, command={} (spider-link 경유)",
                    demoBackendHost,
                    demoBackendPort,
                    command);
            return tcpClient.sendJson(demoBackendHost, demoBackendPort, req);
        } catch (IOException e) {
            log.error(
                    "[DemoBackendAdapter] TCP 전송 실패: command={}, host={}:{}, error={} — biz-channel이 기동 중인지 확인하세요.",
                    command,
                    demoBackendHost,
                    demoBackendPort,
                    e.getMessage());
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }
}

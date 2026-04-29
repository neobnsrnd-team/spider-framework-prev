package com.example.admin_demo.infra.tcp.client;

import com.example.admin_demo.infra.tcp.model.JsonCommandRequest;
import com.example.admin_demo.infra.tcp.model.JsonCommandResponse;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 기동 시 TCP 연결 샘플 동작 확인용 Runner.
 *
 * <p>dev 프로파일에서만 활성화된다.
 * biz-channel 내장 TCP 서버(19400)에 PING을 전송하여 연결 상태를 확인한다.</p>
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class TcpClientRunner implements ApplicationRunner {

    private final TcpClient tcpClient;

    @Value("${tcp.biz-channel.host:localhost}")
    private String bizChannelHost;

    @Value("${tcp.biz-channel.port:19400}")
    private int bizChannelPort;

    @Override
    public void run(ApplicationArguments args) {
        // 서버가 완전히 기동될 때까지 잠시 대기
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        JsonCommandRequest ping = JsonCommandRequest.builder()
                .command("PING")
                .requestId(UUID.randomUUID().toString())
                .payload(Map.of())
                .build();

        try {
            JsonCommandResponse resp = tcpClient.sendJson(bizChannelHost, bizChannelPort, ping);
            log.info(
                    "[TcpClientRunner] spider-link PING 응답: success={}, message={}",
                    resp.isSuccess(),
                    resp.getMessage());
        } catch (Exception e) {
            log.warn("[TcpClientRunner] spider-link PING 실패 (비치명적): {}", e.getMessage());
        }
    }
}

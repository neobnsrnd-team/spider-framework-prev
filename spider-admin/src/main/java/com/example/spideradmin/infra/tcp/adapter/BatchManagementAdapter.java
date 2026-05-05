package com.example.spideradmin.infra.tcp.adapter;

import com.example.spideradmin.domain.wasinstance.dto.WasInstanceResponse;
import com.example.spideradmin.domain.wasinstance.mapper.WasInstanceMapper;
import com.example.spidercommon.infra.tcp.model.ManagementContext;
import com.example.spiderlink.infra.tcp.client.TcpClient;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Admin ↔ batch-was 간 TCP 통신 어댑터.
 *
 * <p>레퍼런스(spiderlink_Admin SocketManagementAdapter) 방식으로
 * isLocal() 분기를 통해 로컬/원격 실행을 결정한다.</p>
 *
 * <p>원격 실행: ManagementContext를 Java ObjectStream 바이너리로 직렬화하여 전송한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchManagementAdapter implements ManagementAdapter<ManagementContext, ManagementContext> {

    private final TcpClient tcpClient;
    private final WasInstanceMapper wasInstanceMapper;

    /** batch-was TCP 서버 포트 (기본값 9998) */
    @Value("${tcp.batch-was.port:9998}")
    private int batchWasTcpPort;

    /**
     * Admin과 batch-was는 항상 별도 프로세스.
     * 로컬 직접 실행 경로는 미사용 (TODO 추후 통합 배포 시 활용 가능).
     */
    @Override
    public boolean isLocal() {
        return false;
    }

    /**
     * batch-was TCP 서버에 ManagementContext를 전송한다.
     *
     * @param command 실행 커맨드 (BATCH_EXEC, PING 등)
     * @param ctx     ManagementContext 인스턴스
     * @return 응답 ManagementContext, 실패 시 예외 정보가 담긴 ManagementContext
     */
    @Override
    public ManagementContext doProcess(String command, ManagementContext ctx) {
        WasInstanceResponse instance = wasInstanceMapper.selectResponseById(ctx.getInstanceId());
        if (instance == null || instance.getIp() == null || instance.getIp().isBlank()) {
            log.warn("[BatchManagementAdapter] WAS 인스턴스 정보 없음: instanceId={}", ctx.getInstanceId());
            return ManagementContext.builder()
                    .command(command)
                    .instanceId(ctx.getInstanceId())
                    .resultCode("ERROR")
                    .build();
        }

        // FWK_WAS_INSTANCE.PORT는 TCP 관리 포트(9998)를 저장한다.
        // 인스턴스별 포트가 설정된 경우 우선 사용, 없으면 전역 설정 포트로 폴백.
        // (HTTP 모니터링 포트는 BatchRunningService가 별도 설정값 batch.was.http-port로 관리)
        int port = batchWasTcpPort;
        if (instance.getPort() != null && !instance.getPort().isBlank()) {
            try {
                port = Integer.parseInt(instance.getPort().trim());
            } catch (NumberFormatException e) {
                log.warn(
                        "[BatchManagementAdapter] 인스턴스 포트 파싱 실패, 전역 포트({}) 사용: instanceId={}, port={}",
                        batchWasTcpPort,
                        ctx.getInstanceId(),
                        instance.getPort());
            }
        }

        try {
            log.info("[BatchManagementAdapter] TCP 전송: host={}, port={}, command={}", instance.getIp(), port, command);
            return tcpClient.sendObject(instance.getIp(), port, ctx);
        } catch (IOException e) {
            log.warn(
                    "[BatchManagementAdapter] TCP 전송 실패: instanceId={}, error={}", ctx.getInstanceId(), e.getMessage());
            return ManagementContext.builder()
                    .command(command)
                    .instanceId(ctx.getInstanceId())
                    .resultCode("ERROR")
                    .build();
        }
    }
}

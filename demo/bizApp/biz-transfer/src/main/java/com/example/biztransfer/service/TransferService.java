package com.example.biztransfer.service;

import com.example.spiderlink.infra.tcp.client.TcpClient;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * TRANSFER_* 커맨드를 mock-core의 CORE_* 커맨드로 중계하는 서비스.
 *
 * <p>TcpClient를 통해 mock-core(기본 localhost:19300)에 JSON 요청을 전송하고,
 * 응답의 커맨드 이름을 인바운드 TRANSFER_* 이름으로 재기록하여 반환한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TcpClient tcpClient;

    /** mock-core 호스트 (환경변수 MOCK_CORE_HOST 또는 기본값 localhost) */
    @Value("${mock.core.host:localhost}")
    private String mockCoreHost;

    /** mock-core TCP 포트 (환경변수 MOCK_CORE_PORT 또는 기본값 19300) */
    @Value("${mock.core.port:19300}")
    private int mockCorePort;

    /** FWK_MESSAGE 조회 기관 ID (기본값: DEMO) */
    @Value("${app.org-id:DEMO}")
    private String orgId;

    /**
     * 인바운드 요청의 페이로드를 그대로 사용해 mock-core로 전달한다.
     *
     * @param transferCmd 인바운드 커맨드명 (TRANSFER_*)
     * @param coreCmd     아웃바운드 커맨드명 (CORE_*)
     * @param inboundReq  채널에서 수신한 원본 요청
     * @return mock-core 응답 (커맨드명을 transferCmd로 재기록)
     */
    public JsonCommandResponse forward(String transferCmd, String coreCmd,
                                       JsonCommandRequest inboundReq) {
        return forward(transferCmd, coreCmd, inboundReq, inboundReq.getPayload());
    }

    /**
     * 명시적 페이로드를 사용해 mock-core로 전달한다.
     *
     * <p>pin 필드 제거 등 페이로드를 가공해야 하는 경우에 사용한다.</p>
     *
     * @param transferCmd 인바운드 커맨드명 (TRANSFER_*)
     * @param coreCmd     아웃바운드 커맨드명 (CORE_*)
     * @param inboundReq  채널에서 수신한 원본 요청 (requestId 재사용 목적)
     * @param payload     mock-core로 전달할 가공된 페이로드
     * @return mock-core 응답 (커맨드명을 transferCmd로 재기록)
     */
    public JsonCommandResponse forward(String transferCmd, String coreCmd,
                                       JsonCommandRequest inboundReq,
                                       Map<String, Object> payload) {

        // 인바운드 requestId를 유지해 end-to-end 트래킹이 가능하도록 한다
        JsonCommandRequest coreRequest = JsonCommandRequest.builder()
                .command(coreCmd)
                .requestId(inboundReq.getRequestId())
                .payload(payload)
                .build();

        try {
            JsonCommandResponse resp = tcpClient.send(mockCoreHost, mockCorePort, orgId, coreRequest);

            // 응답 커맨드명을 인바운드 TRANSFER_* 이름으로 재기록하여 클라이언트가 구분할 수 있게 한다
            return JsonCommandResponse.builder()
                    .command(transferCmd)
                    .success(resp.isSuccess())
                    .message(resp.getMessage())
                    .error(resp.getError())
                    .payload(resp.getPayload())
                    .build();

        } catch (IOException e) {
            log.error("[TransferService] mock-core 연결 실패 — host={}, port={}, cause={}",
                    mockCoreHost, mockCorePort, e.getMessage());
            return JsonCommandResponse.builder()
                    .command(transferCmd)
                    .success(false)
                    .error("계정계 연결 실패: " + e.getMessage())
                    .build();
        }
    }
}

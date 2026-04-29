package com.example.bizchannel.client;

import com.example.spiderlink.infra.tcp.client.TcpClient;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * AP 서버 TCP 통신 중앙 클라이언트.
 *
 * <p>spider-link {@link TcpClient} 를 감싸서 인증AP(biz-auth)와
 * 이체AP(biz-transfer) 로의 요청 전송을 단일 진입점으로 제공한다.</p>
 *
 * <p>각 요청마다 랜덤 {@code requestId} 를 생성하여 {@link JsonCommandRequest} 를 빌드하고,
 * 설정된 호스트·포트로 JSON 직렬화 TCP 통신을 수행한다.</p>
 *
 * <pre>{@code
 *   // 인증AP 호출 예시
 *   JsonCommandResponse resp = bizClient.sendToAuth(BizCommands.AUTH_LOGIN, payload);
 *
 *   // 이체AP 호출 예시
 *   JsonCommandResponse resp = bizClient.sendToTransfer(BizCommands.TRANSFER_CARD_LIST, payload);
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BizClient {

    /** spider-link TCP 클라이언트 (스프링 자동 등록 @Component) */
    private final TcpClient tcpClient;

    /** 인증AP(biz-auth) 접속 호스트 */
    @Value("${biz.auth.host:localhost}")
    private String authHost;

    /** 인증AP(biz-auth) TCP 포트 */
    @Value("${biz.auth.port:19100}")
    private int authPort;

    /** 이체AP(biz-transfer) 접속 호스트 */
    @Value("${biz.transfer.host:localhost}")
    private String transferHost;

    /** 이체AP(biz-transfer) TCP 포트 */
    @Value("${biz.transfer.port:19200}")
    private int transferPort;

    /**
     * 인증AP(biz-auth, TCP 19100) 로 커맨드를 전송하고 응답을 반환한다.
     *
     * @param command   전송할 커맨드 이름 ({@link com.example.bizcommon.BizCommands} 의 AUTH_* 상수)
     * @param payload   요청 페이로드 (커맨드별 필요 파라미터)
     * @param requestId HTTP 수신 시 생성된 거래 추적 UUID — TCP 로그와 동일한 TRX_TRACKING_NO로 연결됨
     * @return 인증AP 의 응답 객체
     * @throws IOException TCP 연결 실패 또는 I/O 오류 발생 시
     */
    public JsonCommandResponse sendToAuth(String command, Map<String, Object> payload, String requestId) throws IOException {
        JsonCommandRequest req = JsonCommandRequest.builder()
                .command(command)
                .requestId(requestId)
                .payload(payload)
                .build();
        log.debug("[BizClient] → biz-auth | command={} host={}:{} requestId={}", command, authHost, authPort, requestId);
        return tcpClient.sendJson(authHost, authPort, req);
    }

    /**
     * 이체AP(biz-transfer, TCP 19200) 로 커맨드를 전송하고 응답을 반환한다.
     *
     * @param command   전송할 커맨드 이름 ({@link com.example.bizcommon.BizCommands} 의 TRANSFER_* 상수)
     * @param payload   요청 페이로드 (커맨드별 필요 파라미터)
     * @param requestId HTTP 수신 시 생성된 거래 추적 UUID — TCP 로그와 동일한 TRX_TRACKING_NO로 연결됨
     * @return 이체AP 의 응답 객체
     * @throws IOException TCP 연결 실패 또는 I/O 오류 발생 시
     */
    public JsonCommandResponse sendToTransfer(String command, Map<String, Object> payload, String requestId) throws IOException {
        JsonCommandRequest req = JsonCommandRequest.builder()
                .command(command)
                .requestId(requestId)
                .payload(payload)
                .build();
        log.debug("[BizClient] → biz-transfer | command={} host={}:{} requestId={}", command, transferHost, transferPort, requestId);
        return tcpClient.sendJson(transferHost, transferPort, req);
    }
}

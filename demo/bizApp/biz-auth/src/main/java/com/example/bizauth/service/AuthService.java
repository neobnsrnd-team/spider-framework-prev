package com.example.bizauth.service;

import com.example.bizcommon.BizCommands;
import com.example.spiderlink.infra.tcp.client.TcpClient;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 인증AP 비즈니스 로직 서비스.
 *
 * <p>채널AP 로부터 수신한 인증·조회 요청을 계정계 Mock(mock-core) 에 위임한다.
 * 내부적으로 {@link TcpClient} 를 사용해 TCP 소켓 통신을 수행하며,
 * 연결 실패 시 실패 응답을 직접 구성하여 반환한다.</p>
 *
 * <pre>{@code
 *   // 로그인 흐름
 *   AUTH_LOGIN (userId, password) → CORE_USER_AUTH → mock-core → AUTH_LOGIN 응답
 *
 *   // 사용자 조회 흐름
 *   AUTH_ME (userId) → CORE_USER_QUERY → mock-core → AUTH_ME 응답
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** spider-link 가 자동 등록하는 TCP 발신 클라이언트 빈 */
    private final TcpClient tcpClient;

    /** 계정계 Mock 호스트 (기본값: localhost) */
    @Value("${mock.core.host:localhost}")
    private String mockCoreHost;

    /** 계정계 Mock TCP 포트 (기본값: 19300) */
    @Value("${mock.core.port:19300}")
    private int mockCorePort;

    /**
     * 로그인 요청을 계정계 Mock 에 위임한다.
     *
     * <p>인바운드 커맨드(AUTH_LOGIN)를 계정계 커맨드(CORE_USER_AUTH)로 변환하여 전송하며,
     * 응답의 커맨드 이름은 호출자가 AUTH_LOGIN 으로 덮어쓴다.</p>
     *
     * @param inboundRequest 채널AP 로부터 수신한 AUTH_LOGIN 요청
     * @return mock-core 의 응답 (성공 시 userId, userName, userGrade, lastLoginDtime 포함)
     */
    public JsonCommandResponse login(JsonCommandRequest inboundRequest) {
        // 인바운드 페이로드(userId, password)를 그대로 전달하고 커맨드만 CORE_USER_AUTH 로 변환
        JsonCommandRequest coreRequest = JsonCommandRequest.builder()
                .command(BizCommands.CORE_USER_AUTH)
                .requestId(inboundRequest.getRequestId())
                .payload(inboundRequest.getPayload())
                .build();

        try {
            return tcpClient.sendJson(mockCoreHost, mockCorePort, coreRequest);
        } catch (IOException e) {
            log.error("[AuthService] mock-core 연결 실패 (login): {}", e.getMessage());
            // 계정계 연결 장애를 실패 응답으로 변환 — 커맨드는 호출자가 AUTH_LOGIN 으로 덮어씀
            return JsonCommandResponse.builder()
                    .command(BizCommands.AUTH_LOGIN)
                    .success(false)
                    .error("계정계 연결 실패: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 사용자 정보 조회 요청을 계정계 Mock 에 위임한다.
     *
     * <p>인바운드 커맨드(AUTH_ME)를 계정계 커맨드(CORE_USER_QUERY)로 변환하여 전송하며,
     * 응답의 커맨드 이름은 호출자가 AUTH_ME 로 덮어쓴다.</p>
     *
     * @param inboundRequest 채널AP 로부터 수신한 AUTH_ME 요청 (userId 포함)
     * @return mock-core 의 응답 (성공 시 userName, userGrade, lastLoginDtime 포함)
     */
    public JsonCommandResponse getMe(JsonCommandRequest inboundRequest) {
        // 인바운드 페이로드(userId)를 그대로 전달하고 커맨드만 CORE_USER_QUERY 로 변환
        JsonCommandRequest coreRequest = JsonCommandRequest.builder()
                .command(BizCommands.CORE_USER_QUERY)
                .requestId(inboundRequest.getRequestId())
                .payload(inboundRequest.getPayload())
                .build();

        try {
            return tcpClient.sendJson(mockCoreHost, mockCorePort, coreRequest);
        } catch (IOException e) {
            log.error("[AuthService] mock-core 연결 실패 (getMe): {}", e.getMessage());
            // 계정계 연결 장애를 실패 응답으로 변환 — 커맨드는 호출자가 AUTH_ME 로 덮어씀
            return JsonCommandResponse.builder()
                    .command(BizCommands.AUTH_ME)
                    .success(false)
                    .error("계정계 연결 실패: " + e.getMessage())
                    .build();
        }
    }
}

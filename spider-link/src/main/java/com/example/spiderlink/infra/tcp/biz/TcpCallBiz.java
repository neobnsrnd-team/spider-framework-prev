package com.example.spiderlink.infra.tcp.biz;

import com.example.spiderlink.infra.tcp.client.TcpClient;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 외부시스템 TCP 호출 Biz 구현체.
 *
 * <p>FWK_COMPONENT 에 아래와 같이 등록한다:</p>
 * <pre>{@code
 *   COMPONENT_TYPE       = 'B'
 *   COMPONENT_CLASS_NAME = 'com.example.spiderlink.infra.tcp.biz.TcpCallBiz'
 *   COMPONENT_METHOD_NAME = '외부시스템에 전송할 TCP 커맨드명'
 * }</pre>
 *
 * <p>접속 대상 호스트·포트는 {@code application.yml} 의 {@code tcp.ext.*} 로 설정한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TcpCallBiz implements Biz {

    private final TcpClient tcpClient;

    /** 외부시스템 TCP 호스트 — application.yml tcp.ext.host */
    @Value("${tcp.ext.host:localhost}")
    private String host;

    /** 외부시스템 TCP 포트 — application.yml tcp.ext.port */
    @Value("${tcp.ext.port:19100}")
    private int port;

    /** 기관 ID — FWK_MESSAGE 전문 구조 조회 키 (application.yml app.org-id) */
    @Value("${app.org-id:DEMO}")
    private String orgId;

    /**
     * 외부시스템으로 TCP 전문을 송신하고 응답 페이로드를 반환한다.
     *
     * @param methodName 호출할 TCP 커맨드명 ({@code COMPONENT_METHOD_NAME})
     * @param params     요청 페이로드 (FWK_RELATION_PARAM 기반 바인딩 값)
     * @return 외부시스템 응답 페이로드 — 다음 스텝 컨텍스트에 병합됨
     * @throws Exception TCP 연결 실패 또는 외부시스템 오류 응답 시
     */
    @Override
    public Map<String, Object> execute(String methodName, Map<String, Object> params) throws Exception {
        JsonCommandRequest req = JsonCommandRequest.builder()
                .command(methodName)
                .requestId(UUID.randomUUID().toString())
                .payload(params)
                .build();

        log.debug("[TcpCallBiz] TCP 호출: host={}:{}, command={}", host, port, methodName);

        JsonCommandResponse response = tcpClient.send(host, port, orgId, req);
        if (!response.isSuccess()) {
            throw new RuntimeException("외부시스템 TCP 호출 실패: command=" + methodName
                    + ", error=" + response.getError());
        }

        return response.getPayload() != null ? response.getPayload() : Map.of();
    }
}

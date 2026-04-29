package com.example.admin_demo.infra.tcp.client;

import com.example.admin_demo.infra.tcp.model.JsonCommandRequest;
import com.example.admin_demo.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.model.ManagementContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TCP 소켓 기반 전송/수신 클라이언트.
 *
 * <p>두 가지 프로토콜을 지원한다:</p>
 * <ul>
 *   <li>ObjectStream: Admin ↔ batch-was 구간 (Java 직렬화, 타입 안전)</li>
 *   <li>JSON: Admin ↔ biz-channel 구간 (4바이트 길이 프리픽스 + UTF-8 JSON)</li>
 * </ul>
 *
 * <p>소켓 옵션은 레퍼런스(spiderlink_Admin ManagementClientWorker)를 준수한다:</p>
 * <ul>
 *   <li>연결 타임아웃: 2초</li>
 *   <li>읽기 타임아웃: 60초</li>
 *   <li>setKeepAlive(true)</li>
 *   <li>setTcpNoDelay(true)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TcpClient {

    /** 연결 타임아웃:F 레퍼런스(spiderlink_Admin) 기준 2초 */
    private static final int CONNECT_TIMEOUT_MS = 2_000;

    /** 읽기 타임아웃: 레퍼런스(spiderlink_Admin) 기준 60초 (배치 실행 대기 포함) */
    private static final int READ_TIMEOUT_MS = 60_000;

    /** JSON 응답 최대 허용 크기 (1 MB) — 초과 시 OutOfMemoryError 방지를 위해 즉시 예외 발생 */
    private static final int MAX_MSG_LEN = 1024 * 1024;

    /**
     * ObjectStream 역직렬화 허용 화이트리스트.
     * ObjectStreamMessageCodec과 동일한 패턴을 사용하여 일관성 유지.
     */
    private static final String OBJECT_STREAM_FILTER =
            "com.example.spiderlink.infra.tcp.model.ManagementContext;java.lang.*;java.util.*;!*";

    private final ObjectMapper objectMapper;

    /**
     * batch-was TCP 서버에 ManagementContext를 ObjectStream으로 전송하고 응답을 수신한다.
     *
     * @param host 대상 호스트
     * @param port 대상 포트 (기본 9998)
     * @param ctx  전송할 ManagementContext
     * @return 응답 ManagementContext
     */
    public ManagementContext sendObject(String host, int port, ManagementContext ctx) throws IOException {
        log.debug("[TcpClient] ObjectStream 전송: host={}, port={}, command={}", host, port, ctx.getCommand());
        // OOS를 flush하여 스트림 헤더가 상대방에 전달된 후에 OIS를 여는 것이 중요하다.
        // (OIS 생성 시 스트림 헤더를 읽어들이기 때문에 순서가 뒤바뀌면 데드락이 발생할 수 있다.)
        try (Socket socket = createSocket(host, port)) {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(ctx);
            oos.flush();
            // ObjectInputFilter 화이트리스트로 허용된 클래스 외 역직렬화 차단 (RCE 방어)
            // ObjectStreamMessageCodec과 동일한 방식으로 통일
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter(OBJECT_STREAM_FILTER));
            ManagementContext response = (ManagementContext) ois.readObject();
            log.debug("[TcpClient] ObjectStream 응답 수신: resultCode={}", response.getResultCode());
            return response;
        } catch (ClassNotFoundException e) {
            throw new IOException("ManagementContext 역직렬화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * biz-channel 내장 TCP 서버에 JsonCommandRequest를 JSON 형식으로 전송하고 응답을 수신한다.
     * 전송 포맷: [4바이트 길이(int)] + [UTF-8 JSON 바이트열]
     *
     * @param host 대상 호스트
     * @param port 대상 포트 (기본 19400)
     * @param req  전송할 JsonCommandRequest
     * @return 응답 JsonCommandResponse
     */
    public JsonCommandResponse sendJson(String host, int port, JsonCommandRequest req) throws IOException {
        log.debug("[TcpClient] JSON 전송: host={}, port={}, command={}", host, port, req.getCommand());
        byte[] requestBytes = objectMapper.writeValueAsBytes(req);
        // DataOutputStream / DataInputStream을 try-with-resources로 열어 명시적으로 close 보장
        try (Socket socket = createSocket(host, port);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            dos.writeInt(requestBytes.length);
            dos.write(requestBytes);
            dos.flush();

            int len = dis.readInt();
            // 음수 또는 허용 최대 크기(1 MB) 초과 시 비정상 요청으로 간주하여 즉시 예외 발생
            if (len < 0 || len > MAX_MSG_LEN) {
                throw new IOException("수신된 메시지 길이가 허용 범위를 초과합니다: " + len);
            }
            byte[] responseBytes = new byte[len];
            dis.readFully(responseBytes);
            JsonCommandResponse response = objectMapper.readValue(responseBytes, JsonCommandResponse.class);
            log.debug("[TcpClient] JSON 응답 수신: success={}", response.isSuccess());
            return response;
        }
    }

    /**
     * 소켓을 생성하고 레퍼런스 기준 소켓 옵션을 적용한다.
     *
     * @param host 대상 호스트
     * @param port 대상 포트
     * @return 설정이 완료된 Socket
     */
    private Socket createSocket(String host, int port) throws IOException {
        Socket socket = new Socket();
        // Nagle 알고리즘 비활성화: 소규모 커맨드 메시지의 지연 최소화
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        socket.setSoTimeout(READ_TIMEOUT_MS);
        return socket;
    }
}

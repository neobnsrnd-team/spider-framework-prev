package com.example.spiderlink.infra.tcp.client;

import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.infra.tcp.model.JsonCommandRequest;
import com.example.spiderlink.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.parser.FixedLengthParser;
import com.example.spiderlink.infra.tcp.parser.MessageStructure;
import com.example.spiderlink.infra.tcp.parser.MessageStructurePool;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * spider-link → demo/backend TCP 소켓 클라이언트.
 *
 * <p>프로토콜: 4바이트 길이 프리픽스(int, big-endian) + UTF-8 JSON 바이트열.
 * Admin의 TcpClient.sendJson()과 동일한 포맷을 사용한다.</p>
 *
 * <p>소켓 옵션 (레퍼런스 spiderlink_Admin 기준):</p>
 * <ul>
 *   <li>연결 타임아웃: 2초</li>
 *   <li>읽기 타임아웃: 60초</li>
 *   <li>setKeepAlive(true)</li>
 *   <li>setTcpNoDelay(true)</li>
 * </ul>
 *
 * <p>{@link MessageInstanceRecorder}를 주입하면 송신 요청과 수신 응답을
 * {@code FWK_MESSAGE_INSTANCE} 테이블에 자동 기록한다.</p>
 */
@Slf4j
@Component
public class TcpClient {

    /** 연결 타임아웃: 레퍼런스(spiderlink_Admin) 기준 2초 */
    private static final int CONNECT_TIMEOUT_MS = 2_000;

    /** 읽기 타임아웃: 레퍼런스(spiderlink_Admin) 기준 60초 (배치 실행 대기 포함) */
    private static final int READ_TIMEOUT_MS = 60_000;

    /** 수신 메시지 최대 허용 크기 (1 MB) — 초과 시 OutOfMemoryError 방지를 위해 즉시 예외 발생 */
    private static final int MAX_MSG_LEN = 1024 * 1024;

    private final ObjectMapper objectMapper;

    /** 전문 이력 기록기 — null이면 DB 기록 생략 */
    @Nullable
    private final MessageInstanceRecorder recorder;

    /** 전문 구조 캐시 — null이면 고정길이 프로토콜 미사용(JSON 고정) */
    @Nullable
    private final MessageStructurePool structurePool;

    /** 고정길이 파서/직렬화기 — structurePool 이 null이면 미사용 */
    @Nullable
    private final FixedLengthParser fixedLengthParser;

    public TcpClient(ObjectMapper objectMapper,
                     @Nullable MessageInstanceRecorder recorder,
                     @Nullable MessageStructurePool structurePool,
                     @Nullable FixedLengthParser fixedLengthParser) {
        this.objectMapper     = objectMapper;
        this.recorder         = recorder;
        this.structurePool    = structurePool;
        this.fixedLengthParser = fixedLengthParser;
    }

    /** 고정길이 미사용 생성자 — 기존 코드 호환용 */
    public TcpClient(ObjectMapper objectMapper, @Nullable MessageInstanceRecorder recorder) {
        this(objectMapper, recorder, null, null);
    }

    /** recorder/고정길이 모두 미사용 편의 생성자 — 테스트 등 */
    public TcpClient(ObjectMapper objectMapper) {
        this(objectMapper, null, null, null);
    }

    /**
     * FWK_MESSAGE.MESSAGE_TYPE 을 확인하여 고정길이('F') 또는 JSON 프로토콜로 자동 전환한다.
     *
     * <p>structurePool 이 null 이거나 REQ 전문 구조가 등록되지 않은 경우 JSON fallback.</p>
     *
     * @param host   대상 호스트
     * @param port   대상 포트
     * @param orgId  기관 ID (FWK_MESSAGE 조회 키)
     * @param req    전송할 JsonCommandRequest
     * @return 응답 JsonCommandResponse
     * @throws IOException 소켓 연결/전송/수신 실패 시
     */
    public JsonCommandResponse send(String host, int port, String orgId, JsonCommandRequest req)
            throws IOException {
        String command     = req.getCommand();
        String reqMsgId    = command + "_REQ";
        String resMsgId    = command + "_RES";

        Optional<MessageStructure> reqStructure = structurePool != null
                ? structurePool.get(orgId, reqMsgId)
                : Optional.empty();

        boolean useFixed = reqStructure.isPresent()
                && "F".equals(reqStructure.get().getMessageType());

        log.debug("[TcpClient] send: host={}, port={}, command={}, protocol={}",
                host, port, command, useFixed ? "FIXED" : "JSON");

        String trxId = UUID.randomUUID().toString();
        if (recorder != null) {
            recorder.recordClientRequest(trxId, req, host, port);
        }

        byte[] requestBytes;
        if (useFixed) {
            Map<String, Object> dataMap = new LinkedHashMap<>();
            dataMap.put("COMMAND", command);
            dataMap.put("REQUEST_ID", trxId);
            if (req.getPayload() != null) {
                dataMap.putAll(req.getPayload());
            }
            requestBytes = fixedLengthParser.serialize(reqStructure.get(), dataMap);
        } else {
            requestBytes = objectMapper.writeValueAsBytes(req);
        }

        try (Socket socket = createSocket(host, port);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeInt(requestBytes.length);
            dos.write(requestBytes);
            dos.flush();

            int len = dis.readInt();
            if (len < 0 || len > MAX_MSG_LEN) {
                throw new IOException("수신된 메시지 길이가 허용 범위를 초과합니다: " + len);
            }
            byte[] responseBytes = new byte[len];
            dis.readFully(responseBytes);

            JsonCommandResponse response;
            if (useFixed) {
                Optional<MessageStructure> resStructure = structurePool.get(orgId, resMsgId);
                if (resStructure.isPresent()) {
                    Map<String, Object> resMap = fixedLengthParser.parse(resStructure.get(), responseBytes);
                    response = toCommandResponse(command, resMap);
                } else {
                    log.warn("[TcpClient] RES 전문 구조 미등록({}), JSON fallback", resMsgId);
                    response = objectMapper.readValue(responseBytes, JsonCommandResponse.class);
                }
            } else {
                response = objectMapper.readValue(responseBytes, JsonCommandResponse.class);
            }

            log.debug("[TcpClient] send 응답: success={}", response.isSuccess());
            if (recorder != null) {
                recorder.recordClientResponse(trxId, req, response, host, port);
            }
            return response;
        }
    }

    /**
     * 고정길이 응답 Map을 JsonCommandResponse 로 변환한다.
     * SUCCESS('Y'/'N') / ERROR_MSG 를 매핑하고 나머지를 payload 로 사용한다.
     */
    private JsonCommandResponse toCommandResponse(String command, Map<String, Object> resMap) {
        boolean success = "Y".equalsIgnoreCase(
                String.valueOf(resMap.getOrDefault("SUCCESS", "N")).trim());
        String errorMsg = String.valueOf(resMap.getOrDefault("ERROR_MSG", "")).trim();

        Map<String, Object> payload = new LinkedHashMap<>(resMap);
        payload.remove("SUCCESS");
        payload.remove("ERROR_MSG");

        return JsonCommandResponse.builder()
                .command(command)
                .success(success)
                .error(success ? null : errorMsg.isEmpty() ? null : errorMsg)
                .payload(payload)
                .build();
    }

    /**
     * 대상 TCP 서버에 JsonCommandRequest를 JSON 형식으로 전송하고 응답을 수신한다.
     * recorder가 설정된 경우 송신 요청과 수신 응답을 FWK_MESSAGE_INSTANCE에 기록한다.
     *
     * @param host 대상 호스트
     * @param port 대상 포트
     * @param req  전송할 JsonCommandRequest
     * @return 응답 JsonCommandResponse
     * @throws IOException 소켓 연결/전송/수신 실패 시
     */
    public JsonCommandResponse sendJson(String host, int port, JsonCommandRequest req) throws IOException {
        log.debug("[TcpClient] JSON 전송: host={}, port={}, command={}", host, port, req.getCommand());

        String trxId = UUID.randomUUID().toString();
        if (recorder != null) {
            recorder.recordClientRequest(trxId, req, host, port);
        }

        byte[] requestBytes = objectMapper.writeValueAsBytes(req);

        try (Socket socket = createSocket(host, port);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeInt(requestBytes.length);
            dos.write(requestBytes);
            dos.flush();

            int len = dis.readInt();
            // 음수 또는 허용 최대 크기(1 MB) 초과 시 비정상 응답으로 간주하여 즉시 예외 발생
            if (len < 0 || len > MAX_MSG_LEN) {
                throw new IOException("수신된 메시지 길이가 허용 범위를 초과합니다: " + len);
            }
            byte[] responseBytes = new byte[len];
            dis.readFully(responseBytes);

            JsonCommandResponse response = objectMapper.readValue(responseBytes, JsonCommandResponse.class);
            log.debug("[TcpClient] JSON 응답 수신: success={}", response.isSuccess());

            if (recorder != null) {
                recorder.recordClientResponse(trxId, req, response, host, port);
            }

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

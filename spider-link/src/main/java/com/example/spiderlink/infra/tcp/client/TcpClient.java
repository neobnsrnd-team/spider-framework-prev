package com.example.spiderlink.infra.tcp.client;

import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.infra.tcp.client.pool.PooledSocket;
import com.example.spiderlink.infra.tcp.client.pool.SocketPool;
import com.example.spiderlink.infra.tcp.client.pool.SocketPoolManager;
import com.example.spiderlink.infra.tcp.model.JsonCommandRequest;
import com.example.spiderlink.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.parser.FixedLengthParser;
import com.example.spiderlink.infra.tcp.parser.MessageStructure;
import com.example.spiderlink.infra.tcp.parser.MessageStructurePool;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
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
 * <p>소켓 관리 방식: {@link SocketPoolManager}를 통해 (host:port) 단위로 소켓을 재사용한다.
 * 풀 매니저가 없는 경우(테스트 등) 요청마다 신규 소켓을 생성한다.</p>
 *
 * <p>재연결 로직: IOException 발생 시 최대 {@value RETRY_COUNT}회 재시도하고,
 * {@link UnknownHostException}은 재시도 없이 즉시 실패한다.</p>
 *
 * <p>참고소스(spiderLink_Admin) {@code MessageClientWorker.connect()} 기준:
 * 재시도 3회, 재시도 간격 2ms.</p>
 */
@Slf4j
@Component
public class TcpClient {

    /** 수신 메시지 최대 허용 크기 (1 MB) — 초과 시 OutOfMemoryError 방지를 위해 즉시 예외 발생 */
    private static final int MAX_MSG_LEN = 1024 * 1024;

    /** IOException 재시도 횟수 — 참고소스 기준값 3회 */
    private static final int RETRY_COUNT = 3;
    /** 재시도 간격 (ms) — 참고소스 기준값 2ms */
    private static final long RETRY_DELAY_MS = 2L;

    private final ObjectMapper objectMapper;

    /** 전문 이력 기록기 — null이면 DB 기록 생략 */
    @Nullable
    private final MessageInstanceRecorder recorder;

    /** 전문 구조 캐시 — null이면 고정길이 프로토콜 미사용(JSON 고정) */
    @Nullable
    private final MessageStructurePool structurePool;

    /** 고정길이 파서/직렬화기 — structurePool이 null이면 미사용 */
    @Nullable
    private final FixedLengthParser fixedLengthParser;

    /** 소켓 커넥션 풀 매니저 — null이면 요청마다 신규 소켓 생성 */
    @Nullable
    private final SocketPoolManager poolManager;

    public TcpClient(ObjectMapper objectMapper,
                     @Nullable MessageInstanceRecorder recorder,
                     @Nullable MessageStructurePool structurePool,
                     @Nullable FixedLengthParser fixedLengthParser,
                     @Nullable SocketPoolManager poolManager) {
        this.objectMapper     = objectMapper;
        this.recorder         = recorder;
        this.structurePool    = structurePool;
        this.fixedLengthParser = fixedLengthParser;
        this.poolManager      = poolManager;
    }

    /** 고정길이 미사용 생성자 — 기존 코드 호환용 */
    public TcpClient(ObjectMapper objectMapper, @Nullable MessageInstanceRecorder recorder) {
        this(objectMapper, recorder, null, null, null);
    }

    /** 모든 선택 의존성 없는 생성자 — 테스트용 */
    public TcpClient(ObjectMapper objectMapper) {
        this(objectMapper, null, null, null, null);
    }

    /**
     * FWK_MESSAGE.MESSAGE_TYPE을 확인하여 고정길이('F') 또는 JSON 프로토콜로 자동 전환한다.
     *
     * <p>structurePool이 null이거나 REQ 전문 구조가 등록되지 않은 경우 JSON fallback.</p>
     *
     * @param host  대상 호스트
     * @param port  대상 포트
     * @param orgId 기관 ID (FWK_MESSAGE 조회 키)
     * @param req   전송할 JsonCommandRequest
     * @return 응답 JsonCommandResponse
     * @throws IOException 소켓 연결/전송/수신 실패 시
     */
    public JsonCommandResponse send(String host, int port, String orgId, JsonCommandRequest req)
            throws IOException {
        String command  = req.getCommand();
        String reqMsgId = command + "_REQ";
        String resMsgId = command + "_RES";

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

        byte[] responseBytes = sendWithRetry(host, port, requestBytes);

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
        byte[] responseBytes = sendWithRetry(host, port, requestBytes);

        JsonCommandResponse response = objectMapper.readValue(responseBytes, JsonCommandResponse.class);
        log.debug("[TcpClient] JSON 응답 수신: success={}", response.isSuccess());

        if (recorder != null) {
            recorder.recordClientResponse(trxId, req, response, host, port);
        }
        return response;
    }

    /**
     * 재시도 래퍼 — IOException 발생 시 최대 {@value RETRY_COUNT}회 재시도한다.
     *
     * <p>{@link UnknownHostException}은 재시도 없이 즉시 재발생한다.
     * 참고소스(spiderLink_Admin) MessageClientWorker 기준: 3회, 2ms 간격.</p>
     */
    private byte[] sendWithRetry(String host, int port, byte[] requestBytes) throws IOException {
        IOException lastException = null;
        for (int attempt = 0; attempt <= RETRY_COUNT; attempt++) {
            if (attempt > 0) {
                log.warn("[TcpClient] 재시도 {}회 ({}:{})", attempt, host, port);
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw lastException;
                }
            }
            try {
                return doSend(host, port, requestBytes);
            } catch (UnknownHostException e) {
                // 호스트 해석 실패 — 재시도해도 동일하므로 즉시 실패
                throw e;
            } catch (IOException e) {
                lastException = e;
                log.warn("[TcpClient] 전송 실패 (시도={}, {}:{}) — {}", attempt + 1, host, port, e.getMessage());
            }
        }
        throw lastException;
    }

    /**
     * 단일 전송·수신 — 풀 매니저가 있으면 풀에서 소켓을 빌리고, 없으면 신규 생성한다.
     *
     * @return 수신된 응답 바이트 배열
     * @throws IOException 소켓 연결/전송/수신 실패 시
     */
    private byte[] doSend(String host, int port, byte[] requestBytes) throws IOException {
        if (poolManager != null) {
            PooledSocket pooled = null;
            boolean success = false;
            try {
                pooled = poolManager.borrow(host, port);
                byte[] responseBytes = doSendReceive(pooled.getOut(), pooled.getIn(), requestBytes);
                success = true;
                return responseBytes;
            } finally {
                if (pooled != null) {
                    poolManager.release(host, port, pooled, success);
                }
            }
        }

        // 풀 미사용 — 요청마다 신규 소켓 생성 (테스트 등 폴백)
        try (java.net.Socket socket = createFreshSocket(host, port);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            return doSendReceive(dos, dis, requestBytes);
        }
    }

    /**
     * 실제 4byte int 길이헤더 + 바이트열 전송 및 응답 수신.
     *
     * @param dos          DataOutputStream
     * @param dis          DataInputStream
     * @param requestBytes 전송할 바이트 배열
     * @return 수신된 응답 바이트 배열
     * @throws IOException 전송/수신 실패 시
     */
    private byte[] doSendReceive(DataOutputStream dos, DataInputStream dis, byte[] requestBytes)
            throws IOException {
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
        return responseBytes;
    }

    /**
     * 풀 미사용 폴백용 단순 소켓 생성 — 소켓 옵션은 SocketPool과 동일하게 적용한다.
     */
    private java.net.Socket createFreshSocket(String host, int port) throws IOException {
        java.net.Socket socket = new java.net.Socket();
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.connect(new java.net.InetSocketAddress(host, port), SocketPool.CONNECT_TIMEOUT_MS);
        socket.setSoTimeout(SocketPool.READ_TIMEOUT_MS);
        return socket;
    }

    /**
     * 고정길이 응답 Map을 JsonCommandResponse로 변환한다.
     * SUCCESS('Y'/'N') / ERROR_MSG를 매핑하고 나머지를 payload로 사용한다.
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
}

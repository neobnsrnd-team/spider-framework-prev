package com.example.spiderlink.infra.tcp.codec;

import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.parser.FixedLengthParser;
import com.example.spiderlink.infra.tcp.parser.MessageStructure;
import com.example.spiderlink.infra.tcp.parser.MessageStructurePool;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 고정길이 바이너리 TCP 프로토콜 코덱 (4바이트 길이 프리픽스 + 고정길이 바이너리).
 *
 * <p>FWK_MESSAGE.MESSAGE_TYPE = 'F' 인 전문에 적용되며, SpiderTcpServer가
 * 레거시 금융 시스템의 고정길이 바이너리 요청을 수신·처리할 수 있게 한다.</p>
 *
 * <pre>{@code
 * // 사용 예: 고정길이 바이너리를 수신하는 SpiderTcpServer 구성
 * FixedLengthMessageCodec codec = new FixedLengthMessageCodec(
 *     "DEMO", structurePool, fixedLengthParser);
 * SpiderTcpServer<JsonCommandRequest, JsonCommandResponse> server =
 *     new SpiderTcpServer<>(port, poolSize, queueCapacity, codec, dispatcher, recorder);
 * }</pre>
 *
 * <h4>프레이밍</h4>
 * <ul>
 *   <li>decode: 4바이트 길이 프리픽스 → byte[] 읽기 → FWK_MESSAGE_FIELD 구조 기반 파싱
 *               → JsonCommandRequest (payload = 헤더 제외 필드 Map)</li>
 *   <li>encode: JsonCommandResponse → FWK_MESSAGE_FIELD 구조 기반 직렬화
 *               → 4바이트 길이 프리픽스 + byte[] 전송</li>
 * </ul>
 *
 * <h4>REQ 헤더 규약 (공통)</h4>
 * <pre>
 * COMMAND(C,20) + REQUEST_ID(C,36) → payload 제외, JsonCommandRequest 헤더 필드로 매핑
 * </pre>
 *
 * <h4>RES 구조 규약</h4>
 * <pre>
 * SUCCESS(C,1) → success(Y=true/N=false)
 * ERROR_MSG(K,200) → error 필드
 * 나머지 → payload Map
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class FixedLengthMessageCodec implements MessageCodec<JsonCommandRequest, JsonCommandResponse> {

    /** 수신 메시지 최대 허용 크기 (1 MB) */
    private static final int MAX_MSG_LEN = 1024 * 1024;

    /** REQ 전문 커맨드 필드 길이 (COMMAND: C,20) */
    private static final int COMMAND_FIELD_LEN = 20;

    /** REQ 전문 요청 ID 필드 길이 (REQUEST_ID: C,36) */
    private static final int REQUEST_ID_FIELD_LEN = 36;

    /** FWK_MESSAGE 조회 기관 ID */
    private final String orgId;

    private final MessageStructurePool structurePool;
    private final FixedLengthParser fixedLengthParser;

    /**
     * 고정길이 바이너리 바이트열을 읽어 JsonCommandRequest 로 변환한다.
     *
     * <p>첫 20바이트에서 커맨드를 추출하고, FWK_MESSAGE에 등록된 COMMAND_REQ 구조로
     * 전체 바이트를 파싱하여 헤더(COMMAND, REQUEST_ID)를 제외한 필드를 payload에 담는다.</p>
     *
     * @param in 소켓 입력 스트림
     * @return 파싱된 JsonCommandRequest
     * @throws IOException 스트림 읽기 오류 또는 구조 미등록 커맨드
     */
    @Override
    public JsonCommandRequest decode(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);
        int length = dis.readInt();
        if (length < 0 || length > MAX_MSG_LEN) {
            throw new IOException("수신된 메시지 길이가 허용 범위를 초과합니다: " + length);
        }
        byte[] bytes = new byte[length];
        dis.readFully(bytes);

        // 첫 20바이트에서 커맨드 추출
        int cmdLen = Math.min(COMMAND_FIELD_LEN, bytes.length);
        String command = new String(bytes, 0, cmdLen).trim();

        // 36바이트에서 requestId 추출
        String requestId = "";
        if (bytes.length >= COMMAND_FIELD_LEN + REQUEST_ID_FIELD_LEN) {
            requestId = new String(bytes, COMMAND_FIELD_LEN, REQUEST_ID_FIELD_LEN).trim();
        }

        log.debug("[FixedLengthMessageCodec] decode: command={}, requestId={}", command, requestId);

        // FWK_MESSAGE에서 REQ 전문 구조 조회
        Optional<MessageStructure> structureOpt = structurePool.get(orgId, command + "_REQ");
        if (structureOpt.isEmpty()) {
            log.warn("[FixedLengthMessageCodec] REQ 전문 구조 미등록: orgId={}, messageId={}_REQ",
                    orgId, command);
            throw new IOException("등록되지 않은 커맨드입니다: " + command);
        }

        // 전체 바이트를 구조 기반으로 파싱
        Map<String, Object> parsed = fixedLengthParser.parse(structureOpt.get(), bytes);

        // 헤더 필드(COMMAND, REQUEST_ID)는 payload에서 제거
        Map<String, Object> payload = new LinkedHashMap<>(parsed);
        payload.remove("COMMAND");
        payload.remove("REQUEST_ID");

        return JsonCommandRequest.builder()
                .command(command)
                .requestId(requestId.isEmpty() ? null : requestId)
                .payload(payload)
                .build();
    }

    /**
     * JsonCommandResponse 를 고정길이 바이너리로 직렬화하여 스트림에 쓴다.
     *
     * <p>FWK_MESSAGE에 등록된 COMMAND_RES 구조로 직렬화하며,
     * success/error 를 SUCCESS(C,1)/ERROR_MSG(K,200) 필드에 매핑한다.
     * 구조가 없으면 최소 오류 응답을 고정 오프셋으로 기록한다.</p>
     *
     * @param out      소켓 출력 스트림
     * @param response 직렬화할 JsonCommandResponse
     * @throws IOException 직렬화 또는 스트림 쓰기 오류
     */
    @Override
    public void encode(OutputStream out, JsonCommandResponse response) throws IOException {
        String command = response.getCommand();
        log.debug("[FixedLengthMessageCodec] encode: command={}, success={}", command, response.isSuccess());

        byte[] bytes;
        Optional<MessageStructure> structureOpt = structurePool.get(orgId, command + "_RES");

        if (structureOpt.isPresent()) {
            Map<String, Object> resMap = buildResMap(response);
            bytes = fixedLengthParser.serialize(structureOpt.get(), resMap);
        } else {
            // RES 구조 미등록: SUCCESS(C,1) + ERROR_MSG(K,200) 최소 응답
            log.warn("[FixedLengthMessageCodec] RES 전문 구조 미등록: orgId={}, messageId={}_RES",
                    orgId, command);
            bytes = buildFallbackResponse(response);
        }

        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(bytes.length);
        dos.write(bytes);
        dos.flush();
    }

    /**
     * JsonCommandResponse 를 고정길이 직렬화용 Map 으로 변환한다.
     *
     * <p>SUCCESS / ERROR_MSG 를 공통 헤더 필드로 매핑하고,
     * payload 필드를 나머지에 병합한다.</p>
     */
    private Map<String, Object> buildResMap(JsonCommandResponse response) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("SUCCESS", response.isSuccess() ? "Y" : "N");
        map.put("ERROR_MSG", response.isSuccess() ? "" : (response.getError() != null ? response.getError() : ""));
        if (response.getPayload() != null) {
            map.putAll(response.getPayload());
        }
        return map;
    }

    /**
     * RES 구조 미등록 시 고정 오프셋으로 최소 응답을 생성한다.
     *
     * <p>SUCCESS(C,1) + ERROR_MSG(K,200) 만 기록한다.</p>
     */
    private byte[] buildFallbackResponse(JsonCommandResponse response) {
        // SUCCESS: 1바이트 (ASCII)
        byte successByte = response.isSuccess() ? (byte) 'Y' : (byte) 'N';

        // ERROR_MSG: 200바이트 (EUC-KR, 우측 공백 패딩)
        String errorMsg = response.isSuccess() ? "" : (response.getError() != null ? response.getError() : "");
        byte[] errorBytes;
        try {
            errorBytes = errorMsg.getBytes("EUC-KR");
        } catch (Exception e) {
            errorBytes = errorMsg.getBytes();
        }

        byte[] result = new byte[201]; // 1 + 200
        result[0] = successByte;
        java.util.Arrays.fill(result, 1, 201, (byte) ' ');
        int copyLen = Math.min(errorBytes.length, 200);
        System.arraycopy(errorBytes, 0, result, 1, copyLen);
        return result;
    }
}

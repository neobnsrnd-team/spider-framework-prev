package com.example.spiderlink.infra.tcp.codec;

import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.parser.HeaderOffsetParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 고정길이 헤더 + JSON 바디 혼합 프로토콜 코덱.
 *
 * <p>수신 전문 구조:</p>
 * <pre>
 * [4byte 바이너리 length] [헤더 고정길이 byte[]] [바디 JSON bytes]
 *                          ↑ FWK_MESSAGE_FIELD 오프셋으로 파싱
 * </pre>
 *
 * <p>헤더에서 REQ_ID_CODE를 추출하여 {@link JsonCommandRequest#getCommand()}에 매핑한다.
 * 이를 통해 JSON command 필드 없이도 FWK_LISTENER_TRX_MESSAGE 기반 라우팅이 가능하다.</p>
 *
 * <p>참고소스 DefaultHeaderParser 방식을 FWK_MESSAGE_FIELD 메타 기반으로 재구현.
 * 바디 포맷(JSON/고정길이/XML)이 달라도 헤더 파싱은 동일하게 처리된다.</p>
 *
 * <p>응답은 {@link JsonMessageCodec}과 동일하게 4byte length + JSON으로 직렬화한다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class HeaderBasedMessageCodec implements MessageCodec<JsonCommandRequest, JsonCommandResponse> {

    /** 수신 메시지 최대 허용 크기 (1 MB) */
    private static final int MAX_MSG_LEN = 1024 * 1024;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final HeaderOffsetParser headerOffsetParser;
    /** FWK_MESSAGE.ORG_ID — 헤더 전문 구조 조회 키 */
    private final String orgId;
    /** FWK_MESSAGE.MESSAGE_ID (HEADER_YN='Y') — 헤더 필드 오프셋 정의 */
    private final String headerMessageId;

    /**
     * 수신 바이트 스트림을 디코딩하여 {@link JsonCommandRequest}로 변환한다.
     *
     * <ol>
     *   <li>4byte 바이너리 int → 전체 메시지 길이</li>
     *   <li>헤더 byte[] → {@link HeaderOffsetParser}로 REQ_ID_CODE, REQUEST_ID 추출</li>
     *   <li>바디 byte[] → JSON 파싱하여 payload Map 구성</li>
     * </ol>
     */
    @Override
    public JsonCommandRequest decode(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);
        int length = dis.readInt();
        if (length < 0 || length > MAX_MSG_LEN) {
            throw new IOException("수신된 메시지 길이가 허용 범위를 초과합니다: " + length);
        }

        byte[] message = new byte[length];
        dis.readFully(message);

        // 헤더 오프셋 파싱 — REQ_ID_CODE: 라우팅 키, REQUEST_ID: 요청 추적
        String reqIdCode = headerOffsetParser.extractReqIdCode(orgId, headerMessageId, message);
        if (reqIdCode == null || reqIdCode.isBlank()) {
            throw new IOException("헤더에서 REQ_ID_CODE 추출 실패: orgId=" + orgId + ", headerMsgId=" + headerMessageId);
        }

        String requestId = headerOffsetParser.extractField(orgId, headerMessageId, message, "REQUEST_ID");

        // 헤더 길이만큼 건너뛴 위치부터 바디(JSON)
        int headerLen = headerOffsetParser.calcHeaderLength(orgId, headerMessageId);
        Map<String, Object> payload = parseBody(message, headerLen);

        log.debug("[HeaderBasedMessageCodec] 수신: reqIdCode={}, requestId={}, headerLen={}",
                reqIdCode, requestId, headerLen);

        return JsonCommandRequest.builder()
                .command(reqIdCode.strip())
                .requestId(requestId != null ? requestId.strip() : null)
                .payload(payload)
                .build();
    }

    /**
     * 응답을 4byte length + JSON으로 직렬화한다.
     * {@link JsonMessageCodec}과 동일한 포맷 — 클라이언트 측 디코더 변경 불필요.
     */
    @Override
    public void encode(OutputStream out, JsonCommandResponse response) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(response);
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(bytes.length);
        dos.write(bytes);
        dos.flush();
    }

    /**
     * 바디 byte[]를 JSON Map으로 파싱한다.
     *
     * <p>헤더 이후 바이트가 없거나 비어있으면 빈 Map을 반환한다.
     * 바디 포맷이 JSON이 아닌 경우(향후 고정길이 바디 지원 시) 여기서 분기한다.</p>
     */
    private Map<String, Object> parseBody(byte[] message, int headerLen) {
        if (headerLen >= message.length) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(message, headerLen, message.length - headerLen, MAP_TYPE);
        } catch (IOException e) {
            log.warn("[HeaderBasedMessageCodec] 바디 JSON 파싱 실패, 빈 payload 반환: {}", e.getMessage());
            return Map.of();
        }
    }
}

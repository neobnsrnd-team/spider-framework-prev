package com.example.spiderlink.infra.tcp.codec;

import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.parser.FixedLengthParser;
import com.example.spiderlink.infra.tcp.parser.HeaderOffsetParser;
import com.example.spiderlink.infra.tcp.parser.MessageStructurePool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 고정길이 헤더 + 가변 바디 혼합 프로토콜 코덱.
 *
 * <p>수신 전문 구조:</p>
 * <pre>
 * [4byte 바이너리 length] [헤더 고정길이 byte[]] [바디 byte[]]
 *                          ↑ FWK_MESSAGE_FIELD 오프셋으로 파싱
 * </pre>
 *
 * <p>헤더에서 REQ_ID_CODE를 추출한 뒤, 해당 REQ_ID_CODE로 FWK_MESSAGE를 조회하여
 * MESSAGE_TYPE에 따라 바디 파싱 전략을 동적으로 결정한다.</p>
 * <ul>
 *   <li>MESSAGE_TYPE='F' (고정길이) → {@link FixedLengthParser}</li>
 *   <li>그 외 또는 미등록 전문 → JSON 파싱 (기존 동작)</li>
 * </ul>
 *
 * <p>참고소스 DefaultHeaderParser 방식을 FWK_MESSAGE_FIELD 메타 기반으로 재구현.
 * REQ_ID_CODE → FWK_MESSAGE 조회로 바디 포맷을 판별하는 방식도 참고소스와 동일하다.</p>
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
    /** REQ_ID_CODE → FWK_MESSAGE 조회 후 바디 타입 결정에 사용 */
    private final MessageStructurePool messageStructurePool;
    /** MESSAGE_TYPE='F' 고정길이 바디 파싱 — JSON fallback 시 미사용 */
    private final FixedLengthParser fixedLengthParser;

    /**
     * 수신 바이트 스트림을 디코딩하여 {@link JsonCommandRequest}로 변환한다.
     *
     * <ol>
     *   <li>4byte 바이너리 int → 전체 메시지 길이</li>
     *   <li>헤더 byte[] → {@link HeaderOffsetParser}로 REQ_ID_CODE, REQUEST_ID 추출</li>
     *   <li>REQ_ID_CODE → FWK_MESSAGE 조회 → MESSAGE_TYPE에 따라 바디 파싱</li>
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

        // 헤더 오프셋 파싱 — REQ_ID_CODE: 라우팅 키 및 바디 전문 구조 조회 키
        String reqIdCode = headerOffsetParser.extractReqIdCode(orgId, headerMessageId, message);
        if (reqIdCode == null || reqIdCode.isBlank()) {
            throw new IOException("헤더에서 REQ_ID_CODE 추출 실패: orgId=" + orgId + ", headerMsgId=" + headerMessageId);
        }

        String requestId = headerOffsetParser.extractField(orgId, headerMessageId, message, "REQUEST_ID");

        int headerLen = headerOffsetParser.calcHeaderLength(orgId, headerMessageId);
        // REQ_ID_CODE로 FWK_MESSAGE 조회 → MESSAGE_TYPE 기반 바디 파싱
        Map<String, Object> payload = parseBody(message, headerLen, reqIdCode.strip());

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
     * 헤더 이후 바디 byte[]를 파싱한다.
     *
     * <p>REQ_ID_CODE로 FWK_MESSAGE를 조회하여 MESSAGE_TYPE을 확인한다.
     * MESSAGE_TYPE='F'(고정길이)이면 {@link FixedLengthParser}로, 그 외이면 JSON으로 파싱한다.
     * 전문이 미등록된 경우에도 JSON fallback을 적용하여 기존 동작과 호환성을 유지한다.</p>
     */
    private Map<String, Object> parseBody(byte[] message, int headerLen, String reqIdCode) {
        if (headerLen >= message.length) {
            return Map.of();
        }
        byte[] bodyBytes = Arrays.copyOfRange(message, headerLen, message.length);

        // REQ_ID_CODE → FWK_MESSAGE 조회 → MESSAGE_TYPE='F'이면 고정길이 파싱
        return messageStructurePool.get(orgId, reqIdCode)
                .filter(structure -> "F".equals(structure.getMessageType()))
                .map(structure -> {
                    log.debug("[HeaderBasedMessageCodec] 바디 고정길이 파싱: reqIdCode={}", reqIdCode);
                    return fixedLengthParser.parse(structure, bodyBytes);
                })
                .orElseGet(() -> parseJson(reqIdCode, bodyBytes));
    }

    /** JSON 바디 파싱 — 파싱 실패 시 빈 Map 반환 */
    private Map<String, Object> parseJson(String reqIdCode, byte[] bodyBytes) {
        try {
            return objectMapper.readValue(bodyBytes, MAP_TYPE);
        } catch (IOException e) {
            log.warn("[HeaderBasedMessageCodec] 바디 JSON 파싱 실패, 빈 payload 반환: reqIdCode={}, error={}",
                    reqIdCode, e.getMessage());
            return Map.of();
        }
    }
}

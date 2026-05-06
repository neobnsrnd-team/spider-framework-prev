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
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 실제 뱅킹 프로토콜 코덱 — 헤더 내 ASCII 길이 필드 방식.
 *
 * <p>한국 금융/뱅킹 시스템(ATM, 코어뱅킹, 인터넷뱅킹 서버 등)의 실제 전문 포맷을 처리한다.
 * 참고소스 {@code OrgMessageReader.readMessage()} 방식을 Spring Boot 스타일로 재구현.</p>
 *
 * <p>수신 전문 구조:</p>
 * <pre>
 * [헤더 고정길이 bytes                    ] [바디 bytes]
 *  ↑ 헤더 내 특정 offset에 ASCII 숫자 문자열로 바디(또는 전체) 길이 기술
 *  예) headerLength=64, lengthFieldOffset=0, lengthFieldLength=8
 *      → header[0..7] = "00000256" 이면 바디 256byte
 * </pre>
 *
 * <p>{@link HeaderBasedMessageCodec}과의 차이:</p>
 * <ul>
 *   <li>HeaderBasedMessageCodec: 4byte 바이너리 int 프리픽스 (POC 내부 프로토콜)</li>
 *   <li>BankingHeaderMessageCodec: 헤더 내 ASCII 문자열 길이 필드 (실제 뱅킹 프로토콜)</li>
 * </ul>
 *
 * <p>{@code GatewayLoader}는 {@code GW_PROPERTIES}에 {@code header-length}가 존재하면
 * 이 코덱을 선택한다. 기존 POC 데모 환경은 영향 없음.</p>
 *
 * <p>응답은 {@link JsonMessageCodec}과 동일하게 4byte length + JSON으로 직렬화한다.</p>
 *
 * <pre>{@code
 * // GW_PROPERTIES 설정 예시
 * port=19100;header-length=56;length-field-offset=0;length-field-length=8;
 *   is-total-length=false;header-msg-id=DEMO_GW_HEADER;org-id=DEMO
 * }</pre>
 */
@Slf4j
@RequiredArgsConstructor
public class BankingHeaderMessageCodec implements MessageCodec<JsonCommandRequest, JsonCommandResponse> {

    /** 바디 최대 허용 크기 (1 MB) */
    private static final int MAX_BODY_LEN = 1024 * 1024;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final HeaderOffsetParser headerOffsetParser;
    /** FWK_MESSAGE.ORG_ID */
    private final String orgId;
    /** FWK_MESSAGE.MESSAGE_ID (HEADER_YN='Y') — 헤더 필드 오프셋 정의 */
    private final String headerMessageId;
    /** REQ_ID_CODE → FWK_MESSAGE 조회 후 바디 타입 결정 */
    private final MessageStructurePool messageStructurePool;
    /** MESSAGE_TYPE='F' 고정길이 바디 파싱 */
    private final FixedLengthParser fixedLengthParser;
    /** 헤더 고정 길이 (byte) — GW_PROPERTIES.header-length */
    private final int headerLength;
    /** 헤더 내 길이 필드 시작 위치 — GW_PROPERTIES.length-field-offset */
    private final int lengthFieldOffset;
    /** 헤더 내 길이 필드 크기 (ASCII 문자 수) — GW_PROPERTIES.length-field-length */
    private final int lengthFieldLength;
    /**
     * 길이 필드가 전체(헤더+바디) 길이인지 여부 — GW_PROPERTIES.is-total-length.
     * true면 바디 길이 = 길이 필드 값 - headerLength.
     */
    private final boolean isTotalLength;

    /**
     * 수신 바이트 스트림을 디코딩하여 {@link JsonCommandRequest}로 변환한다.
     *
     * <ol>
     *   <li>헤더 고정길이만큼 읽기</li>
     *   <li>헤더 내 ASCII 숫자 문자열 → 바디 길이 계산 (참고소스 OrgMessageReader 방식)</li>
     *   <li>바디 읽기</li>
     *   <li>HeaderOffsetParser로 REQ_ID_CODE, REQUEST_ID 추출</li>
     *   <li>REQ_ID_CODE → FWK_MESSAGE.MESSAGE_TYPE 조회 → 바디 파싱 전략 결정</li>
     * </ol>
     */
    @Override
    public JsonCommandRequest decode(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);

        // 1. 헤더 고정길이만큼 먼저 읽기
        byte[] headerBuf = new byte[headerLength];
        dis.readFully(headerBuf);

        // 2. 헤더 내 ASCII 숫자 문자열로 길이 파싱 (참고소스 OrgMessageReader.readMessage() 동일)
        String lengthStr = new String(headerBuf, lengthFieldOffset, lengthFieldLength).trim();
        int bodyLength;
        try {
            bodyLength = Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            throw new IOException("길이 필드 파싱 실패 — 숫자가 아님: '" + lengthStr + "'");
        }

        // is-total-length=true면 전체 길이에서 헤더 길이 차감
        if (isTotalLength) {
            bodyLength -= headerLength;
        }

        if (bodyLength < 0 || bodyLength > MAX_BODY_LEN) {
            throw new IOException("바디 길이가 허용 범위를 초과합니다: " + bodyLength);
        }

        // 3. 바디 읽기
        byte[] bodyBuf = new byte[bodyLength];
        if (bodyLength > 0) {
            dis.readFully(bodyBuf);
        }

        // 4. 헤더에서 REQ_ID_CODE, REQUEST_ID 추출 (헤더 부분만 파싱)
        String reqIdCode = headerOffsetParser.extractReqIdCode(orgId, headerMessageId, headerBuf);
        if (reqIdCode == null || reqIdCode.isBlank()) {
            throw new IOException("헤더에서 REQ_ID_CODE 추출 실패: orgId=" + orgId
                    + ", headerMsgId=" + headerMessageId);
        }
        String requestId = headerOffsetParser.extractField(orgId, headerMessageId, headerBuf, "REQUEST_ID");

        // 5. REQ_ID_CODE → FWK_MESSAGE.MESSAGE_TYPE → 바디 파싱 전략 결정
        Map<String, Object> payload = parseBody(bodyBuf, reqIdCode.strip());

        log.debug("[BankingHeaderMessageCodec] 수신: reqIdCode={}, requestId={}, bodyLength={}",
                reqIdCode, requestId, bodyLength);

        return JsonCommandRequest.builder()
                .command(reqIdCode.strip())
                .requestId(requestId != null ? requestId.strip() : null)
                .payload(payload)
                .build();
    }

    /**
     * 응답을 4byte length + JSON으로 직렬화한다.
     * {@link JsonMessageCodec}과 동일한 포맷.
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
     * 바디 byte[]를 파싱한다.
     *
     * <p>REQ_ID_CODE로 FWK_MESSAGE를 조회하여 MESSAGE_TYPE을 확인한다.
     * MESSAGE_TYPE='F'(고정길이)이면 {@link FixedLengthParser}로, 그 외이면 JSON으로 파싱한다.</p>
     */
    private Map<String, Object> parseBody(byte[] bodyBuf, String reqIdCode) {
        if (bodyBuf.length == 0) {
            return Map.of();
        }

        return messageStructurePool.get(orgId, reqIdCode)
                .filter(structure -> "F".equals(structure.getMessageType()))
                .map(structure -> {
                    log.debug("[BankingHeaderMessageCodec] 바디 고정길이 파싱: reqIdCode={}", reqIdCode);
                    return fixedLengthParser.parse(structure, bodyBuf);
                })
                .orElseGet(() -> parseJson(reqIdCode, bodyBuf));
    }

    /** JSON 바디 파싱 — 파싱 실패 시 빈 Map 반환 */
    private Map<String, Object> parseJson(String reqIdCode, byte[] bodyBuf) {
        try {
            return objectMapper.readValue(bodyBuf, MAP_TYPE);
        } catch (IOException e) {
            log.warn("[BankingHeaderMessageCodec] 바디 JSON 파싱 실패, 빈 payload 반환: reqIdCode={}, error={}",
                    reqIdCode, e.getMessage());
            return Map.of();
        }
    }
}

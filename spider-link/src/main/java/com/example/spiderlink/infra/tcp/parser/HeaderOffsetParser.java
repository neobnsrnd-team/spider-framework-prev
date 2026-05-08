package com.example.spiderlink.infra.tcp.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 고정길이 헤더 byte[] 에서 특정 필드 값을 오프셋 기반으로 추출하는 파서.
 *
 * <p>FWK_MESSAGE_FIELD(HEADER_YN='Y') 의 SORT_ORDER + DATA_LENGTH 누적합으로
 * 바이트 오프셋을 계산하여 필드를 추출한다. JSON/고정길이/XML 등 바디 포맷에 무관하게
 * 헤더 포맷이 고정이므로 모든 프로토콜을 동일하게 처리할 수 있다.</p>
 *
 * <p>참고소스 DefaultHeaderParser 의 msgIdOffset/msgIdLength 접근 방식을
 * FWK_MESSAGE_FIELD 메타 기반으로 재구현한 것이다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeaderOffsetParser {

    /** REQ_ID_CODE 추출에 사용하는 FWK_MESSAGE_FIELD.MESSAGE_FIELD_ID 기본값 */
    public static final String DEFAULT_REQ_ID_CODE_FIELD = "REQ_ID_CODE";

    private final MessageStructurePool messageStructurePool;

    /**
     * 수신 byte[] 헤더에서 REQ_ID_CODE 값을 추출한다.
     *
     * @param orgId           기관 ID (FWK_MESSAGE 조회 키)
     * @param headerMessageId 헤더 전문 ID (FWK_MESSAGE.HEADER_YN='Y' 인 전문)
     * @param message         수신된 전체 byte[] (4byte 길이 프리픽스 제외, 헤더+바디 원본)
     * @return REQ_ID_CODE 문자열, 구조 미등록이거나 필드 없으면 null
     */
    public String extractReqIdCode(String orgId, String headerMessageId, byte[] message) {
        return extractField(orgId, headerMessageId, message, DEFAULT_REQ_ID_CODE_FIELD);
    }

    /**
     * 헤더 byte[] 에서 지정된 필드(MESSAGE_FIELD_ID) 값을 오프셋 기반으로 추출한다.
     *
     * <p>SORT_ORDER 오름차순으로 DATA_LENGTH 를 누적하여 대상 필드의 시작 오프셋을 계산한다.
     * LoopField(반복 구조)는 헤더에 포함되지 않으므로 건너뛴다.</p>
     *
     * @param orgId           기관 ID
     * @param headerMessageId 헤더 전문 ID
     * @param message         수신 byte[]
     * @param fieldId         추출할 필드의 MESSAGE_FIELD_ID
     * @return 추출된 문자열 (trailing 공백 제거), 필드 없거나 범위 초과 시 null
     */
    public String extractField(String orgId, String headerMessageId, byte[] message, String fieldId) {
        var structureOpt = messageStructurePool.get(orgId, headerMessageId);
        if (structureOpt.isEmpty()) {
            log.warn("[HeaderOffsetParser] 헤더 전문 구조 미등록: orgId={}, messageId={}", orgId, headerMessageId);
            return null;
        }

        int offset = 0;
        for (MessageField field : structureOpt.get().getFields()) {
            // 헤더에는 반복 구조 없음 — LoopField 건너뜀
            if (field instanceof LoopField) {
                continue;
            }
            if (field.getName().equalsIgnoreCase(fieldId)) {
                int len = Math.min(field.getLength(), message.length - offset);
                if (len <= 0) {
                    log.warn("[HeaderOffsetParser] 필드 범위 초과: field={}, offset={}, messageLen={}",
                            fieldId, offset, message.length);
                    return null;
                }
                return new String(message, offset, len).stripTrailing();
            }
            offset += field.getLength();
        }

        log.warn("[HeaderOffsetParser] 필드 미발견: orgId={}, messageId={}, fieldId={}", orgId, headerMessageId, fieldId);
        return null;
    }

    /**
     * 헤더의 총 바이트 길이를 계산한다.
     *
     * <p>바디 파싱 시작 오프셋을 구할 때 사용한다.
     * {@code message[calcHeaderLength()] ~} 구간이 바디부다.</p>
     *
     * @param orgId           기관 ID
     * @param headerMessageId 헤더 전문 ID
     * @return 헤더 총 바이트 수, 구조 미등록이면 0
     */
    public int calcHeaderLength(String orgId, String headerMessageId) {
        return messageStructurePool.get(orgId, headerMessageId)
                .map(MessageStructure::getStaticTotalLength)
                .orElse(0);
    }
}

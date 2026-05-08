package com.example.spiderlink.infra.tcp.parser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JSON 전문 검증·마스킹 처리기.
 *
 * <p>수신 JSON payload를 FWK_MESSAGE_FIELD에 등록된 필드 구조와 대조하여
 * 필수 필드를 검증하고, 민감 필드(REMARK 설정)를 마스킹한 로그용 문자열을 생성한다.</p>
 *
 * <p>고정길이 전문과 달리 JSON은 Jackson이 파싱을 담당하므로 이 클래스는 파싱이 아닌
 * 검증·마스킹 역할만 수행한다. FWK_MESSAGE_FIELD에 미등록된 전문은
 * 검증 없이 raw payload 문자열을 그대로 반환한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonPayloadValidator {

    /** MessageStructureCache 조회 시 사용할 기관 ID */
    private static final String ORG_ID = "DEMO";

    private final MessageStructureCache messageStructureCache;

    /**
     * JSON payload를 FWK_MESSAGE_FIELD의 REQUIRED_YN 기준으로 필수 필드를 검증한다.
     *
     * <p>REQUIRED_YN='Y'인 필드가 payload에 없거나 빈 값이면
     * {@link IllegalArgumentException}을 던진다.
     * FWK_MESSAGE_FIELD에 미등록된 전문은 검증 없이 통과한다.</p>
     *
     * @param messageId 전문 ID (예: DEMO_AUTH_LOGIN_REQ)
     * @param payload   수신된 JSON payload
     * @throws IllegalArgumentException 필수 필드 누락 시
     */
    public void validate(String messageId, Map<String, Object> payload) {
        Optional<MessageStructure> optStructure = messageStructureCache.get(ORG_ID, messageId);
        if (optStructure.isEmpty()) {
            log.debug("[JsonPayloadValidator] FWK_MESSAGE_FIELD 미등록 전문: messageId={}, 검증 생략", messageId);
            return;
        }

        MessageStructure structure = optStructure.get();
        for (MessageField field : structure.getFields()) {
            if (!field.isRequired()) {
                continue;
            }
            Object value = payload != null ? payload.get(field.getName()) : null;
            if (value == null || String.valueOf(value).isBlank()) {
                throw new IllegalArgumentException("필수 필드 누락: " + field.getName());
            }
        }
    }

    /**
     * JSON payload를 FWK_MESSAGE_FIELD 기준으로 처리하여 로그 안전 문자열을 반환한다.
     *
     * <ul>
     *   <li>REMARK 설정 필드(예: password): 원본 값 길이만큼 REMARK 문자로 대체</li>
     *   <li>LOG_YN='N' 필드: 로그에서 완전히 제외</li>
     *   <li>FWK_MESSAGE_FIELD 미등록 전문: payload.toString() 그대로 반환</li>
     * </ul>
     *
     * @param messageId 전문 ID (예: DEMO_AUTH_LOGIN_REQ)
     * @param payload   수신된 JSON payload
     * @return 로그 출력용 마스킹 처리된 문자열
     */
    public String maskForLog(String messageId, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }

        Optional<MessageStructure> optStructure = messageStructureCache.get(ORG_ID, messageId);
        if (optStructure.isEmpty()) {
            log.debug("[JsonPayloadValidator] FWK_MESSAGE_FIELD 미등록 전문: messageId={}, 마스킹 생략", messageId);
            return payload.toString();
        }

        MessageStructure structure = optStructure.get();
        Map<String, Object> safeMap = new LinkedHashMap<>();

        for (MessageField field : structure.getFields()) {
            // LOG_YN=N: 로그에서 완전히 제외
            if (!field.isLogMode()) {
                continue;
            }

            Object value = payload.get(field.getName());
            if (value == null) {
                continue;
            }

            // REMARK 설정 필드: 원본 길이만큼 마스킹 문자로 대체 (예: password → ***)
            if (field.getRemark() != null && !field.getRemark().isEmpty()) {
                safeMap.put(field.getName(), field.getRemark().repeat(String.valueOf(value).length()));
            } else {
                safeMap.put(field.getName(), value);
            }
        }

        return safeMap.toString();
    }
}

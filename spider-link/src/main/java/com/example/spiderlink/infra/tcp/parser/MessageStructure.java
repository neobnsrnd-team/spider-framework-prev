package com.example.spiderlink.infra.tcp.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 전문 구조 모델 — FWK_MESSAGE + FWK_MESSAGE_FIELD 를 파서가 사용하는 형태로 조합한 객체.
 *
 * <p>MessageStructureCache가 DB에서 로드 후 캐시하며,
 * FixedLengthParser가 파싱 시 참조한다.</p>
 */
public class MessageStructure {

    private final String orgId;
    private final String messageId;

    /**
     * 전문 타입 코드 (FWK_MESSAGE.MESSAGE_TYPE).
     * F=고정길이, J=JSON, X=XML, I=ISO8583, D=구분자, C=CSV
     */
    private final String messageType;

    /** 최상위 필드 목록 (LoopField 포함) */
    private final List<MessageField> fields = new ArrayList<>();

    public MessageStructure(String orgId, String messageId, String messageType) {
        this.orgId       = orgId;
        this.messageId   = messageId;
        this.messageType = messageType;
    }

    public void addField(MessageField field) {
        fields.add(field);
    }

    public String getOrgId() {
        return orgId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public List<MessageField> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * 단순 필드의 총 바이트 길이.
     * LoopField가 포함된 전문은 반복 횟수에 따라 실제 길이가 달라지므로 참고용으로만 사용.
     */
    public int getStaticTotalLength() {
        return fields.stream().mapToInt(MessageField::getLength).sum();
    }
}

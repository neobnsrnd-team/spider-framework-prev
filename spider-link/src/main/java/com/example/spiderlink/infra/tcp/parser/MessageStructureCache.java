package com.example.spiderlink.infra.tcp.parser;

import com.example.spiderlink.domain.message.dto.MessageFieldMeta;
import com.example.spiderlink.domain.message.mapper.MessageMetaMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 전문 구조(MessageStructure) 메모리 캐시.
 *
 * <p>최초 요청 시 DB(FWK_MESSAGE + FWK_MESSAGE_FIELD)에서 로드하고,
 * 이후에는 메모리 캐시에서 반환한다.</p>
 *
 * <p>FWK_MESSAGE_FIELD 변경 후 반영하려면 {@link #evict} 또는 {@link #clear}를 호출한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageStructureCache {

    private static final String BEGIN_LOOP_PREFIX = "_BeginLoop_";
    private static final String END_LOOP          = "_EndLoop_";

    private final MessageMetaMapper messageMetaMapper;

    /** 캐시 키: "orgId:messageId" */
    private final ConcurrentHashMap<String, MessageStructure> cache = new ConcurrentHashMap<>();

    /**
     * DB 조회 후 등록되지 않은 것으로 확인된 키 집합.
     * 반복 DB 조회를 방지한다.
     */
    private final Set<String> notFoundKeys = ConcurrentHashMap.newKeySet();

    /**
     * 전문 구조를 반환한다. 캐시 미스 시 DB에서 로드한다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return 전문 구조, FWK_MESSAGE 에 등록되지 않았으면 empty
     */
    public Optional<MessageStructure> get(String orgId, String messageId) {
        String key = cacheKey(orgId, messageId);

        // 이미 없다고 확인된 키는 DB 재조회 생략
        if (notFoundKeys.contains(key)) {
            return Optional.empty();
        }

        MessageStructure cached = cache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }

        // DB 로드 후 캐시 등록
        MessageStructure loaded = load(orgId, messageId);
        if (loaded == null) {
            notFoundKeys.add(key);
            return Optional.empty();
        }

        cache.put(key, loaded);
        return Optional.of(loaded);
    }

    /**
     * 특정 전문의 캐시를 무효화한다.
     * FWK_MESSAGE_FIELD 변경 후 재로드할 때 호출한다.
     */
    public void evict(String orgId, String messageId) {
        String key = cacheKey(orgId, messageId);
        cache.remove(key);
        notFoundKeys.remove(key);
        log.info("[MessageStructureCache] 캐시 제거: orgId={}, messageId={}", orgId, messageId);
    }

    /** 전체 캐시 초기화 */
    public void clear() {
        cache.clear();
        notFoundKeys.clear();
        log.info("[MessageStructureCache] 전체 캐시 초기화");
    }

    /** DB에서 전문 구조를 로드하여 빌드 */
    private MessageStructure load(String orgId, String messageId) {
        String messageType = messageMetaMapper.selectMessageType(orgId, messageId);
        if (messageType == null) {
            log.warn("[MessageStructureCache] FWK_MESSAGE 미등록 전문: orgId={}, messageId={}", orgId, messageId);
            return null;
        }

        List<MessageFieldMeta> fieldMetas = messageMetaMapper.selectFields(orgId, messageId);
        MessageStructure structure = buildStructure(orgId, messageId, messageType, fieldMetas);
        log.info("[MessageStructureCache] 전문 구조 로드: orgId={}, messageId={}, type={}, fields={}",
                orgId, messageId, messageType, fieldMetas.size());
        return structure;
    }

    /**
     * 평탄 필드 목록을 트리 구조로 변환.
     *
     * <p>_BeginLoop_xxx ~ _EndLoop_ 구간을 LoopField 로 묶는다.
     * 단일 뎁스 루프만 지원한다.</p>
     */
    private MessageStructure buildStructure(
            String orgId, String messageId, String messageType, List<MessageFieldMeta> metas) {

        MessageStructure structure = new MessageStructure(orgId, messageId, messageType);
        LoopField currentLoop = null;

        for (MessageFieldMeta meta : metas) {
            String fieldId = meta.getMessageFieldId();

            if (fieldId.length() > BEGIN_LOOP_PREFIX.length()
                    && fieldId.substring(0, BEGIN_LOOP_PREFIX.length())
                              .equalsIgnoreCase(BEGIN_LOOP_PREFIX)) {

                String loopName = fieldId.substring(BEGIN_LOOP_PREFIX.length());
                currentLoop = new LoopField(
                        loopName,
                        meta.getDataLength() != null ? meta.getDataLength().intValue() : 0,
                        meta.getFieldRepeatCnt() != null ? meta.getFieldRepeatCnt() : 0,
                        meta.getDefaultValue()
                );
                structure.addField(currentLoop);

            } else if (END_LOOP.equalsIgnoreCase(fieldId)) {
                currentLoop = null;

            } else {
                MessageField field = toMessageField(meta);
                if (currentLoop != null) {
                    currentLoop.addChild(field);
                } else {
                    structure.addField(field);
                }
            }
        }

        return structure;
    }

    private MessageField toMessageField(MessageFieldMeta meta) {
        return new MessageField(
                meta.getMessageFieldId(),
                meta.getDataType() != null ? meta.getDataType() : MessageField.CHR,
                meta.getDataLength() != null ? meta.getDataLength().intValue() : 0,
                meta.getScale() != null ? meta.getScale() : 0,
                meta.getAlign() != null ? meta.getAlign() : MessageField.LEFT,
                resolveFillerChar(meta.getFiller()),
                meta.getRemark(),
                "Y".equalsIgnoreCase(meta.getLogYn()),
                "Y".equalsIgnoreCase(meta.getRequiredYn())
        );
    }

    private char resolveFillerChar(String filler) {
        if (filler == null || filler.isEmpty()) return ' ';
        return filler.charAt(0);
    }

    private String cacheKey(String orgId, String messageId) {
        return orgId + ":" + messageId;
    }
}

package com.example.spideradmin.domain.messageparsing.service;

import com.example.spideradmin.domain.message.dto.MessageParseResponse;
import com.example.spideradmin.domain.message.dto.MessageSearchResponse;
import com.example.spideradmin.domain.message.mapper.MessageMapper;
import com.example.spideradmin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spideradmin.domain.messageparsing.dto.MessageParseRequest;
import com.example.spideradmin.domain.messageparsing.dto.MessageResponse;
import com.example.spideradmin.domain.messageparsing.dto.ParsedFieldResponse;
import com.example.spideradmin.domain.messageparsing.util.MessageParser;
import com.example.spideradmin.domain.org.dto.OrgResponse;
import com.example.spideradmin.domain.org.mapper.OrgMapper;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageParsingService {

    private final MessageMapper messageMapper;
    private final OrgMapper orgMapper;

    public List<OrgResponse> getOrgList() {
        return orgMapper.findAll();
    }

    public MessageParseResponse parseMessage(MessageParseRequest request) {
        try {
            MessageResponse message = messageMapper.selectResponseById(request.getOrgId(), request.getMessageId());
            if (message == null) {
                throw new NotFoundException("messageId: " + request.getOrgId() + "/" + request.getMessageId());
            }

            List<MessageFieldResponse> allFields =
                    collectAllFields(request.getOrgId(), request.getMessageId(), message);

            if (allFields.isEmpty()) {
                throw new InternalException(String.format(
                        "orgId: %s, messageId: %s - %s",
                        request.getOrgId(), request.getMessageId(), "전문에 필드가 정의되어 있지 않습니다"));
            }

            List<ParsedFieldResponse> parsedFields = MessageParser.parseMessage(request.getRawString(), allFields);

            return MessageParseResponse.builder()
                    .orgId(request.getOrgId())
                    .messageId(request.getMessageId())
                    .messageName(message.getMessageName())
                    .messageDesc(message.getMessageDesc())
                    .rawString(request.getRawString())
                    .totalLength(MessageParser.calculateByteLength(request.getRawString()))
                    .fields(parsedFields)
                    .build();

        } catch (NotFoundException | InternalException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalException(String.format(
                    "orgId: %s, messageId: %s - %s",
                    request.getOrgId(), request.getMessageId(), "파싱 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    private List<MessageFieldResponse> collectAllFields(String orgId, String messageId, MessageResponse message) {
        List<MessageFieldResponse> allFields = new ArrayList<>();

        if (message.getParentMessageId() != null
                && !message.getParentMessageId().isEmpty()) {
            allFields.addAll(traverseHeaderChain(orgId, message.getParentMessageId()));
        }

        List<MessageFieldResponse> bodyFields = messageMapper.findFieldsByMessageId(orgId, messageId);
        if (bodyFields != null) {
            allFields.addAll(bodyFields);
        }

        return allFields;
    }

    private List<MessageFieldResponse> traverseHeaderChain(String orgId, String parentMessageId) {
        List<String> headerChain = new ArrayList<>();
        Set<String> visitedParentIds = new HashSet<>();
        String currentParentId = parentMessageId;

        while (currentParentId != null && !currentParentId.isEmpty()) {
            if (!visitedParentIds.add(currentParentId)) {
                log.warn("헤더 체인에서 순환 참조 감지, 중단: messageId={}", currentParentId);
                break;
            }
            headerChain.add(currentParentId);
            MessageResponse parentMsg = messageMapper.selectResponseById(orgId, currentParentId);
            if (parentMsg == null) {
                break;
            }
            currentParentId = parentMsg.getParentMessageId();
        }

        Collections.reverse(headerChain);

        return headerChain.stream()
                .map(headerId -> messageMapper.findFieldsByMessageId(orgId, headerId))
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

    /**
     * 고정 길이 전문을 파싱한 후 JSON 구조로 변환합니다.
     * targetOrgId/targetMessageId가 지정된 경우, 대상 전문 필드 정의에 맞춰 변환합니다.
     *
     * @param request 파싱 요청 (orgId, messageId, rawString, targetOrgId, targetMessageId)
     * @return 필드명-값 매핑이 포함된 JSON 구조
     */
    public Map<String, Object> convertToJson(MessageParseRequest request) {
        MessageParseResponse parsed = parseMessage(request);

        // 지정 전문 JSON 생성: target이 지정되고 source와 다른 경우
        boolean hasTarget = request.getTargetOrgId() != null
                && !request.getTargetOrgId().isEmpty()
                && request.getTargetMessageId() != null
                && !request.getTargetMessageId().isEmpty();
        boolean isDifferentTarget = hasTarget
                && (!request.getTargetOrgId().equals(request.getOrgId())
                        || !request.getTargetMessageId().equals(request.getMessageId()));

        if (isDifferentTarget) {
            return buildTargetFormatJson(request, parsed);
        }

        // 동일 포멧 JSON 생성
        Map<String, Object> jsonResult = new LinkedHashMap<>();
        jsonResult.put("orgId", request.getOrgId());
        jsonResult.put("messageId", request.getMessageId());
        jsonResult.put("messageName", parsed.getMessageName());

        Map<String, Object> fields = new LinkedHashMap<>();
        for (ParsedFieldResponse field : parsed.getFields()) {
            fields.put(field.getFieldId(), field.getRawValue());
        }
        jsonResult.put("fields", fields);
        jsonResult.put("totalLength", parsed.getTotalLength());

        return jsonResult;
    }

    /**
     * 대상 전문 필드 정의에 맞춰 JSON을 생성합니다.
     * 원본 파싱 결과의 필드값을 순서(position)로 대상 필드에 매핑합니다.
     */
    private Map<String, Object> buildTargetFormatJson(MessageParseRequest request, MessageParseResponse parsed) {
        MessageResponse targetMsg =
                messageMapper.selectResponseById(request.getTargetOrgId(), request.getTargetMessageId());
        if (targetMsg == null) {
            throw new NotFoundException(
                    "targetMessageId: " + request.getTargetOrgId() + "/" + request.getTargetMessageId());
        }

        List<MessageFieldResponse> targetFields =
                collectAllFields(request.getTargetOrgId(), request.getTargetMessageId(), targetMsg);

        Map<String, Object> jsonResult = new LinkedHashMap<>();
        jsonResult.put("orgId", request.getTargetOrgId());
        jsonResult.put("messageId", request.getTargetMessageId());
        jsonResult.put("messageName", targetMsg.getMessageName());

        Map<String, Object> fields = new LinkedHashMap<>();
        List<ParsedFieldResponse> parsedFields = parsed.getFields();

        for (int i = 0; i < targetFields.size(); i++) {
            MessageFieldResponse targetField = targetFields.get(i);
            String value = i < parsedFields.size() ? parsedFields.get(i).getRawValue() : "";
            fields.put(targetField.getMessageFieldId(), value);
        }
        jsonResult.put("fields", fields);
        jsonResult.put("totalLength", parsed.getTotalLength());

        return jsonResult;
    }

    public List<String> getOrgIdList() {
        return messageMapper.findDistinctOrgIds();
    }

    public List<MessageSearchResponse> searchMessages(String orgId, String searchField, String keyword) {
        String effectiveField = null;
        String searchValue = null;

        if (keyword != null && !keyword.isEmpty() && searchField != null) {
            effectiveField = searchField;
            searchValue = keyword;
        }

        List<MessageResponse> messages =
                messageMapper.findAllBySearch(effectiveField, searchValue, orgId, null, null, null, null, null);

        return messages.stream()
                .map(msg -> MessageSearchResponse.builder()
                        .orgId(msg.getOrgId())
                        .messageId(msg.getMessageId())
                        .messageName(msg.getMessageName())
                        .messageDesc(msg.getMessageDesc())
                        .build())
                .toList();
    }
}

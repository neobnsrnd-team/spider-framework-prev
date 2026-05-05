package com.example.spideradmin.domain.messagefield.service;

import com.example.spideradmin.domain.messagefield.dto.FieldBatchCreateRequest;
import com.example.spideradmin.domain.messagefield.dto.FieldCreateRequest;
import com.example.spideradmin.domain.messagefield.dto.FieldListResponse;
import com.example.spideradmin.domain.messagefield.dto.FieldUpdateRequest;
import com.example.spideradmin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spideradmin.domain.messagefield.mapper.FieldMapper;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for MessageField management
 * Handles business logic for MessageField operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FieldService {

    private final FieldMapper fieldMapper;

    public List<FieldListResponse> getFieldsByMessageId(String orgId, String messageId) {
        log.info("Finding fields by orgId: {}, messageId: {}", orgId, messageId);

        return fieldMapper.findByMessageId(orgId, messageId).stream()
                .map(f -> FieldListResponse.builder()
                        .sortOrder(f.getSortOrder())
                        .messageFieldName(f.getMessageFieldName())
                        .messageFieldId(f.getMessageFieldId())
                        .dataType(f.getDataType())
                        .dataLength(f.getDataLength())
                        .align(f.getAlign())
                        .requiredYn(f.getRequiredYn())
                        .scale(f.getScale())
                        .filler(f.getFiller())
                        .useMode(f.getUseMode())
                        .codeGroup(f.getCodeGroup())
                        .codeMappingYn(f.getCodeMappingYn())
                        .defaultValue(f.getDefaultValue())
                        .remark(f.getRemark())
                        .logYn(f.getLogYn())
                        .validationRuleId(f.getValidationRuleId())
                        .fieldTag(f.getFieldTag())
                        .build())
                .toList();
    }

    @Transactional
    public MessageFieldResponse createField(FieldCreateRequest requestDTO) {
        log.info(
                "Creating MessageField: orgId={}, messageId={}, messageFieldId={}",
                requestDTO.getOrgId(),
                requestDTO.getMessageId(),
                requestDTO.getMessageFieldId());

        // 중복 체크
        if (fieldMapper.countByFieldId(requestDTO.getOrgId(), requestDTO.getMessageId(), requestDTO.getMessageFieldId())
                > 0) {
            throw new DuplicateException("messageFieldId: " + requestDTO.getMessageFieldId());
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        // 저장
        fieldMapper.insertField(requestDTO, now, currentUserId);
        log.info(
                "MessageField created successfully: orgId={}, messageId={}, messageFieldId={}",
                requestDTO.getOrgId(),
                requestDTO.getMessageId(),
                requestDTO.getMessageFieldId());

        return fieldMapper.selectResponseById(
                requestDTO.getOrgId(), requestDTO.getMessageId(), requestDTO.getMessageFieldId());
    }

    @Transactional
    public MessageFieldResponse updateField(
            String orgId, String messageId, String messageFieldId, FieldUpdateRequest requestDTO) {
        log.info("Updating MessageField: orgId={}, messageId={}, messageFieldId={}", orgId, messageId, messageFieldId);

        // 존재 확인
        if (fieldMapper.countByFieldId(orgId, messageId, messageFieldId) == 0) {
            throw new NotFoundException("messageFieldId: " + messageFieldId);
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        // 저장
        fieldMapper.updateField(orgId, messageId, messageFieldId, requestDTO, now, currentUserId);
        log.info(
                "MessageField updated successfully: orgId={}, messageId={}, messageFieldId={}",
                orgId,
                messageId,
                messageFieldId);

        return fieldMapper.selectResponseById(orgId, messageId, messageFieldId);
    }

    @Transactional
    public void deleteField(String orgId, String messageId, String messageFieldId) {
        log.info("Deleting MessageField: orgId={}, messageId={}, messageFieldId={}", orgId, messageId, messageFieldId);

        if (fieldMapper.countByFieldId(orgId, messageId, messageFieldId) == 0) {
            throw new NotFoundException("messageFieldId: " + messageFieldId);
        }

        fieldMapper.deleteField(orgId, messageId, messageFieldId);
        log.info(
                "MessageField deleted successfully: orgId={}, messageId={}, messageFieldId={}",
                orgId,
                messageId,
                messageFieldId);
    }

    @Transactional
    public List<MessageFieldResponse> createFieldBatch(FieldBatchCreateRequest requestDTO) {
        log.info("Creating MessageField batch: count={}", requestDTO.getFields().size());

        // 중복 체크
        for (FieldCreateRequest fieldDTO : requestDTO.getFields()) {
            if (fieldMapper.countByFieldId(fieldDTO.getOrgId(), fieldDTO.getMessageId(), fieldDTO.getMessageFieldId())
                    > 0) {
                throw new DuplicateException("messageFieldId: " + fieldDTO.getMessageFieldId());
            }
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        // 일괄 저장
        fieldMapper.insertFieldBatch(requestDTO.getFields(), now, currentUserId);
        log.info(
                "MessageField batch created successfully: count={}",
                requestDTO.getFields().size());

        // 저장된 결과 조회하여 반환
        return requestDTO.getFields().stream()
                .map(dto -> fieldMapper.selectResponseById(dto.getOrgId(), dto.getMessageId(), dto.getMessageFieldId()))
                .toList();
    }
}

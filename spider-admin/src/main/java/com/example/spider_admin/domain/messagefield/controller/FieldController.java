package com.example.spider_admin.domain.messagefield.controller;

import com.example.spider_admin.domain.messagefield.dto.FieldBatchCreateRequest;
import com.example.spider_admin.domain.messagefield.dto.FieldCreateRequest;
import com.example.spider_admin.domain.messagefield.dto.FieldUpdateRequest;
import com.example.spider_admin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spider_admin.domain.messagefield.service.FieldService;
import com.example.spider_admin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * MessageField 리소스에 대한 CRUD 인터페이스를 제공합니다.
 * 모든 응답은 {@link ApiResponse} 규격으로 통일하여 반환합니다.
 *
 * @see FieldService
 */
@Slf4j
@RestController
@RequestMapping("/api/messages/{messageId}/fields")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MESSAGE:R')")
public class FieldController {

    private final FieldService fieldService;

    /**
     * 새로운 필드를 생성합니다.
     *
     * @param messageId  전문 ID (경로 파라미터)
     * @param requestDTO 생성할 필드 정보
     * @return 생성된 필드 정보 (201 Created)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MESSAGE:W')")
    public ResponseEntity<ApiResponse<MessageFieldResponse>> createField(
            @PathVariable String messageId, @Valid @RequestBody FieldCreateRequest requestDTO) {

        log.info(
                "POST /api/messages/{}/fields - Creating field: orgId={}, messageFieldId={}",
                messageId,
                requestDTO.getOrgId(),
                requestDTO.getMessageFieldId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(fieldService.createField(requestDTO)));
    }

    /**
     * 기존 필드의 정보를 수정합니다.
     *
     * @param orgId          기관 ID
     * @param messageId      전문 ID
     * @param messageFieldId 필드 ID
     * @param requestDTO     수정할 필드 정보
     * @return 수정된 필드 정보
     */
    @PutMapping("/{messageFieldId}")
    @PreAuthorize("hasAuthority('MESSAGE:W')")
    public ResponseEntity<ApiResponse<MessageFieldResponse>> updateField(
            @RequestParam String orgId,
            @PathVariable String messageId,
            @PathVariable String messageFieldId,
            @Valid @RequestBody FieldUpdateRequest requestDTO) {

        log.info("PUT /api/messages/{}/fields/{} - Updating field: orgId={}", messageId, messageFieldId, orgId);
        return ResponseEntity.ok(
                ApiResponse.success(fieldService.updateField(orgId, messageId, messageFieldId, requestDTO)));
    }

    /**
     * 필드를 삭제합니다.
     *
     * @param orgId          기관 ID
     * @param messageId      전문 ID
     * @param messageFieldId 필드 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{messageFieldId}")
    @PreAuthorize("hasAuthority('MESSAGE:W')")
    public ResponseEntity<ApiResponse<Void>> deleteField(
            @RequestParam String orgId, @PathVariable String messageId, @PathVariable String messageFieldId) {

        log.info("DELETE /api/messages/{}/fields/{} - Deleting field: orgId={}", messageId, messageFieldId, orgId);
        fieldService.deleteField(orgId, messageId, messageFieldId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 여러 필드를 일괄 생성합니다.
     *
     * @param messageId  전문 ID (경로 파라미터)
     * @param requestDTO 생성할 필드 목록
     * @return 생성된 필드 목록 (201 Created)
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('MESSAGE:W')")
    public ResponseEntity<ApiResponse<List<MessageFieldResponse>>> createFieldBatch(
            @PathVariable String messageId, @Valid @RequestBody FieldBatchCreateRequest requestDTO) {

        log.info(
                "POST /api/messages/{}/fields/batch - Creating batch fields: count={}",
                messageId,
                requestDTO.getFields().size());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(fieldService.createFieldBatch(requestDTO)));
    }
}

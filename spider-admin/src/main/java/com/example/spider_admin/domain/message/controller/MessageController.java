package com.example.spider_admin.domain.message.controller;

import com.example.spider_admin.domain.message.dto.FieldPoolVerifyRequest;
import com.example.spider_admin.domain.message.dto.FieldPoolVerifyResponse;
import com.example.spider_admin.domain.message.dto.HeaderMessageResponse;
import com.example.spider_admin.domain.message.dto.MessageBackupRequest;
import com.example.spider_admin.domain.message.dto.MessageCreateRequest;
import com.example.spider_admin.domain.message.dto.MessageDetailResponse;
import com.example.spider_admin.domain.message.dto.MessageExcelImportResponse;
import com.example.spider_admin.domain.message.dto.MessageListItemResponse;
import com.example.spider_admin.domain.message.dto.MessageRestoreRequest;
import com.example.spider_admin.domain.message.dto.MessageSearchRequest;
import com.example.spider_admin.domain.message.dto.MessageUpdateRequest;
import com.example.spider_admin.domain.message.dto.MessageVersionResponse;
import com.example.spider_admin.domain.message.dto.StdMessageSearchResponse;
import com.example.spider_admin.domain.message.dto.TableColumnResponse;
import com.example.spider_admin.domain.message.service.MessageService;
import com.example.spider_admin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spider_admin.domain.messageparsing.dto.MessageResponse;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Message 리소스에 대한 CRUD 및 검색 인터페이스를 제공합니다.
 * 모든 응답은 {@link ApiResponse} 규격으로 통일하여 반환합니다.
 *
 * @see MessageService
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MESSAGE:R')")
public class MessageController {

    private final MessageService messageService;

    /**
     * 기관 ID에 해당하는 헤더 전문 목록 조회 (상위전문ID select box용)
     *
     * @param orgId 기관 ID
     * @return 헤더 전문 목록 (messageId, messageName)
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getMessageTypes() {
        return ResponseEntity.ok(ApiResponse.success(messageService.getMessageTypes()));
    }

    @GetMapping("/validation-rules")
    public ResponseEntity<ApiResponse<List<String>>> getValidationRuleIds() {
        return ResponseEntity.ok(ApiResponse.success(messageService.getValidationRuleIds()));
    }

    @GetMapping("/headers")
    public ResponseEntity<ApiResponse<List<HeaderMessageResponse>>> getHeaderMessages(@RequestParam String orgId) {

        return ResponseEntity.ok(ApiResponse.success(messageService.getHeaderMessages(orgId)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMessages(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String orgIdFilter,
            @RequestParam(required = false) String headerYnFilter,
            @RequestParam(required = false) String parentMessageIdFilter,
            @RequestParam(required = false) String messageTypeFilter) {

        byte[] excelBytes = messageService.exportMessages(
                searchField,
                searchValue,
                orgIdFilter,
                headerYnFilter,
                parentMessageIdFilter,
                messageTypeFilter,
                sortBy,
                sortDirection);
        String fileName = ExcelExportUtil.generateFileName("Message", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * 엑셀 파일에서 전문 데이터를 읽어 일괄 등록/수정합니다.
     *
     * @param file 업로드할 .xlsx 파일
     * @return 처리 결과 (생성/수정/건너뜀/오류 건수)
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MESSAGE:W')")
    public ResponseEntity<ApiResponse<MessageExcelImportResponse>> importMessages(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(ApiResponse.success(messageService.importFromExcel(file)));
    }

    /**
     * 전문 단건 조회 (필드 포함 옵션)
     * includeFields 파라미터에 따라 필드 목록을 포함할 수 있습니다.
     *
     * @param orgId         기관 ID
     * @param messageId     전문 ID
     * @param includeFields 필드 목록 포함 여부 (기본값: false)
     * @return 전문 정보 (includeFields=true 시 fields 목록 포함)
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponse>> getMessageById(
            @RequestParam String orgId,
            @PathVariable String messageId,
            @RequestParam(defaultValue = "false") boolean includeFields) {

        return ResponseEntity.ok(ApiResponse.success(messageService.getMessageById(orgId, messageId, includeFields)));
    }

    /**
     * 전문 상세 조회 (상위 전문 필드 + 전문 필드 포함)
     * 상위 전문의 필드 목록과 현재 전문의 필드 목록을 모두 포함합니다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return 전문 상세 정보 (parent fields + message fields)
     */
    @GetMapping("/{messageId}/detail")
    public ResponseEntity<ApiResponse<MessageDetailResponse>> getMessageDetail(
            @RequestParam String orgId, @PathVariable String messageId) {

        return ResponseEntity.ok(ApiResponse.success(messageService.getMessageDetail(orgId, messageId)));
    }

    /**
     * 필터 조건에 따른 전문 페이징 검색을 수행합니다.
     *
     * @param page              페이지 번호 (1-based index)
     * @param size              페이지 당 항목 수
     * @param sortBy            정렬 기준 필드
     * @param sortDirection     정렬 방향 (ASC, DESC)
     * @param searchField       검색 대상 필드
     * @param searchValue       검색어
     * @param orgIdFilter       기관 ID 필터
     * @param ioTypeFilter      I/O 타입 필터
     * @param messageTypeFilter 전문 타입 필터
     * @return 페이징 처리된 전문 검색 결과
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<MessageListItemResponse>>> getMessagesWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String orgIdFilter,
            @RequestParam(required = false) String headerYnFilter,
            @RequestParam(required = false) String parentMessageIdFilter,
            @RequestParam(required = false) String messageTypeFilter) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1) // 1-based → 0-based 변환
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        MessageSearchRequest searchDTO = MessageSearchRequest.builder()
                .searchField(searchField)
                .searchValue(searchValue)
                .orgIdFilter(orgIdFilter)
                .headerYnFilter(headerYnFilter)
                .parentMessageIdFilter(parentMessageIdFilter)
                .messageTypeFilter(messageTypeFilter)
                .build();

        return ResponseEntity.ok(ApiResponse.success(messageService.searchMessages(pageRequest, searchDTO)));
    }

    /**
     * 새로운 전문을 생성합니다.
     *
     * @param requestDTO 생성할 전문 정보
     * @return 생성된 전문 정보 (201 Created)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MESSAGE:W')")
    public ResponseEntity<ApiResponse<MessageResponse>> createMessage(
            @Valid @RequestBody MessageCreateRequest requestDTO) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(messageService.createMessage(requestDTO)));
    }

    /**
     * 기존 전문의 정보를 수정합니다.
     *
     * @param orgId      기관 ID
     * @param messageId  전문 ID
     * @param requestDTO 수정할 전문 정보
     * @return 수정된 전문 정보
     */
    @PutMapping("/{messageId}")
    @PreAuthorize("hasAuthority('MESSAGE:W')")
    public ResponseEntity<ApiResponse<MessageResponse>> updateMessage(
            @RequestParam String orgId,
            @PathVariable String messageId,
            @Valid @RequestBody MessageUpdateRequest requestDTO) {

        return ResponseEntity.ok(ApiResponse.success(messageService.updateMessage(orgId, messageId, requestDTO)));
    }

    /**
     * 전문을 삭제합니다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{messageId}")
    @PreAuthorize("hasAuthority('MESSAGE:W')")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@RequestParam String orgId, @PathVariable String messageId) {

        messageService.deleteMessage(orgId, messageId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 전문과 필드를 히스토리 테이블에 백업합니다.
     *
     * @param messageId 전문 ID
     * @param request   백업 요청 DTO (orgId, historyReason)
     * @return 성공 응답
     */
    @PostMapping("/{messageId}/backup")
    @PreAuthorize("hasAuthority('MESSAGE:W')")
    public ResponseEntity<ApiResponse<Void>> backupMessage(
            @PathVariable String messageId, @Valid @RequestBody MessageBackupRequest request) {

        messageService.backupMessage(request.getOrgId(), messageId, request.getHistoryReason());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 히스토리에서 특정 버전의 전문과 필드를 복원합니다.
     *
     * @param messageId 전문 ID
     * @param request   복원 요청 DTO (orgId, version)
     * @return 복원된 전문 정보
     */
    @PostMapping("/{messageId}/restore")
    @PreAuthorize("hasAuthority('MESSAGE:W')")
    public ResponseEntity<ApiResponse<MessageResponse>> restoreMessage(
            @PathVariable String messageId, @Valid @RequestBody MessageRestoreRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                messageService.restoreMessage(request.getOrgId(), messageId, request.getVersion())));
    }

    /**
     * 특정 전문의 백업 버전 목록을 조회합니다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return 버전 목록 (최신순)
     */
    @GetMapping("/{messageId}/versions")
    public ResponseEntity<ApiResponse<List<MessageVersionResponse>>> listVersions(
            @RequestParam String orgId, @PathVariable String messageId) {

        return ResponseEntity.ok(ApiResponse.success(messageService.listVersions(orgId, messageId)));
    }

    /**
     * 특정 버전의 필드 목록을 조회합니다. (복원 전 비교용)
     *
     * @param messageId 전문 ID
     * @param version   버전 번호
     * @param orgId     기관 ID
     * @return 해당 버전의 필드 목록
     */
    @GetMapping("/{messageId}/versions/{version}/fields")
    public ResponseEntity<ApiResponse<List<MessageFieldResponse>>> listFieldsByVersion(
            @PathVariable String messageId, @PathVariable int version, @RequestParam String orgId) {

        return ResponseEntity.ok(ApiResponse.success(messageService.listFieldsByVersion(orgId, messageId, version)));
    }

    /**
     * 필드풀 검증 — 전문필드ID 목록에 대해 FWK_FIELD_POOL 등록 여부를 확인합니다.
     */
    @PostMapping("/field-pool/verify")
    public ResponseEntity<ApiResponse<List<FieldPoolVerifyResponse>>> verifyFieldPool(
            @RequestBody FieldPoolVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(messageService.verifyFieldPool(request.getMessageFieldIds())));
    }

    /**
     * Oracle 시스템 뷰에서 테이블 컬럼 정보를 조회합니다.
     */
    @GetMapping("/table-columns")
    public ResponseEntity<ApiResponse<List<TableColumnResponse>>> findTableColumns(@RequestParam String tableName) {
        return ResponseEntity.ok(ApiResponse.success(messageService.findTableColumns(tableName)));
    }

    /**
     * 헤더전문포함조회 — 상위전문부터 현재전문까지 재귀적으로 모든 필드를 반환합니다.
     */
    @GetMapping("/{messageId}/header-fields")
    public ResponseEntity<ApiResponse<List<MessageFieldResponse>>> listHeaderIncludedFields(
            @PathVariable String messageId, @RequestParam String orgId) {
        return ResponseEntity.ok(ApiResponse.success(messageService.listHeaderIncludedFields(orgId, messageId)));
    }

    /**
     * 표준전문조회복사용 — 거래ID/거래명으로 FWK_TRX_MESSAGE + FWK_TRX + FWK_MESSAGE를 검색합니다.
     *
     * @param page        페이지 번호 (1-based)
     * @param size        페이지 당 항목 수
     * @param searchField 검색 기준 (trxId / trxName)
     * @param searchValue 검색어
     * @return 페이징 처리된 거래-전문 검색 결과
     */
    @GetMapping("/std-search")
    public ResponseEntity<ApiResponse<PageResponse<StdMessageSearchResponse>>> searchStdMessages(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue) {

        PageRequest pageRequest =
                PageRequest.builder().page(page - 1).size(size).build();

        return ResponseEntity.ok(
                ApiResponse.success(messageService.searchTrxMessagesForCopy(pageRequest, searchField, searchValue)));
    }
}

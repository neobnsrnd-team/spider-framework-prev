package com.example.spider_admin.domain.message.service;

import com.example.spider_admin.domain.message.dto.HeaderMessageResponse;
import com.example.spider_admin.domain.message.dto.MessageCreateRequest;
import com.example.spider_admin.domain.message.dto.MessageDetailResponse;
import com.example.spider_admin.domain.message.dto.MessageExcelImportResponse;
import com.example.spider_admin.domain.message.dto.MessageListItemResponse;
import com.example.spider_admin.domain.message.dto.MessageSearchRequest;
import com.example.spider_admin.domain.message.dto.MessageUpdateRequest;
import com.example.spider_admin.domain.message.dto.MessageVersionResponse;
import com.example.spider_admin.domain.message.dto.StdMessageSearchResponse;
import com.example.spider_admin.domain.message.mapper.MessageMapper;
import com.example.spider_admin.domain.messagefield.dto.FieldListResponse;
import com.example.spider_admin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spider_admin.domain.messageparsing.dto.MessageResponse;
import com.example.spider_admin.global.aop.WorkListRecord;
import com.example.spider_admin.global.common.enums.MessageType;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import com.example.spider_admin.global.util.ExcelColumnDefinition;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service implementation for Message management
 * Handles business logic for Message operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageMapper messageMapper;

    public MessageResponse getMessageById(String orgId, String messageId, boolean includeFields) {
        log.info("Finding Message by orgId: {}, messageId: {}, includeFields: {}", orgId, messageId, includeFields);

        MessageResponse message = messageMapper.selectResponseById(orgId, messageId);
        if (message == null) {
            throw new NotFoundException("messageId: " + messageId);
        }

        if (includeFields) {
            List<MessageFieldResponse> fields = messageMapper.findFieldsByMessageId(orgId, messageId);
            message.setFields(fields);
        }

        return message;
    }

    public MessageDetailResponse getMessageDetail(String orgId, String messageId) {
        log.info("Finding Message detail by orgId: {}, messageId: {}", orgId, messageId);

        MessageResponse message = messageMapper.selectResponseById(orgId, messageId);
        if (message == null) {
            throw new NotFoundException("messageId: " + messageId);
        }

        MessageDetailResponse dto = MessageDetailResponse.builder()
                .orgId(message.getOrgId())
                .parentMessageId(message.getParentMessageId())
                .messageId(message.getMessageId())
                .messageName(message.getMessageName())
                .messageDesc(message.getMessageDesc())
                .logLevel(message.getLogLevel())
                .headerYn(message.getHeaderYn())
                .messageType(message.getMessageType())
                .requestYn(message.getRequestYn())
                .preLoadYn(message.getPreLoadYn())
                .bizDomain(message.getBizDomain())
                .build();

        List<MessageFieldResponse> fields = messageMapper.findFieldsByMessageId(orgId, messageId);
        dto.setFields(fields.stream()
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
                .toList());

        return dto;
    }

    public PageResponse<MessageListItemResponse> searchMessages(
            PageRequest pageRequest, MessageSearchRequest searchDTO) {

        log.info("Searching Messages with pageRequest: {}, searchDTO: {}", pageRequest, searchDTO);

        long total = messageMapper.countAllWithSearch(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getOrgIdFilter(),
                searchDTO.getHeaderYnFilter(),
                searchDTO.getParentMessageIdFilter(),
                searchDTO.getMessageTypeFilter());

        List<MessageListItemResponse> dtos = messageMapper.findAllWithSearch(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getOrgIdFilter(),
                searchDTO.getHeaderYnFilter(),
                searchDTO.getParentMessageIdFilter(),
                searchDTO.getMessageTypeFilter(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(dtos, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Transactional
    @WorkListRecord(
            workId = "Message",
            crudType = "C",
            pkExpression = "#requestDTO.orgId + '@' + #requestDTO.messageId",
            workName = "전문")
    public MessageResponse createMessage(MessageCreateRequest requestDTO) {
        log.info("Creating Message: orgId={}, messageId={}", requestDTO.getOrgId(), requestDTO.getMessageId());

        // 중복 체크
        if (messageMapper.countByMessageId(requestDTO.getOrgId(), requestDTO.getMessageId()) > 0) {
            throw new DuplicateException("messageId: " + requestDTO.getMessageId());
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        // 저장
        messageMapper.insertMessage(requestDTO, now, currentUserId);
        log.info(
                "Message created successfully: orgId={}, messageId={}",
                requestDTO.getOrgId(),
                requestDTO.getMessageId());

        return messageMapper.selectResponseById(requestDTO.getOrgId(), requestDTO.getMessageId());
    }

    @Transactional
    @WorkListRecord(workId = "Message", crudType = "U", pkExpression = "#orgId + '@' + #messageId", workName = "전문")
    public MessageResponse updateMessage(String orgId, String messageId, MessageUpdateRequest requestDTO) {
        log.info("Updating Message: orgId={}, messageId={}", orgId, messageId);

        // 존재 확인
        if (messageMapper.countByMessageId(orgId, messageId) == 0) {
            throw new NotFoundException("messageId: " + messageId);
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        // 저장
        messageMapper.updateMessage(orgId, messageId, requestDTO, now, currentUserId);
        log.info("Message updated successfully: orgId={}, messageId={}", orgId, messageId);

        return messageMapper.selectResponseById(orgId, messageId);
    }

    @Transactional
    @WorkListRecord(workId = "Message", crudType = "D", pkExpression = "#orgId + '@' + #messageId", workName = "전문")
    public void deleteMessage(String orgId, String messageId) {
        log.info("Deleting Message: orgId={}, messageId={}", orgId, messageId);

        if (messageMapper.countByMessageId(orgId, messageId) == 0) {
            throw new NotFoundException("messageId: " + messageId);
        }

        messageMapper.deleteMessage(orgId, messageId);
        log.info("Message deleted successfully: orgId={}, messageId={}", orgId, messageId);
    }

    public byte[] exportMessages(
            String searchField,
            String searchValue,
            String orgIdFilter,
            String headerYnFilter,
            String parentMessageIdFilter,
            String messageTypeFilter,
            String sortBy,
            String sortDirection) {
        List<MessageListItemResponse> data = messageMapper.findAllForExport(
                searchField,
                searchValue,
                orgIdFilter,
                headerYnFilter,
                parentMessageIdFilter,
                messageTypeFilter,
                sortBy,
                sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("기관명", 10, "orgId"),
                new ExcelColumnDefinition("전문ID", 20, "messageId"),
                new ExcelColumnDefinition("전문명", 25, "messageName"),
                new ExcelColumnDefinition("전문설명", 30, "messageDesc"),
                new ExcelColumnDefinition("전문유형", 12, "messageType"),
                new ExcelColumnDefinition("상위전문ID", 20, "parentMessageId"),
                new ExcelColumnDefinition("헤더여부", 10, "headerYn"),
                new ExcelColumnDefinition("요청여부", 10, "requestYn"),
                new ExcelColumnDefinition("잠금여부", 10, "lockYn"),
                new ExcelColumnDefinition("버전", 8, "curVersion"),
                new ExcelColumnDefinition("최종 수정 일시", 20, "lastUpdateDtime"),
                new ExcelColumnDefinition("최종 수정자", 15, "lastUpdateUserId"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (MessageListItemResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orgId", item.getOrgId());
            row.put("messageId", item.getMessageId());
            row.put("messageName", item.getMessageName());
            row.put("messageDesc", item.getMessageDesc());
            row.put("messageType", item.getMessageType());
            row.put("parentMessageId", item.getParentMessageId());
            row.put("headerYn", item.getHeaderYn());
            row.put("requestYn", item.getRequestYn());
            row.put("lockYn", item.getLockYn());
            row.put("curVersion", item.getCurVersion());
            row.put("lastUpdateDtime", item.getLastUpdateDtime());
            row.put("lastUpdateUserId", item.getLastUpdateUserId());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("전문", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 엑셀 파일에서 전문 데이터를 읽어 일괄 등록/수정합니다.
     * 엑셀 컬럼 순서는 export 형식과 동일합니다:
     * 기관명(orgId), 전문ID, 전문명, 전문설명, 전문유형, 상위전문ID, 헤더여부, 요청여부, 잠금여부, 버전
     *
     * @param file 업로드된 .xlsx 파일
     * @return 처리 결과 (생성/수정/건너뜀/오류 건수)
     */
    @Transactional
    public MessageExcelImportResponse importFromExcel(MultipartFile file) {
        log.info("Importing messages from Excel: {}", file.getOriginalFilename());

        validateExcelFile(file);

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        int created = 0;
        int updated = 0;
        int skipped = 0;
        int totalRows = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int rowIdx = 1; rowIdx <= lastRowNum; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }
                totalRows++;

                int result = processExcelRow(row, rowIdx, now, currentUserId, errors);
                if (result > 0) {
                    created++;
                } else if (result == 0) {
                    updated++;
                } else {
                    skipped++;
                }
            }
        } catch (InvalidInputException e) {
            throw e;
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 읽기 중 오류가 발생했습니다", e);
        } catch (Exception e) {
            throw new InternalException("엑셀 파일 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

        log.info(
                "Excel import completed: total={}, created={}, updated={}, skipped={}, errors={}",
                totalRows,
                created,
                updated,
                skipped,
                errors.size());

        return MessageExcelImportResponse.builder()
                .totalRows(totalRows)
                .created(created)
                .updated(updated)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    /**
     * 엑셀 행 하나를 처리합니다.
     *
     * @return 1 = created, 0 = updated, -1 = skipped
     */
    private int processExcelRow(Row row, int rowIdx, String now, String currentUserId, List<String> errors) {
        String orgId = getCellStringValue(row, 0);
        String messageId = getCellStringValue(row, 1);

        if (orgId == null || orgId.isBlank()) {
            errors.add(rowIdx + 1 + "행: 기관ID가 비어있습니다.");
            return -1;
        }
        if (messageId == null || messageId.isBlank()) {
            errors.add(rowIdx + 1 + "행: 전문ID가 비어있습니다.");
            return -1;
        }

        try {
            return upsertMessageFromRow(row, orgId, messageId, now, currentUserId);
        } catch (Exception e) {
            errors.add(rowIdx + 1 + "행: " + e.getMessage());
            return -1;
        }
    }

    private int upsertMessageFromRow(Row row, String orgId, String messageId, String now, String currentUserId) {
        String messageName = getCellStringValue(row, 2);
        String messageDesc = getCellStringValue(row, 3);
        String messageType = getCellStringValue(row, 4);
        String parentMessageId = getCellStringValue(row, 5);
        String headerYn = getCellStringValue(row, 6);
        String requestYn = getCellStringValue(row, 7);
        String lockYn = getCellStringValue(row, 8);

        boolean exists = messageMapper.countByMessageId(orgId, messageId) > 0;

        if (exists) {
            MessageUpdateRequest updateReq = MessageUpdateRequest.builder()
                    .messageName(messageName)
                    .messageDesc(messageDesc)
                    .messageType(messageType)
                    .parentMessageId(parentMessageId)
                    .headerYn(headerYn)
                    .requestYn(requestYn)
                    .lockYn(lockYn)
                    .build();
            messageMapper.updateMessage(orgId, messageId, updateReq, now, currentUserId);
            return 0;
        } else {
            MessageCreateRequest createReq = MessageCreateRequest.builder()
                    .orgId(orgId)
                    .messageId(messageId)
                    .messageName(messageName)
                    .messageDesc(messageDesc)
                    .messageType(messageType)
                    .parentMessageId(parentMessageId)
                    .headerYn(headerYn)
                    .requestYn(requestYn)
                    .lockYn(lockYn)
                    .build();
            messageMapper.insertMessage(createReq, now, currentUserId);
            return 1;
        }
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidInputException("업로드할 파일을 선택해주세요.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".xlsx")) {
            throw new InvalidInputException("xlsx 형식의 파일만 업로드 가능합니다.");
        }

        // 10MB 제한
        long maxSize = 10L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new InvalidInputException("파일 크기가 10MB를 초과합니다.");
        }
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < 2; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(row, i);
                if (value != null && !value.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getCellStringValue(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double numericValue = cell.getNumericCellValue();
                if (numericValue == Math.floor(numericValue) && !Double.isInfinite(numericValue)) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    public List<Map<String, String>> getMessageTypes() {
        return messageMapper.findDistinctMessageTypes().stream()
                .map(code -> {
                    MessageType type = MessageType.fromCode(code);
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("code", code);
                    entry.put("name", type != null ? type.getDescription() : code);
                    return entry;
                })
                .toList();
    }

    public List<HeaderMessageResponse> getHeaderMessages(String orgId) {
        log.info("Finding header messages by orgId: {}", orgId);
        return messageMapper.findHeaderMessagesByOrgId(orgId);
    }

    public List<String> getValidationRuleIds() {
        return messageMapper.findDistinctValidationRuleIds();
    }

    /**
     * 전문과 필드를 히스토리 테이블에 백업합니다.
     *
     * @param orgId         기관 ID
     * @param messageId     전문 ID
     * @param historyReason 백업 사유
     */
    @Transactional
    public void backupMessage(String orgId, String messageId, String historyReason) {
        log.info("Backing up message: orgId={}, messageId={}", orgId, messageId);

        if (messageMapper.countByMessageId(orgId, messageId) == 0) {
            throw new NotFoundException("messageId: " + messageId);
        }

        // Get current max version and increment
        Integer maxVersion = messageMapper.getMaxVersion(orgId, messageId);
        int nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        // 1. Copy message to history
        messageMapper.insertMessageHistory(orgId, messageId, nextVersion, historyReason, now, userId);

        // 2. Copy fields to history
        messageMapper.insertMessageFieldHistory(orgId, messageId, nextVersion, now, userId);

        // 3. Update version in FWK_MESSAGE
        messageMapper.updateMessageVersion(orgId, messageId, nextVersion, now, userId);

        log.info("Message backed up: version={}", nextVersion);
    }

    /**
     * 히스토리에서 특정 버전의 전문과 필드를 복원합니다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @param version   복원할 버전
     * @return 복원된 전문 정보
     */
    @Transactional
    public MessageResponse restoreMessage(String orgId, String messageId, int version) {
        log.info("Restoring message: orgId={}, messageId={}, version={}", orgId, messageId, version);

        // Verify version exists
        List<MessageVersionResponse> versions = messageMapper.findVersionsByMessageId(orgId, messageId);
        boolean versionExists = versions.stream().anyMatch(v -> v.getVersion() == version);
        if (!versionExists) {
            throw new NotFoundException("version: " + version);
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        // 1. Delete current fields
        messageMapper.deleteMessageFields(orgId, messageId);

        // 2. Restore message from history (MERGE)
        messageMapper.restoreMessageFromHistory(orgId, messageId, version, now, userId);

        // 3. Restore fields from history
        messageMapper.restoreMessageFieldsFromHistory(orgId, messageId, version, now, userId);

        log.info("Message restored from version: {}", version);
        return messageMapper.selectResponseById(orgId, messageId);
    }

    /**
     * 특정 전문의 백업 버전 목록을 조회합니다.
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @return 버전 목록 (최신순)
     */
    public List<MessageVersionResponse> listVersions(String orgId, String messageId) {
        return messageMapper.findVersionsByMessageId(orgId, messageId);
    }

    /**
     * 특정 버전의 필드 목록을 조회합니다. (복원 전 비교용)
     *
     * @param orgId     기관 ID
     * @param messageId 전문 ID
     * @param version   버전 번호
     * @return 해당 버전의 필드 목록
     */
    public List<MessageFieldResponse> listFieldsByVersion(String orgId, String messageId, int version) {
        log.info("Finding fields by version: orgId={}, messageId={}, version={}", orgId, messageId, version);
        return messageMapper.findFieldsByVersion(orgId, messageId, version);
    }

    /**
     * 필드풀 검증 — 전문필드ID 목록에 대해 FWK_FIELD_POOL 등록 여부를 확인합니다.
     */
    @Transactional(readOnly = true)
    public List<com.example.spider_admin.domain.message.dto.FieldPoolVerifyResponse> verifyFieldPool(
            List<String> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return List.of();
        }
        return messageMapper.verifyFieldPool(fieldIds);
    }

    /**
     * Oracle 시스템 뷰에서 테이블 컬럼 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<com.example.spider_admin.domain.message.dto.TableColumnResponse> findTableColumns(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return List.of();
        }
        return messageMapper.findTableColumns(tableName.toUpperCase().trim());
    }

    /**
     * 헤더전문포함조회 — 상위전문부터 현재전문까지 재귀적으로 모든 필드를 수집합니다.
     */
    @Transactional(readOnly = true)
    public List<MessageFieldResponse> listHeaderIncludedFields(String orgId, String messageId) {
        List<MessageFieldResponse> result = new ArrayList<>();
        collectFieldsRecursive(orgId, messageId, result, 0);
        return result;
    }

    /**
     * 표준전문조회복사용 — 거래ID/거래명으로 FWK_TRX_MESSAGE를 검색합니다.
     *
     * @param pageRequest 페이지 요청 (page, size)
     * @param searchField 검색 기준 (trxId / trxName)
     * @param searchValue 검색어
     * @return 페이징 처리된 거래-전문 검색 결과
     */
    public PageResponse<StdMessageSearchResponse> searchTrxMessagesForCopy(
            PageRequest pageRequest, String searchField, String searchValue) {

        long total = messageMapper.countTrxMessagesForStdSearch(searchField, searchValue);

        List<StdMessageSearchResponse> list = messageMapper.findTrxMessagesForStdSearch(
                searchField, searchValue, pageRequest.getOffset(), pageRequest.getEndRow());

        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    private void collectFieldsRecursive(String orgId, String messageId, List<MessageFieldResponse> result, int depth) {
        if (depth > 10) return; // 무한재귀 방지

        MessageResponse msg = messageMapper.selectResponseById(orgId, messageId);
        if (msg == null) return;

        // 상위전문이 있으면 먼저 재귀 (상위→하위 순서)
        if (msg.getParentMessageId() != null && !msg.getParentMessageId().isEmpty()) {
            collectFieldsRecursive(orgId, msg.getParentMessageId(), result, depth + 1);
        }

        // 현재 전문의 필드 추가
        List<MessageFieldResponse> fields = messageMapper.findFieldsByMessageId(orgId, messageId);
        result.addAll(fields);
    }
}

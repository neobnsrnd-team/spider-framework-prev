package com.example.spideradmin.domain.messagehandler.service;

import com.example.spideradmin.domain.messagehandler.dto.HandlerBatchRequest;
import com.example.spideradmin.domain.messagehandler.dto.HandlerDeleteRequest;
import com.example.spideradmin.domain.messagehandler.dto.HandlerResponse;
import com.example.spideradmin.domain.messagehandler.dto.HandlerUpsertRequest;
import com.example.spideradmin.domain.messagehandler.mapper.MessageHandlerMapper;
import com.example.spideradmin.domain.transport.service.TransportService;
import com.example.spideradmin.global.common.enums.UseYnFlag;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.util.AuditUtil;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
import com.example.spideradmin.global.util.ValidationUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageHandlerService {

    private final MessageHandlerMapper handlerMapper;
    private final TransportService transportService;

    public PageResponse<HandlerResponse> searchHandlers(
            PageRequest pageRequest, String orgId, String trxType, String ioType) {
        int offset = pageRequest.getPage() * pageRequest.getSize();
        int limit = pageRequest.getSize();

        List<HandlerResponse> content = handlerMapper.findBySearchPagingDto(
                orgId, trxType, ioType, offset, limit, pageRequest.getSortBy(), pageRequest.getSortDirection());
        long totalCount = handlerMapper.countBySearch(orgId, trxType, ioType);

        int totalPages = (int) Math.ceil((double) totalCount / pageRequest.getSize());
        PageResponse<HandlerResponse> response = new PageResponse<>();
        response.setContent(content);
        response.setTotalElements(totalCount);
        response.setTotalPages(totalPages);
        response.setCurrentPage(pageRequest.getPage() + 1);
        response.setSize(pageRequest.getSize());
        response.setHasPrevious(pageRequest.getPage() > 0);
        response.setHasNext(pageRequest.getPage() + 1 < totalPages);
        return response;
    }

    public byte[] exportMessageHandlers(
            String orgId, String trxType, String ioType, String sortBy, String sortDirection) {
        List<HandlerResponse> data = handlerMapper.findAllForExport(orgId, trxType, ioType, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("기관명", 15, "orgId"),
                new ExcelColumnDefinition("거래유형", 15, "trxType"),
                new ExcelColumnDefinition("어댑터리스너", 15, "ioType"),
                new ExcelColumnDefinition("운영모드", 15, "operModeType"),
                new ExcelColumnDefinition("전문처리핸들러", 15, "handler"),
                new ExcelColumnDefinition("전문처리핸들러설명", 20, "handlerDesc"),
                new ExcelColumnDefinition("상태", 15, "stopYn"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orgId", item.getOrgId());
            row.put("trxType", item.getTrxType());
            row.put("ioType", item.getIoType());
            row.put("operModeType", item.getOperModeType());
            row.put("handler", item.getHandler());
            row.put("handlerDesc", item.getHandlerDesc());
            row.put("stopYn", item.getStopYn());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("전문처리핸들러", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    @Transactional
    public void saveBatch(HandlerBatchRequest request) {
        if (request == null) {
            throw new InvalidInputException("요청 정보가 없습니다.");
        }

        List<HandlerDeleteRequest> deletes = request.getDeletes();
        if (deletes != null) {
            for (HandlerDeleteRequest dto : deletes) {
                if (dto == null) {
                    continue;
                }
                handlerMapper.deleteHandler(dto.getOrgId(), dto.getTrxType(), dto.getIoType(), dto.getOperModeType());
            }
        }

        List<HandlerUpsertRequest> upserts = request.getUpserts();
        if (upserts == null) {
            upserts = Collections.emptyList();
        }

        validateDuplicateKeys(upserts);

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        for (HandlerUpsertRequest dto : upserts) {
            validateHandler(dto);
            if (dto.getStopYn() == null || dto.getStopYn().isBlank()) {
                dto.setStopYn("N");
            }
            int count = handlerMapper.countByHandler(
                    dto.getOrgId(), dto.getTrxType(), dto.getIoType(), dto.getOperModeType());
            if (count == 0) {
                handlerMapper.insertHandler(dto, now, currentUserId);
            } else {
                handlerMapper.updateHandler(dto, now, currentUserId);
            }
        }
    }

    private void validateDuplicateKeys(List<HandlerUpsertRequest> upserts) {
        if (upserts == null || upserts.isEmpty()) {
            return;
        }
        var seen = upserts.stream()
                .filter(Objects::nonNull)
                .map(dto -> String.join(
                        "|",
                        dto.getOrgId() == null ? "" : dto.getOrgId(),
                        dto.getTrxType() == null ? "" : dto.getTrxType(),
                        dto.getIoType() == null ? "" : dto.getIoType(),
                        dto.getOperModeType() == null ? "" : dto.getOperModeType()))
                .collect(Collectors.groupingBy(key -> key, Collectors.counting()));

        String duplicateKey = seen.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (duplicateKey != null) {
            throw new InvalidInputException("동일 기관/거래유형/IO/운영모드 조합이 중복되었습니다.");
        }
    }

    private void validateHandler(HandlerUpsertRequest dto) {
        if (dto == null) {
            throw new InvalidInputException("전문처리 핸들러 정보가 없습니다.");
        }
        if (!transportService.isValidTrxType(dto.getTrxType())) {
            throw new InvalidInputException("거래유형 값이 올바르지 않습니다.");
        }
        if (!ValidationUtils.isValidIoType(dto.getIoType())) {
            throw new InvalidInputException("어댑터/리스너 구분 값이 올바르지 않습니다.");
        }
        if (!ValidationUtils.isValidOperModeType(dto.getOperModeType())) {
            throw new InvalidInputException("운영모드 값이 올바르지 않습니다.");
        }
        if (dto.getStopYn() != null && !dto.getStopYn().isBlank() && UseYnFlag.fromCode(dto.getStopYn()) == null) {
            throw new InvalidInputException("상태 값이 올바르지 않습니다.");
        }
    }
}

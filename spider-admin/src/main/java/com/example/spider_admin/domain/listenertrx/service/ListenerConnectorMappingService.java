package com.example.spider_admin.domain.listenertrx.service;

import com.example.spider_admin.domain.gateway.mapper.GatewayMapper;
import com.example.spider_admin.domain.gwsystem.mapper.SystemMapper;
import com.example.spider_admin.domain.listenertrx.dto.ListenerConnectorMappingBatchRequest;
import com.example.spider_admin.domain.listenertrx.dto.ListenerConnectorMappingResponse;
import com.example.spider_admin.domain.listenertrx.dto.ListenerConnectorMappingUpsertRequest;
import com.example.spider_admin.domain.listenertrx.mapper.ListenerConnectorMappingMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.ExcelColumnDefinition;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListenerConnectorMappingService {

    private final ListenerConnectorMappingMapper mappingMapper;
    private final GatewayMapper gatewayMapper;
    private final SystemMapper systemMapper;

    public PageResponse<ListenerConnectorMappingResponse> searchMappings(
            PageRequest pageRequest,
            String listenerGwId,
            String listenerSystemId,
            String identifier,
            String connectorGwId,
            String connectorSystemId) {

        int page = Math.max(pageRequest.getPage(), 0);
        int size = Math.max(pageRequest.getSize(), 1);
        int offset = page * size;

        String searchListenerGwId = trimToNull(listenerGwId);
        String searchListenerSystemId = trimToNull(listenerSystemId);
        String searchIdentifier = trimToNull(identifier);
        String searchConnectorGwId = trimToNull(connectorGwId);
        String searchConnectorSystemId = trimToNull(connectorSystemId);

        List<ListenerConnectorMappingResponse> content = mappingMapper.findBySearchPaging(
                searchListenerGwId,
                searchListenerSystemId,
                searchIdentifier,
                searchConnectorGwId,
                searchConnectorSystemId,
                offset,
                size,
                pageRequest.getSortBy(),
                pageRequest.getSortDirection());

        long total = mappingMapper.countBySearch(
                searchListenerGwId,
                searchListenerSystemId,
                searchIdentifier,
                searchConnectorGwId,
                searchConnectorSystemId);

        if (content == null) {
            content = new ArrayList<>();
        }

        int totalPages = (int) Math.ceil((double) total / size);

        return PageResponse.<ListenerConnectorMappingResponse>builder()
                .content(content)
                .currentPage(page + 1)
                .totalPages(totalPages)
                .totalElements(total)
                .size(size)
                .hasPrevious(page > 0)
                .hasNext(page + 1 < totalPages)
                .build();
    }

    public ListenerConnectorMappingResponse getMappingByPk(
            String listenerGwId, String listenerSystemId, String identifier) {

        ListenerConnectorMappingResponse mapping =
                mappingMapper.selectResponseByPk(listenerGwId, listenerSystemId, identifier);

        if (mapping == null) {
            throw new NotFoundException(String.format(
                    "listenerGwId: %s, listenerSystemId: %s, identifier: %s",
                    listenerGwId, listenerSystemId, identifier));
        }

        return mapping;
    }

    @Transactional
    public void createMapping(ListenerConnectorMappingUpsertRequest request) {
        // 복합키 중복 검증
        if (mappingMapper.countByPk(request.getListenerGwId(), request.getListenerSystemId(), request.getIdentifier())
                > 0) {
            throw new InvalidInputException(String.format(
                    "이미 존재하는 리스너-커넥터 매핑입니다: listenerGwId=%s, listenerSystemId=%s, identifier=%s",
                    request.getListenerGwId(), request.getListenerSystemId(), request.getIdentifier()));
        }

        // 리스너 Gateway/System 참조 무결성 검증
        validateListenerReference(request.getListenerGwId(), request.getListenerSystemId());

        // 커넥터 Gateway/System 참조 무결성 검증
        validateConnectorReference(request.getConnectorGwId(), request.getConnectorSystemId());

        mappingMapper.insert(request);
    }

    @Transactional
    public void updateMapping(
            String listenerGwId,
            String listenerSystemId,
            String identifier,
            ListenerConnectorMappingUpsertRequest request) {

        if (mappingMapper.countByPk(listenerGwId, listenerSystemId, identifier) == 0) {
            throw new NotFoundException(String.format(
                    "listenerGwId: %s, listenerSystemId: %s, identifier: %s",
                    listenerGwId, listenerSystemId, identifier));
        }

        // 커넥터 Gateway/System 참조 무결성 검증
        validateConnectorReference(request.getConnectorGwId(), request.getConnectorSystemId());

        mappingMapper.update(listenerGwId, listenerSystemId, identifier, request);
    }

    @Transactional
    public void deleteMapping(String listenerGwId, String listenerSystemId, String identifier) {

        if (mappingMapper.countByPk(listenerGwId, listenerSystemId, identifier) == 0) {
            throw new NotFoundException(String.format(
                    "listenerGwId: %s, listenerSystemId: %s, identifier: %s",
                    listenerGwId, listenerSystemId, identifier));
        }

        mappingMapper.deleteByPk(listenerGwId, listenerSystemId, identifier);
    }

    @Transactional
    public void saveMappingBatch(ListenerConnectorMappingBatchRequest request) {
        if (request.getMappings() == null || request.getMappings().isEmpty()) {
            throw new InvalidInputException("매핑 목록이 비어있습니다");
        }

        // 중복(PK) 선검증 및 참조 무결성 검증
        List<ListenerConnectorMappingUpsertRequest> mappings = request.getMappings();
        java.util.Set<String> seenKeys = new java.util.HashSet<>();

        for (ListenerConnectorMappingUpsertRequest dto : mappings) {
            String key = buildPkKey(dto.getListenerGwId(), dto.getListenerSystemId(), dto.getIdentifier());
            if (!seenKeys.add(key)) {
                throw new InvalidInputException(String.format(
                        "배치 내 중복 매핑이 있습니다: listenerGwId=%s, listenerSystemId=%s, identifier=%s",
                        dto.getListenerGwId(), dto.getListenerSystemId(), dto.getIdentifier()));
            }

            if (mappingMapper.countByPk(dto.getListenerGwId(), dto.getListenerSystemId(), dto.getIdentifier()) > 0) {
                throw new InvalidInputException(String.format(
                        "이미 존재하는 매핑이 포함되어 있습니다: listenerGwId=%s, listenerSystemId=%s, identifier=%s",
                        dto.getListenerGwId(), dto.getListenerSystemId(), dto.getIdentifier()));
            }

            validateListenerReference(dto.getListenerGwId(), dto.getListenerSystemId());
            validateConnectorReference(dto.getConnectorGwId(), dto.getConnectorSystemId());
        }

        mappingMapper.insertBatch(mappings);
    }

    private void validateListenerReference(String listenerGwId, String listenerSystemId) {
        if (gatewayMapper.countByGwId(listenerGwId) == 0) {
            throw new InvalidInputException(String.format("리스너 Gateway가 존재하지 않습니다: %s", listenerGwId));
        }

        if (systemMapper.countBySystem(listenerGwId, listenerSystemId) == 0) {
            throw new InvalidInputException(
                    String.format("리스너 System이 존재하지 않습니다: gwId=%s, systemId=%s", listenerGwId, listenerSystemId));
        }
    }

    private void validateConnectorReference(String connectorGwId, String connectorSystemId) {
        if (gatewayMapper.countByGwId(connectorGwId) == 0) {
            throw new InvalidInputException(String.format("커넥터 Gateway가 존재하지 않습니다: %s", connectorGwId));
        }

        if (systemMapper.countBySystem(connectorGwId, connectorSystemId) == 0) {
            throw new InvalidInputException(
                    String.format("커넥터 System이 존재하지 않습니다: gwId=%s, systemId=%s", connectorGwId, connectorSystemId));
        }
    }

    public byte[] exportListenerConnectorMappings(
            String listenerGwId,
            String listenerSystemId,
            String identifier,
            String connectorGwId,
            String connectorSystemId,
            String sortBy,
            String sortDirection) {

        List<ListenerConnectorMappingResponse> data = mappingMapper.findAllForExport(
                trimToNull(listenerGwId),
                trimToNull(listenerSystemId),
                trimToNull(identifier),
                trimToNull(connectorGwId),
                trimToNull(connectorSystemId),
                sortBy,
                sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("리스너 GATEWAY", 15, "listenerGwId"),
                new ExcelColumnDefinition("리스너 SYSTEM", 15, "listenerSystemId"),
                new ExcelColumnDefinition("응답커넥터 GATEWAY", 18, "connectorGwId"),
                new ExcelColumnDefinition("응답커넥터 SYSTEM", 18, "connectorSystemId"),
                new ExcelColumnDefinition("식별자", 15, "identifier"),
                new ExcelColumnDefinition("설명", 15, "description"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("listenerGwId", item.getListenerGwId());
            row.put("listenerSystemId", item.getListenerSystemId());
            row.put("connectorGwId", item.getConnectorGwId());
            row.put("connectorSystemId", item.getConnectorSystemId());
            row.put("identifier", item.getIdentifier());
            row.put("description", item.getDescription());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("리스너커넥터매핑", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String buildPkKey(String listenerGwId, String listenerSystemId, String identifier) {
        return String.join(
                "|",
                listenerGwId == null ? "" : listenerGwId,
                listenerSystemId == null ? "" : listenerSystemId,
                identifier == null ? "" : identifier);
    }
}

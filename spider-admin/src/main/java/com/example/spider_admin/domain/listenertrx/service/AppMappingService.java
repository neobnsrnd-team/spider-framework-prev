package com.example.spider_admin.domain.listenertrx.service;

import com.example.spider_admin.domain.listenertrx.dto.AppMappingResponse;
import com.example.spider_admin.domain.listenertrx.dto.AppMappingUpsertRequest;
import com.example.spider_admin.domain.listenertrx.mapper.ListenerTrxMessageMapper;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppMappingService {

    private final ListenerTrxMessageMapper mappingMapper;

    private static final java.util.Set<String> ALLOWED_SORT_COLUMNS =
            java.util.Set.of("gwId", "gwName", "orgId", "reqIdCode", "bizAppId", "orgName", "trxName");

    public PageResponse<AppMappingResponse> searchMappings(
            PageRequest pageRequest, String gwId, String orgId, String reqIdCode, String trxKeyword, String bizAppId) {

        String searchGwId = trimToNull(gwId);
        String searchOrgId = trimToNull(orgId);
        String searchReqIdCode = trimToNull(reqIdCode);
        String searchTrxKeyword = trimToNull(trxKeyword);
        String searchBizAppId = trimToNull(bizAppId);

        // 정렬 필드 검증
        String sortBy = pageRequest.getSortBy();
        if (sortBy != null && !ALLOWED_SORT_COLUMNS.contains(sortBy)) {
            log.warn("Invalid sortBy value rejected: {}", sortBy);
            sortBy = null;
        }
        String sortDirection = pageRequest.getSortDirection();

        int page = pageRequest.getPage();
        int size = pageRequest.getSize();
        int offset = pageRequest.getOffset();
        int endRow = pageRequest.getEndRow();

        long total =
                mappingMapper.countBySearch(searchGwId, searchOrgId, searchReqIdCode, searchTrxKeyword, searchBizAppId);

        List<AppMappingResponse> content = mappingMapper.findBySearch(
                searchGwId,
                searchOrgId,
                searchReqIdCode,
                searchTrxKeyword,
                searchBizAppId,
                sortBy,
                sortDirection,
                offset,
                endRow);

        return PageResponse.of(content, total, page, size);
    }

    public AppMappingResponse getMappingByPk(String gwId, String reqIdCode) {
        AppMappingResponse mapping = mappingMapper.selectResponseByPk(gwId, reqIdCode);
        if (mapping == null) {
            throw new NotFoundException(String.format("gwId=%s, reqIdCode=%s", gwId, reqIdCode));
        }
        return mapping;
    }

    @Transactional
    public void createMapping(AppMappingUpsertRequest request) {
        if (mappingMapper.countByPk(request.getGwId(), request.getReqIdCode()) > 0) {
            throw new InvalidInputException(
                    String.format("이미 존재하는 매핑입니다: gwId=%s, reqIdCode=%s", request.getGwId(), request.getReqIdCode()));
        }

        mappingMapper.insert(request);
    }

    @Transactional
    public void updateMapping(String gwId, String reqIdCode, AppMappingUpsertRequest request) {
        if (mappingMapper.countByPk(gwId, reqIdCode) == 0) {
            throw new NotFoundException(String.format("gwId=%s, reqIdCode=%s", gwId, reqIdCode));
        }

        mappingMapper.update(gwId, reqIdCode, request);
    }

    @Transactional
    public void deleteMapping(String gwId, String reqIdCode) {
        if (mappingMapper.countByPk(gwId, reqIdCode) == 0) {
            throw new NotFoundException(String.format("gwId=%s, reqIdCode=%s", gwId, reqIdCode));
        }
        mappingMapper.deleteByPk(gwId, reqIdCode);
    }

    public byte[] exportAppMappings(
            String gwId,
            String orgId,
            String reqIdCode,
            String trxKeyword,
            String bizAppId,
            String sortBy,
            String sortDirection) {
        String searchGwId = trimToNull(gwId);
        String searchOrgId = trimToNull(orgId);
        String searchReqIdCode = trimToNull(reqIdCode);
        String searchTrxKeyword = trimToNull(trxKeyword);
        String searchBizAppId = trimToNull(bizAppId);

        if (sortBy != null && !ALLOWED_SORT_COLUMNS.contains(sortBy)) {
            sortBy = null;
        }

        List<AppMappingResponse> data = mappingMapper.findAllForExport(
                searchGwId, searchOrgId, searchReqIdCode, searchTrxKeyword, searchBizAppId, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("게이트웨이", 15, "gwId"),
                new ExcelColumnDefinition("게이트웨이명", 20, "gwName"),
                new ExcelColumnDefinition("전문식별자", 15, "reqIdCode"),
                new ExcelColumnDefinition("기관", 15, "orgId"),
                new ExcelColumnDefinition("기관명", 20, "orgName"),
                new ExcelColumnDefinition("거래", 25, "trxName"),
                new ExcelColumnDefinition("요청처리 App", 12, "ioType"),
                new ExcelColumnDefinition("거래전문", 15, "bizAppId"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("gwId", item.getGwId());
            row.put("gwName", item.getGwName());
            row.put("reqIdCode", item.getReqIdCode());
            row.put("orgId", item.getOrgId());
            row.put("orgName", item.getOrgName());
            row.put("trxName", item.getTrxName());
            row.put("ioType", item.getIoType());
            row.put("bizAppId", item.getBizAppId());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("요청처리APP", columns, rows);
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
}

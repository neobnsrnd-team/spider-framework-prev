package com.example.spideradmin.domain.gateway.service;

import com.example.spideradmin.domain.gateway.dto.GatewayDetailResponse;
import com.example.spideradmin.domain.gateway.dto.GatewayResponse;
import com.example.spideradmin.domain.gateway.dto.GatewayUpsertRequest;
import com.example.spideradmin.domain.gateway.dto.GatewayWithSystemsRequest;
import com.example.spideradmin.domain.gateway.mapper.GatewayMapper;
import com.example.spideradmin.domain.gwsystem.dto.SystemBatchRequest;
import com.example.spideradmin.domain.gwsystem.dto.SystemResponse;
import com.example.spideradmin.domain.gwsystem.mapper.SystemMapper;
import com.example.spideradmin.domain.gwsystem.service.GwSystemService;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
import com.example.spideradmin.global.util.ValidationUtils;
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
public class GatewayService {

    private final GatewayMapper gatewayMapper;
    private final SystemMapper systemMapper;

    private final GwSystemService systemService;

    public PageResponse<GatewayResponse> searchGateways(
            PageRequest pageRequest, String searchField, String searchValue, String ioType) {
        int page = Math.max(pageRequest.getPage(), 0);
        int size = Math.max(pageRequest.getSize(), 1);
        int offset = page * size;

        String keyword = (searchValue == null || searchValue.isBlank()) ? null : searchValue.trim();
        String gwId = null;
        String gwName = null;

        if ("gwName".equals(searchField)) {
            gwName = keyword;
        } else if ("gwId".equals(searchField)) {
            gwId = keyword;
        }

        List<GatewayResponse> gateways = gatewayMapper.findBySearchPaging(
                gwId, gwName, ioType, offset, size, pageRequest.getSortBy(), pageRequest.getSortDirection());
        long total = gatewayMapper.countBySearch(gwId, gwName, ioType);

        List<GatewayResponse> content = gateways == null ? new ArrayList<>() : gateways;

        int totalPages = (int) Math.ceil((double) total / size);

        return PageResponse.<GatewayResponse>builder()
                .content(content)
                .currentPage(page + 1)
                .totalPages(totalPages)
                .totalElements(total)
                .size(size)
                .hasNext(page + 1 < totalPages)
                .hasPrevious(page > 0)
                .build();
    }

    public byte[] exportGateways(
            String searchField, String searchValue, String ioType, String sortBy, String sortDirection) {
        String keyword = (searchValue == null || searchValue.isBlank()) ? null : searchValue.trim();
        String gwId = null;
        String gwName = null;

        if ("gwName".equals(searchField)) {
            gwName = keyword;
        } else if ("gwId".equals(searchField)) {
            gwId = keyword;
        }

        List<GatewayResponse> data = gatewayMapper.findAllForExport(gwId, gwName, ioType, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("GATEWAY", 15, "gwId"),
                new ExcelColumnDefinition("G/W 설명", 25, "gwDesc"),
                new ExcelColumnDefinition("G/W 구현 APP명", 20, "gwAppName"),
                new ExcelColumnDefinition("어댑터/리스너", 15, "ioType"),
                new ExcelColumnDefinition("THREAD", 12, "threadCount"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("gwId", item.getGwId());
            row.put("gwDesc", item.getGwDesc());
            row.put("gwAppName", item.getGwAppName());
            String ioTypeLabel = "I".equals(item.getIoType()) ? "리스너(수신)" : "어댑터(송신)";
            row.put("ioType", ioTypeLabel);
            row.put("threadCount", item.getThreadCount());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("연계기관", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    public GatewayDetailResponse getGatewayDetail(String gwId) {
        GatewayResponse gatewayResponse = gatewayMapper.selectResponseById(gwId);
        if (gatewayResponse == null) {
            throw new NotFoundException("gwId=" + gwId);
        }
        List<SystemResponse> systems = systemMapper.findByGateway(gwId);
        return GatewayDetailResponse.builder()
                .gateway(gatewayResponse)
                .systems(systems)
                .build();
    }

    private void validateGateway(GatewayUpsertRequest dto) {
        if (dto == null) {
            throw new InvalidInputException("Gateway 정보가 없습니다.");
        }
        if (!ValidationUtils.isValidAdapterListenerIoType(dto.getIoType())) {
            throw new InvalidInputException("송/수신 구분 값이 올바르지 않습니다.");
        }
        if (dto.getThreadCount() == null || dto.getThreadCount() < 1 || dto.getThreadCount() > 999) {
            throw new InvalidInputException("THREAD 수는 1~999 범위여야 합니다.");
        }
    }

    @Transactional
    public void saveGatewayWithSystems(GatewayWithSystemsRequest request) {
        if (request == null) {
            throw new InvalidInputException("요청 정보가 없습니다.");
        }

        GatewayUpsertRequest gatewayDto = request.getGateway();
        if (gatewayDto == null) {
            throw new InvalidInputException("Gateway 정보는 필수입니다.");
        }

        validateGateway(gatewayDto);
        boolean exists = gatewayMapper.countByGwId(gatewayDto.getGwId()) > 0;
        if (!exists) {
            gatewayMapper.insertGateway(gatewayDto);
        } else {
            gatewayMapper.updateGateway(gatewayDto);
        }

        SystemBatchRequest systemsDto = request.getSystems();
        if (systemsDto != null) {
            systemService.saveSystemBatch(gatewayDto.getGwId(), systemsDto);
        }
    }
}

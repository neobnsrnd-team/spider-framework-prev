package com.example.spideradmin.domain.monitor.service;

import com.example.spideradmin.domain.monitor.dto.MonitorCreateRequest;
import com.example.spideradmin.domain.monitor.dto.MonitorResponse;
import com.example.spideradmin.domain.monitor.dto.MonitorUpdateRequest;
import com.example.spideradmin.domain.monitor.mapper.MonitorMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
import com.example.spideradmin.global.util.SqlValidator;
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
public class MonitorService {

    private final MonitorMapper monitorMapper;

    public List<MonitorResponse> getAllMonitors(String sortBy, String sortDirection) {
        List<MonitorResponse> monitors;
        if (sortBy != null && !sortBy.isEmpty()) {
            monitors = monitorMapper.findAllWithSearch(null, null, null, sortBy, sortDirection, 0, Integer.MAX_VALUE);
        } else {
            monitors = monitorMapper.findAll();
        }
        return new ArrayList<>(monitors);
    }

    public MonitorResponse getMonitorById(String monitorId) {
        MonitorResponse monitor = monitorMapper.findById(monitorId);
        if (monitor == null) {
            throw new NotFoundException("monitorId: " + monitorId);
        }
        return monitor;
    }

    @Transactional
    public MonitorResponse createMonitor(MonitorCreateRequest dto) {
        if (monitorMapper.countByMonitorId(dto.getMonitorId()) > 0) {
            throw new DuplicateException("monitorId: " + dto.getMonitorId());
        }

        validateSqlQueries(dto.getMonitorQuery(), dto.getDetailQuery());

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        monitorMapper.insertMonitor(dto, now, userId);
        return monitorMapper.findById(dto.getMonitorId());
    }

    @Transactional
    public MonitorResponse updateMonitor(String monitorId, MonitorUpdateRequest dto) {
        if (monitorMapper.countByMonitorId(monitorId) == 0) {
            throw new NotFoundException("monitorId: " + monitorId);
        }

        String newMonitorId = dto.getMonitorId() != null ? dto.getMonitorId().trim() : null;
        String oldMonitorId = monitorId != null ? monitorId.trim() : null;
        boolean monitorIdChanged = newMonitorId != null && !newMonitorId.equals(oldMonitorId);

        if (monitorIdChanged && monitorMapper.countByMonitorId(newMonitorId) > 0) {
            throw new DuplicateException("monitorId: " + newMonitorId);
        }

        validateSqlQueries(dto.getMonitorQuery(), dto.getDetailQuery());

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        monitorMapper.updateMonitor(dto, oldMonitorId, monitorIdChanged, now, userId);
        String resultMonitorId = monitorIdChanged ? newMonitorId : oldMonitorId;
        return monitorMapper.findById(resultMonitorId);
    }

    @Transactional
    public void deleteMonitor(String monitorId) {
        int deletedCount = monitorMapper.deleteById(monitorId);
        if (deletedCount == 0) {
            throw new NotFoundException("monitorId: " + monitorId);
        }
    }

    public long getTotalCount() {
        return monitorMapper.countAll();
    }

    public long getActiveCount() {
        return monitorMapper.countActive();
    }

    public PageResponse<MonitorResponse> getMonitorsWithPagination(
            PageRequest pageRequest, String searchField, String searchValue) {
        long total = monitorMapper.countAllWithSearch(searchField, searchValue, null);

        List<MonitorResponse> monitors = monitorMapper.findAllWithSearch(
                searchField,
                searchValue,
                null,
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(monitors, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public byte[] exportMonitors(String searchField, String searchValue, String sortBy, String sortDirection) {
        List<MonitorResponse> data = monitorMapper.findAllWithSearch(
                searchField, searchValue, null, sortBy, sortDirection, 0, Integer.MAX_VALUE);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("모니터ID", 15, "monitorId"),
                new ExcelColumnDefinition("모니터명", 20, "monitorName"),
                new ExcelColumnDefinition("모니터쿼리", 40, "monitorQuery"),
                new ExcelColumnDefinition("새로고침주기", 12, "refreshTerm"),
                new ExcelColumnDefinition("사용여부", 10, "useYn"),
                new ExcelColumnDefinition("경고조건", 20, "alertCondition"),
                new ExcelColumnDefinition("경고메세지", 20, "alertMessage"),
                new ExcelColumnDefinition("수정일시", 18, "lastUpdateDtime"),
                new ExcelColumnDefinition("수정자", 12, "lastUpdateUserId"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (MonitorResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("monitorId", item.getMonitorId());
            row.put("monitorName", item.getMonitorName());
            row.put("monitorQuery", item.getMonitorQuery());
            row.put("refreshTerm", item.getRefreshTerm());
            row.put("useYn", item.getUseYn());
            row.put("alertCondition", item.getAlertCondition());
            row.put("alertMessage", item.getAlertMessage());
            row.put("lastUpdateDtime", item.getLastUpdateDtime());
            row.put("lastUpdateUserId", item.getLastUpdateUserId());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("현황판", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    private void validateSqlQueries(String monitorQuery, String detailQuery) {
        if (monitorQuery != null && !monitorQuery.trim().isEmpty()) {
            try {
                SqlValidator.validateQuery(monitorQuery);
            } catch (IllegalArgumentException e) {
                throw new InvalidInputException("monitorQuery - " + e.getMessage());
            }
        }

        if (detailQuery != null && !detailQuery.trim().isEmpty()) {
            try {
                SqlValidator.validateQuery(detailQuery);
            } catch (IllegalArgumentException e) {
                throw new InvalidInputException("detailQuery - " + e.getMessage());
            }
        }
    }
}

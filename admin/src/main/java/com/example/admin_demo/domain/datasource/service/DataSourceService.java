package com.example.admin_demo.domain.datasource.service;

import com.example.admin_demo.domain.datasource.dto.DataSourceCreateRequest;
import com.example.admin_demo.domain.datasource.dto.DataSourceResponse;
import com.example.admin_demo.domain.datasource.dto.DataSourceSearchRequest;
import com.example.admin_demo.domain.datasource.dto.DataSourceUpdateRequest;
import com.example.admin_demo.domain.datasource.mapper.DataSourceMapper;
import com.example.admin_demo.global.aop.WorkListRecord;
import com.example.admin_demo.global.dto.PageRequest;
import com.example.admin_demo.global.dto.PageResponse;
import com.example.admin_demo.global.exception.DuplicateException;
import com.example.admin_demo.global.exception.InternalException;
import com.example.admin_demo.global.exception.InvalidInputException;
import com.example.admin_demo.global.exception.NotFoundException;
import com.example.admin_demo.global.util.AuditUtil;
import com.example.admin_demo.global.util.ExcelColumnDefinition;
import com.example.admin_demo.global.util.ExcelExportUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 데이터소스 관리 Service
 *
 * <p>DB_PASSWORD 처리 정책:
 * <ul>
 *   <li>저장: 입력값 그대로 저장 (FWK_SQL_CONF.DB_PASSWORD 는 SpiderLink 실제 연결에 사용되므로 복호화 가능해야 함)
 *   <li>조회: Response DTO 에서 항상 "****" 로 마스킹 반환
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataSourceService {

    private static final String RESPONSE_MASK = "****";

    private final DataSourceMapper dataSourceMapper;

    public PageResponse<DataSourceResponse> getDataSourcesWithSearch(DataSourceSearchRequest searchDTO) {
        PageRequest pageRequest = searchDTO.toPageRequest();

        long total = dataSourceMapper.countAllWithSearch(
                searchDTO.getSearchField(), searchDTO.getSearchValue(), searchDTO.getJndiYnFilter());

        List<DataSourceResponse> list = dataSourceMapper.findAllWithSearch(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getJndiYnFilter(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        list.forEach(this::maskPassword);
        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public DataSourceResponse getById(String dbId) {
        DataSourceResponse response = dataSourceMapper.selectResponseById(dbId);
        if (response == null) {
            throw new NotFoundException("dbId: " + dbId);
        }
        maskPassword(response);
        return response;
    }

    @Transactional
    @WorkListRecord(workId = "SQL_CONF", crudType = "C", pkExpression = "#dto.dbId", workName = "데이터소스관리")
    public DataSourceResponse create(DataSourceCreateRequest dto) {
        try {
            dataSourceMapper.insert(dto, AuditUtil.now(), AuditUtil.currentUserId());
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("dbId: " + dto.getDbId());
        }
        DataSourceResponse response = dataSourceMapper.selectResponseById(dto.getDbId());
        if (response == null) {
            throw new InternalException("데이터소스 등록 후 조회 실패: " + dto.getDbId());
        }
        maskPassword(response);
        return response;
    }

    @Transactional
    @WorkListRecord(workId = "SQL_CONF", crudType = "U", pkExpression = "#dbId", workName = "데이터소스관리")
    public DataSourceResponse update(String dbId, DataSourceUpdateRequest dto) {
        if (dataSourceMapper.countByDbId(dbId) == 0) {
            throw new NotFoundException("dbId: " + dbId);
        }
        dataSourceMapper.update(dbId, dto, AuditUtil.now(), AuditUtil.currentUserId());
        DataSourceResponse response = dataSourceMapper.selectResponseById(dbId);
        maskPassword(response);
        return response;
    }

    public byte[] exportDataSources(
            String searchField, String searchValue, String jndiYnFilter, String sortBy, String sortDirection) {
        List<DataSourceResponse> data =
                dataSourceMapper.findAllForExport(searchField, searchValue, jndiYnFilter, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("DB ID", 20, "dbId"),
                new ExcelColumnDefinition("DB 명", 25, "dbName"),
                new ExcelColumnDefinition("접속 URL", 50, "connectionUrl"),
                new ExcelColumnDefinition("드라이버 클래스", 40, "driverClass"),
                new ExcelColumnDefinition("DB 사용자 ID", 20, "dbUserId"),
                new ExcelColumnDefinition("JNDI 여부", 10, "jndiYn"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (DataSourceResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dbId", item.getDbId());
            row.put("dbName", item.getDbName());
            row.put("connectionUrl", item.getConnectionUrl());
            row.put("driverClass", item.getDriverClass());
            row.put("dbUserId", item.getDbUserId());
            row.put("jndiYn", item.getJndiYn());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("데이터소스", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    @Transactional
    @WorkListRecord(workId = "SQL_CONF", crudType = "D", pkExpression = "#dbId", workName = "데이터소스관리")
    public void delete(String dbId) {
        if (dataSourceMapper.countByDbId(dbId) == 0) {
            throw new NotFoundException("dbId: " + dbId);
        }
        dataSourceMapper.deleteById(dbId);
    }

    private void maskPassword(DataSourceResponse response) {
        if (response != null) {
            response.setDbPassword(RESPONSE_MASK);
        }
    }
}

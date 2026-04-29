package com.example.spider_admin.domain.orgcode.service;

import com.example.spider_admin.domain.orgcode.dto.OrgCodePopupResponse;
import com.example.spider_admin.domain.orgcode.dto.OrgCodeResponse;
import com.example.spider_admin.domain.orgcode.dto.OrgCodeRowRequest;
import com.example.spider_admin.domain.orgcode.dto.OrgCodeSaveRequest;
import com.example.spider_admin.domain.orgcode.dto.OrgCodeSearchRequest;
import com.example.spider_admin.domain.orgcode.mapper.OrgCodeMapper;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.util.ExcelColumnDefinition;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgCodeService {

    private static final Set<String> ALLOWED_SORT_COLUMNS =
            Set.of("codeGroupId", "codeGroupName", "code", "codeName", "orgCode", "priority", "orgId");

    private final OrgCodeMapper orgCodeMapper;

    public PageResponse<OrgCodeResponse> search(OrgCodeSearchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 1;
        int size = request.getSize() != null ? request.getSize() : 20;
        int offset = (page - 1) * size;
        int endRow = offset + size;

        long total = orgCodeMapper.countByCondition(request.getOrgId(), request.getCodeGroupId());
        List<OrgCodeResponse> list =
                orgCodeMapper.findAllByCondition(request.getOrgId(), request.getCodeGroupId(), offset, endRow);
        return PageResponse.of(list, total, page - 1, size);
    }

    public byte[] exportOrgCodes(String orgId, String codeGroupId, String sortBy, String sortDirection) {
        if (sortBy != null && !ALLOWED_SORT_COLUMNS.contains(sortBy)) {
            log.warn("Invalid sortBy value rejected: {}", sortBy);
            sortBy = null;
        }
        List<OrgCodeResponse> data = orgCodeMapper.findAllForExport(orgId, codeGroupId, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("코드그룹ID", 15, "codeGroupId"),
                new ExcelColumnDefinition("코드그룹명", 20, "codeGroupName"),
                new ExcelColumnDefinition("표준코드", 15, "code"),
                new ExcelColumnDefinition("표준코드명", 20, "codeName"),
                new ExcelColumnDefinition("기관코드", 15, "orgCode"),
                new ExcelColumnDefinition("우선순위", 10, "priority"),
                new ExcelColumnDefinition("기관", 15, "orgName"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (OrgCodeResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("codeGroupId", item.getCodeGroupId());
            row.put("codeGroupName", item.getCodeGroupName());
            row.put("code", item.getCode());
            row.put("codeName", item.getCodeName());
            row.put("orgCode", item.getOrgCode());
            row.put("priority", item.getPriority());
            row.put("orgName", item.getOrgName() != null ? item.getOrgName() : item.getOrgId());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("코드맵핑", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    public List<OrgCodePopupResponse> getPopupData(String codeGroupId, String orgId) {
        return orgCodeMapper.findPopupData(codeGroupId, orgId);
    }

    @Transactional
    public void saveAll(OrgCodeSaveRequest request) {
        String orgId = request.getOrgId();
        String codeGroupId = request.getCodeGroupId();

        for (OrgCodeRowRequest row : request.getRows()) {
            String crudType = row.getCrudType();
            if (crudType == null) {
                throw new InvalidInputException("crudType: " + "null");
            }

            switch (crudType.toUpperCase()) {
                case "C" -> {
                    int count = orgCodeMapper.countByPk(orgId, codeGroupId, row.getCode(), row.getOrgCode());
                    if (count > 0) {
                        throw new DuplicateException("orgId: " + orgId + ", codeGroupId: " + codeGroupId + ", code: "
                                + row.getCode() + ", orgCode: " + row.getOrgCode());
                    }
                    orgCodeMapper.insert(row, orgId, codeGroupId);
                }
                case "U" -> {
                    // U타입: orgCode가 PK 일부 → 기존 삭제 후 새로 삽입
                    String oldOrgCode = row.getOldOrgCode() != null ? row.getOldOrgCode() : row.getOrgCode();
                    orgCodeMapper.delete(orgId, codeGroupId, row.getCode(), oldOrgCode);
                    orgCodeMapper.insert(row, orgId, codeGroupId);
                }
                case "D" -> {
                    String delOrgCode = row.getOldOrgCode() != null ? row.getOldOrgCode() : row.getOrgCode();
                    orgCodeMapper.delete(orgId, codeGroupId, row.getCode(), delOrgCode);
                }
                default -> throw new InvalidInputException("crudType: " + crudType);
            }
        }
    }
}

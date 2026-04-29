package com.example.spider_admin.domain.codegroup.service;

import com.example.spider_admin.domain.code.dto.CodeCreateRequest;
import com.example.spider_admin.domain.code.mapper.CodeMapper;
import com.example.spider_admin.domain.codegroup.dto.CodeGroupCreateRequest;
import com.example.spider_admin.domain.codegroup.dto.CodeGroupResponse;
import com.example.spider_admin.domain.codegroup.dto.CodeGroupUpdateRequest;
import com.example.spider_admin.domain.codegroup.dto.CodeGroupWithCodesResponse;
import com.example.spider_admin.domain.codegroup.mapper.CodeGroupMapper;
import com.example.spider_admin.global.aop.WorkListRecord;
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
public class CodeGroupService {

    private final CodeGroupMapper codeGroupMapper;
    private final CodeMapper codeMapper;

    public List<CodeGroupResponse> getAllCodeGroupsWithCodeCount() {
        return codeGroupMapper.findAllWithCodeCount();
    }

    public PageResponse<CodeGroupResponse> getCodeGroups(PageRequest pageRequest) {
        long total = codeGroupMapper.countAllWithCodeCountAndSearch(
                pageRequest.getSearchField(), pageRequest.getSearchValue());
        List<CodeGroupResponse> codeGroups = codeGroupMapper.findAllWithCodeCountAndSearch(
                pageRequest.getSearchField(),
                pageRequest.getSearchValue(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());
        return PageResponse.of(codeGroups, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public CodeGroupWithCodesResponse getCodeGroupWithCodes(String codeGroupId) {
        CodeGroupWithCodesResponse result = codeGroupMapper.findByIdWithCodes(codeGroupId);
        if (result == null) {
            throw new NotFoundException("codeGroupId: " + codeGroupId);
        }
        return result;
    }

    public List<CodeGroupResponse> getCodeGroupsByBizGroupId(String bizGroupId) {
        return codeGroupMapper.findByBizGroupId(bizGroupId);
    }

    @Transactional
    @WorkListRecord(workId = "Codegroup", crudType = "C", pkExpression = "#dto.codeGroupId", workName = "코드그룹")
    public CodeGroupWithCodesResponse createCodeGroupWithCodes(CodeGroupCreateRequest dto) {
        if (codeGroupMapper.countByCodeGroupId(dto.getCodeGroupId()) > 0) {
            throw new DuplicateException("codeGroupId: " + dto.getCodeGroupId());
        }

        if (codeGroupMapper.countByCodeGroupName(dto.getCodeGroupName()) > 0) {
            throw new DuplicateException("codeGroupName: " + dto.getCodeGroupName());
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        codeGroupMapper.insert(dto, now, currentUserId);

        if (dto.getCodes() != null) {
            for (CodeCreateRequest codeDto : dto.getCodes()) {
                codeDto.setCodeGroupId(dto.getCodeGroupId());
                codeMapper.insert(codeDto, now, currentUserId);
            }
        }

        return codeGroupMapper.findByIdWithCodes(dto.getCodeGroupId());
    }

    @Transactional
    @WorkListRecord(workId = "Codegroup", crudType = "U", pkExpression = "#codeGroupId", workName = "코드그룹")
    public CodeGroupWithCodesResponse updateCodeGroupWithCodes(String codeGroupId, CodeGroupUpdateRequest dto) {
        CodeGroupResponse existing = codeGroupMapper.selectResponseById(codeGroupId);
        if (existing == null) {
            throw new NotFoundException("codeGroupId: " + codeGroupId);
        }

        if (!existing.getCodeGroupName().equals(dto.getCodeGroupName())
                && codeGroupMapper.countByCodeGroupName(dto.getCodeGroupName()) > 0) {
            throw new DuplicateException("codeGroupName: " + dto.getCodeGroupName());
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        codeGroupMapper.update(codeGroupId, dto, now, currentUserId);

        codeMapper.deleteByCodeGroupId(codeGroupId);
        if (dto.getCodes() != null) {
            for (CodeCreateRequest codeDto : dto.getCodes()) {
                codeDto.setCodeGroupId(codeGroupId);
                codeMapper.insert(codeDto, now, currentUserId);
            }
        }

        return codeGroupMapper.findByIdWithCodes(codeGroupId);
    }

    @Transactional
    @WorkListRecord(workId = "Codegroup", crudType = "D", pkExpression = "#codeGroupId", workName = "코드그룹")
    public void deleteCodeGroupWithCodes(String codeGroupId) {
        if (codeGroupMapper.countByCodeGroupId(codeGroupId) == 0) {
            throw new NotFoundException("codeGroupId: " + codeGroupId);
        }

        codeMapper.deleteByCodeGroupId(codeGroupId);
        codeGroupMapper.deleteById(codeGroupId);
    }

    public byte[] exportCodeGroups(String searchField, String searchValue, String sortBy, String sortDirection) {
        List<CodeGroupResponse> data =
                codeGroupMapper.findAllForExport(searchField, searchValue, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("코드그룹ID", 15, "codeGroupId"),
                new ExcelColumnDefinition("코드그룹명", 25, "codeGroupName"),
                new ExcelColumnDefinition("코드설명", 30, "codeGroupDesc"),
                new ExcelColumnDefinition("업무분류", 15, "bizGroupId"),
                new ExcelColumnDefinition("속한코드수", 10, "codeCount"),
                new ExcelColumnDefinition("최종 수정 일시", 20, "lastUpdateDtime"),
                new ExcelColumnDefinition("최종 수정자", 15, "lastUpdateUserId"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (CodeGroupResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("codeGroupId", item.getCodeGroupId());
            row.put("codeGroupName", item.getCodeGroupName());
            row.put("codeGroupDesc", item.getCodeGroupDesc());
            row.put("bizGroupId", item.getBizGroupId());
            row.put("codeCount", item.getCodeCount());
            row.put("lastUpdateDtime", item.getLastUpdateDtime());
            row.put("lastUpdateUserId", item.getLastUpdateUserId());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("코드 그룹", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }
}

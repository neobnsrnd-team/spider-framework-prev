package com.example.spideradmin.domain.code.service;

import com.example.spideradmin.domain.code.dto.CodeCreateRequest;
import com.example.spideradmin.domain.code.dto.CodeIdRequest;
import com.example.spideradmin.domain.code.dto.CodeResponse;
import com.example.spideradmin.domain.code.dto.CodeUpdateRequest;
import com.example.spideradmin.domain.code.dto.CodeWithGroupResponse;
import com.example.spideradmin.domain.code.mapper.CodeMapper;
import com.example.spideradmin.domain.codegroup.mapper.CodeGroupMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
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
public class CodeService {

    private final CodeMapper codeMapper;
    private final CodeGroupMapper codeGroupMapper;

    public PageResponse<CodeWithGroupResponse> getCodesWithGroup(PageRequest pageRequest, String codeGroupId) {
        long total =
                codeMapper.countAllWithGroup(codeGroupId, pageRequest.getSearchField(), pageRequest.getSearchValue());
        List<CodeWithGroupResponse> codes = codeMapper.findAllWithGroup(
                codeGroupId,
                pageRequest.getSearchField(),
                pageRequest.getSearchValue(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());
        return PageResponse.of(codes, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public CodeWithGroupResponse getCodeById(String codeGroupId, String code) {
        CodeWithGroupResponse result = codeMapper.findByIdWithGroup(codeGroupId, code);
        if (result == null) {
            throw new NotFoundException("codeGroupId: " + codeGroupId + ", code: " + code);
        }
        return result;
    }

    public List<CodeResponse> getCodesByCodeGroupId(String codeGroupId) {
        return codeMapper.findByCodeGroupId(codeGroupId);
    }

    @Transactional
    public CodeResponse createCode(CodeCreateRequest dto) {
        if (codeGroupMapper.countByCodeGroupId(dto.getCodeGroupId()) == 0) {
            throw new NotFoundException("codeGroupId: " + dto.getCodeGroupId());
        }

        if (codeMapper.countByCodeGroupIdAndCode(dto.getCodeGroupId(), dto.getCode()) > 0) {
            throw new DuplicateException("codeGroupId: " + dto.getCodeGroupId() + ", code: " + dto.getCode());
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        codeMapper.insert(dto, now, currentUserId);
        return codeMapper.selectResponseById(dto.getCodeGroupId(), dto.getCode());
    }

    @Transactional
    public CodeResponse updateCode(String codeGroupId, String code, CodeUpdateRequest dto) {
        if (codeMapper.countByCodeGroupIdAndCode(codeGroupId, code) == 0) {
            throw new NotFoundException("codeGroupId: " + codeGroupId + ", code: " + code);
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        codeMapper.update(codeGroupId, code, dto, now, currentUserId);
        return codeMapper.selectResponseById(codeGroupId, code);
    }

    @Transactional
    public void deleteCode(String codeGroupId, String code) {
        if (codeMapper.countByCodeGroupIdAndCode(codeGroupId, code) == 0) {
            throw new NotFoundException("codeGroupId: " + codeGroupId + ", code: " + code);
        }
        codeMapper.deleteById(codeGroupId, code);
    }

    @Transactional
    public void deleteMultipleCodes(List<CodeIdRequest> codeIds) {
        for (CodeIdRequest codeId : codeIds) {
            codeMapper.deleteById(codeId.getCodeGroupId(), codeId.getCode());
        }
    }

    public long countByCodeGroupId(String codeGroupId) {
        return codeMapper.countByCodeGroupId(codeGroupId);
    }

    public byte[] exportCodes(
            String codeGroupId, String searchField, String searchValue, String sortBy, String sortDirection) {
        List<CodeWithGroupResponse> data =
                codeMapper.findAllForExport(codeGroupId, searchField, searchValue, sortBy, sortDirection);
        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }
        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("코드그룹", 15, "codeGroupId"),
                new ExcelColumnDefinition("코드그룹명", 20, "codeGroupName"),
                new ExcelColumnDefinition("코드", 12, "code"),
                new ExcelColumnDefinition("코드명", 20, "codeName"),
                new ExcelColumnDefinition("코드설명", 30, "codeDesc"),
                new ExcelColumnDefinition("정렬순서", 10, "sortOrder"),
                new ExcelColumnDefinition("사용", 8, "useYn"),
                new ExcelColumnDefinition("영문명", 15, "codeEngname"),
                new ExcelColumnDefinition("수정일시", 18, "lastUpdateDtime"),
                new ExcelColumnDefinition("수정자", 12, "lastUpdateUserId"));
        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (CodeWithGroupResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("codeGroupId", item.getCodeGroupId());
            row.put("codeGroupName", item.getCodeGroupName());
            row.put("code", item.getCode());
            row.put("codeName", item.getCodeName());
            row.put("codeDesc", item.getCodeDesc());
            row.put("sortOrder", item.getSortOrder());
            row.put("useYn", item.getUseYn());
            row.put("codeEngname", item.getCodeEngname());
            row.put("lastUpdateDtime", item.getLastUpdateDtime());
            row.put("lastUpdateUserId", item.getLastUpdateUserId());
            rows.add(row);
        }
        try {
            return ExcelExportUtil.createWorkbook("코드", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }
}

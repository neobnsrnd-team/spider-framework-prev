package com.example.spider_admin.domain.validation.service;

import com.example.spider_admin.domain.validation.dto.ValidationCreateRequest;
import com.example.spider_admin.domain.validation.dto.ValidationResponse;
import com.example.spider_admin.domain.validation.dto.ValidationSearchRequest;
import com.example.spider_admin.domain.validation.dto.ValidationUpdateRequest;
import com.example.spider_admin.domain.validation.mapper.ValidationMapper;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Validation 관리 Service */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidationService {

    private final ValidationMapper validationMapper;

    public PageResponse<ValidationResponse> getValidationsWithSearch(ValidationSearchRequest searchDTO) {
        PageRequest pageRequest = searchDTO.toPageRequest();

        long total = validationMapper.countAllWithSearch(searchDTO.getValidationId(), searchDTO.getValidationDesc());

        List<ValidationResponse> validations = validationMapper.findAllWithSearch(
                searchDTO.getValidationId(),
                searchDTO.getValidationDesc(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(validations, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public ValidationResponse getById(String validationId) {
        ValidationResponse response = validationMapper.selectResponseById(validationId);
        if (response == null) {
            throw new NotFoundException("validationId: " + validationId);
        }
        return response;
    }

    @Transactional
    public ValidationResponse create(ValidationCreateRequest dto) {
        try {
            validationMapper.insert(dto, AuditUtil.now(), AuditUtil.currentUserId());
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("validationId: " + dto.getValidationId());
        }
        return validationMapper.selectResponseById(dto.getValidationId());
    }

    @Transactional
    public ValidationResponse update(String validationId, ValidationUpdateRequest dto) {
        if (validationMapper.countById(validationId) == 0) {
            throw new NotFoundException("validationId: " + validationId);
        }
        validationMapper.update(validationId, dto, AuditUtil.now(), AuditUtil.currentUserId());
        return validationMapper.selectResponseById(validationId);
    }

    @Transactional
    public void delete(String validationId) {
        if (validationMapper.countById(validationId) == 0) {
            throw new NotFoundException("validationId: " + validationId);
        }
        validationMapper.deleteById(validationId);
    }

    public byte[] exportValidations(String validationId, String validationDesc, String sortBy, String sortDirection) {
        List<ValidationResponse> data =
                validationMapper.findAllForExport(validationId, validationDesc, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("Validation ID", 20, "validationId"),
                new ExcelColumnDefinition("Validation 설명", 30, "validationDesc"),
                new ExcelColumnDefinition("Field Event Text", 30, "fieldEventText"),
                new ExcelColumnDefinition("Java Class Name", 40, "javaClassName"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("validationId", item.getValidationId());
            row.put("validationDesc", item.getValidationDesc());
            row.put("fieldEventText", item.getFieldEventText());
            row.put("javaClassName", item.getJavaClassName());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("Validation", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }
}

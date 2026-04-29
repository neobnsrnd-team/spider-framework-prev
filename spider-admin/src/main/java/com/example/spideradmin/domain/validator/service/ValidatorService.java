package com.example.spideradmin.domain.validator.service;

import com.example.spideradmin.domain.validator.dto.ValidatorCreateRequest;
import com.example.spideradmin.domain.validator.dto.ValidatorResponse;
import com.example.spideradmin.domain.validator.dto.ValidatorSearchRequest;
import com.example.spideradmin.domain.validator.dto.ValidatorUpdateRequest;
import com.example.spideradmin.domain.validator.mapper.ValidatorMapper;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validator 관리 Service 구현체
 *
 * - 조회 로직 (목록, 검색) → ValidatorMapper
 * - CRUD 로직 (등록, 수정, 삭제) → ValidatorMapper
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidatorService {

    private final ValidatorMapper validatorMapper;

    public PageResponse<ValidatorResponse> getValidatorsWithSearch(ValidatorSearchRequest searchDTO) {
        PageRequest pageRequest = searchDTO.toPageRequest();

        long total = validatorMapper.countAllWithSearch(
                searchDTO.getValidatorId(),
                searchDTO.getValidatorName(),
                searchDTO.getBizDomain(),
                searchDTO.getUseYn());

        List<ValidatorResponse> validators = validatorMapper.findAllWithSearch(
                searchDTO.getValidatorId(),
                searchDTO.getValidatorName(),
                searchDTO.getBizDomain(),
                searchDTO.getUseYn(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(validators, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public ValidatorResponse getById(String validatorId) {
        ValidatorResponse response = validatorMapper.selectResponseById(validatorId);
        if (response == null) {
            throw new NotFoundException("validatorId: " + validatorId);
        }
        return response;
    }

    @Transactional
    public ValidatorResponse create(ValidatorCreateRequest dto) {
        try {
            validatorMapper.insert(dto, AuditUtil.now(), AuditUtil.currentUserId());
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("validatorId: " + dto.getValidatorId());
        }
        return validatorMapper.selectResponseById(dto.getValidatorId());
    }

    @Transactional
    public ValidatorResponse update(String validatorId, ValidatorUpdateRequest dto) {
        int count = validatorMapper.countById(validatorId);
        if (count == 0) {
            throw new NotFoundException("validatorId: " + validatorId);
        }

        validatorMapper.update(validatorId, dto, AuditUtil.now(), AuditUtil.currentUserId());
        return validatorMapper.selectResponseById(validatorId);
    }

    public byte[] exportValidators(
            String validatorId,
            String validatorName,
            String bizDomain,
            String useYn,
            String sortBy,
            String sortDirection) {
        List<ValidatorResponse> data =
                validatorMapper.findAllForExport(validatorId, validatorName, bizDomain, useYn, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("Validator ID", 20, "validatorId"),
                new ExcelColumnDefinition("Validator 명", 25, "validatorName"),
                new ExcelColumnDefinition("Validator Application 명", 40, "javaClassName"),
                new ExcelColumnDefinition("Site 구분", 15, "bizDomainName"),
                new ExcelColumnDefinition("사용여부", 8, "useYn"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("validatorId", item.getValidatorId());
            row.put("validatorName", item.getValidatorName());
            row.put("javaClassName", item.getJavaClassName());
            row.put("bizDomainName", item.getBizDomainName());
            row.put("useYn", item.getUseYn());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("거래Validator", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    @Transactional
    public void delete(String validatorId) {
        int count = validatorMapper.countById(validatorId);
        if (count == 0) {
            throw new NotFoundException("validatorId: " + validatorId);
        }
        validatorMapper.deleteById(validatorId);
    }
}

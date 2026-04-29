package com.example.spider_admin.domain.errorhistory.service;

import com.example.spider_admin.domain.errorcode.enums.ErrorLevel;
import com.example.spider_admin.domain.errorhistory.dto.ErrorHisResponse;
import com.example.spider_admin.domain.errorhistory.dto.ErrorHisSearchRequest;
import com.example.spider_admin.domain.errorhistory.mapper.ErrorHisMapper;
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

/**
 * 오류 발생 이력 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErrorHisService {

    private final ErrorHisMapper errorHisMapper;

    public PageResponse<ErrorHisResponse> getErrorHistories(ErrorHisSearchRequest searchDTO) {
        // searchDTO.getPage()는 1-based
        int page = searchDTO.getPage() != null ? searchDTO.getPage() : 1;
        int size = searchDTO.getSize() != null ? searchDTO.getSize() : 20;

        int offset = (page - 1) * size;
        int endRow = offset + size;

        long total = errorHisMapper.countWithErrorInfo(searchDTO);

        List<ErrorHisResponse> dtos = errorHisMapper.searchWithErrorInfo(searchDTO, offset, endRow);

        // errorLevelName 설정
        dtos.forEach(dto -> {
            if (dto.getErrorLevel() != null) {
                dto.setErrorLevelName(ErrorLevel.getDescriptionByCode(dto.getErrorLevel()));
            }
        });

        return PageResponse.of(dtos, total, page - 1, size);
    }

    public ErrorHisResponse getErrorHis(String errorCode, String errorSerNo) {
        ErrorHisResponse response = errorHisMapper.selectResponseById(errorCode, errorSerNo);
        if (response == null) {
            throw new NotFoundException("errorCode: " + errorCode + ", errorSerNo: " + errorSerNo);
        }
        return response;
    }

    public byte[] exportErrorHistories(ErrorHisSearchRequest searchDTO) {
        List<ErrorHisResponse> data = errorHisMapper.findAllForExport(searchDTO);

        data.forEach(dto -> {
            if (dto.getErrorLevel() != null) {
                dto.setErrorLevelName(ErrorLevel.getDescriptionByCode(dto.getErrorLevel()));
            }
        });

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("오류코드", 15, "errorCode"),
                new ExcelColumnDefinition("오류 발생 일련번호", 18, "errorSerNo"),
                new ExcelColumnDefinition("고객ID", 15, "custUserId"),
                new ExcelColumnDefinition("고객 전화번호", 15, "custPhoneNo"),
                new ExcelColumnDefinition("고객 출력 메세지", 40, "errorMessage"),
                new ExcelColumnDefinition("메뉴명", 30, "errorUrl"),
                new ExcelColumnDefinition("발생 인스턴스ID", 15, "errorInstanceId"),
                new ExcelColumnDefinition("오류발생일시", 18, "errorOccurDtime"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("errorCode", item.getErrorCode());
            row.put("errorSerNo", item.getErrorSerNo());
            row.put("custUserId", item.getCustUserId());
            row.put("custPhoneNo", item.getCustPhoneNo());
            row.put("errorMessage", item.getErrorMessage());
            row.put("errorUrl", item.getErrorUrl());
            row.put("errorInstanceId", item.getErrorInstanceId());
            row.put("errorOccurDtime", item.getErrorOccurDtime());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("오류발생현황", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }
}

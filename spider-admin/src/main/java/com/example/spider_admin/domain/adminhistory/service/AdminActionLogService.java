package com.example.spider_admin.domain.adminhistory.service;

import com.example.spider_admin.domain.adminhistory.dto.AdminActionLogResponse;
import com.example.spider_admin.domain.adminhistory.dto.AdminActionLogSearchRequest;
import com.example.spider_admin.domain.adminhistory.mapper.AdminActionLogMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.util.ExcelColumnDefinition;
import com.example.spider_admin.global.util.ExcelExportUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 작업이력 로그 비즈니스 로직 구현 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminActionLogService {

    private final AdminActionLogMapper adminActionLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 검색 조건과 페이징 정보를 기반으로 관리자 작업이력 로그를 조회합니다.
     */
    public PageResponse<AdminActionLogResponse> searchLogs(
            AdminActionLogSearchRequest searchDTO, PageRequest pageRequest) {

        long total = adminActionLogMapper.countSearchLogs(searchDTO);
        List<AdminActionLogResponse> logs =
                adminActionLogMapper.searchLogs(searchDTO, pageRequest.getOffset(), pageRequest.getEndRow());

        log.debug("Found {} logs (total: {})", logs.size(), total);

        return PageResponse.of(logs, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public byte[] exportLogs(AdminActionLogSearchRequest searchDTO) {
        List<AdminActionLogResponse> data = adminActionLogMapper.findAllForExport(searchDTO);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("사용자ID", 15, "userId"),
                new ExcelColumnDefinition("접근 일시", 20, "accessDtime"),
                new ExcelColumnDefinition("접근 IP", 20, "accessIp"),
                new ExcelColumnDefinition("접근 URL", 40, "accessUrl"),
                new ExcelColumnDefinition("입력 데이터", 50, "inputData"),
                new ExcelColumnDefinition("결과 메세지", 40, "resultMessage"));

        List<Map<String, Object>> rows = data.stream()
                .map(item -> objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {}))
                .toList();

        try {
            return ExcelExportUtil.createWorkbook("관리자 작업이력", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }
}

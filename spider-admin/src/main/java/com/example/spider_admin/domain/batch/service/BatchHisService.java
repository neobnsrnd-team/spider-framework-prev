package com.example.spider_admin.domain.batch.service;

import com.example.spider_admin.domain.batch.dto.BatchHisResponse;
import com.example.spider_admin.domain.batch.dto.BatchHisSearchRequest;
import com.example.spider_admin.domain.batch.mapper.BatchHisMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
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

/**
 * 배치 수행 이력 조회 Service 구현체
 * - QueryMapper에서 DTO 직접 반환 (팀 표준 패턴)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchHisService {

    private final BatchHisMapper batchHisMapper;

    public PageResponse<BatchHisResponse> searchBatchHistory(PageRequest pageRequest, BatchHisSearchRequest searchDTO) {
        long total = batchHisMapper.countAllWithSearch(searchDTO);

        List<BatchHisResponse> batchHisList =
                batchHisMapper.findAllWithSearch(searchDTO, pageRequest.getOffset(), pageRequest.getEndRow());

        return PageResponse.of(batchHisList, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public byte[] exportBatchHistory(BatchHisSearchRequest searchDTO) {
        List<BatchHisResponse> data = batchHisMapper.findAllForExport(searchDTO);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("배치 APP ID", 15, "batchAppId"),
                new ExcelColumnDefinition("인스턴스ID", 15, "instanceId"),
                new ExcelColumnDefinition("실행횟수", 15, "batchExecuteSeq"),
                new ExcelColumnDefinition("기준일", 15, "batchDate"),
                new ExcelColumnDefinition("시작일", 15, "logDtime"),
                new ExcelColumnDefinition("종료일", 15, "batchEndDtime"),
                new ExcelColumnDefinition("상태코드", 15, "resRtCode"),
                new ExcelColumnDefinition("수행자ID", 15, "lastUpdateUserId"),
                new ExcelColumnDefinition("총 처리건", 15, "recordCount"),
                new ExcelColumnDefinition("정상 처리건", 15, "successCount"),
                new ExcelColumnDefinition("오류건수", 15, "failCount"),
                new ExcelColumnDefinition("오류코드", 15, "errorCode"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("batchAppId", item.getBatchAppId());
            row.put("instanceId", item.getInstanceId());
            row.put("batchExecuteSeq", item.getBatchExecuteSeq());
            row.put("batchDate", item.getBatchDate());
            row.put("logDtime", item.getLogDtime());
            row.put("batchEndDtime", item.getBatchEndDtime());
            row.put("resRtCode", item.getResRtCode());
            row.put("lastUpdateUserId", item.getLastUpdateUserId());
            row.put("recordCount", item.getRecordCount());
            row.put("successCount", item.getSuccessCount());
            row.put("failCount", item.getFailCount());
            row.put("errorCode", item.getErrorCode());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("배치수행내역", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }
}

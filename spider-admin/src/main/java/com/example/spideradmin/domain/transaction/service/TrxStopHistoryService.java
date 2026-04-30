package com.example.spideradmin.domain.transaction.service;

import com.example.spideradmin.domain.transaction.dto.TrxStopHistorySearchRequest;
import com.example.spideradmin.domain.transaction.dto.TrxStopHistoryWithTrxNameResponse;
import com.example.spideradmin.domain.transaction.mapper.TrxStopHistoryMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 거래중지이력 조회 서비스 구현 클래스입니다.
 * <p>이 구현체는 {@link TrxStopHistoryService} 인터페이스를 구현하며,
 * 거래 및 서비스의 중지/재개 이력을 조회하는 기능을 제공합니다.
 * <p>이력 생성(INSERT)은 message 도메인의 TrxStopHistoryMapper가 담당합니다.
 *
 * @see TrxStopHistoryService
 * @see TrxStopHistoryMapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrxStopHistoryService {

    private final TrxStopHistoryMapper trxStopHistoryMapper;

    public PageResponse<TrxStopHistoryWithTrxNameResponse> searchHistories(
            TrxStopHistorySearchRequest searchDTO, PageRequest pageRequest) {
        log.info(
                "Searching TrxStopHistory: gubunType={}, trxId={}, dateRange=[{} ~ {}], sortBy={}, sortDirection={}, page={}, size={}",
                searchDTO.getGubunType(),
                searchDTO.getTrxId(),
                searchDTO.getStartDtime(),
                searchDTO.getEndDtime(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getPage(),
                pageRequest.getSize());

        // 검색 조건 카운트 조회
        long total = trxStopHistoryMapper.countSearchHistories(searchDTO);

        // 네이티브 ROWNUM 페이징 조회
        List<TrxStopHistoryWithTrxNameResponse> histories = trxStopHistoryMapper.searchHistories(
                searchDTO,
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        log.info("Found {} histories (total: {})", histories.size(), total);

        return PageResponse.of(histories, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public List<TrxStopHistoryWithTrxNameResponse> findByTrxId(String trxId) {
        log.info("Finding TrxStopHistory by trxId: {}", trxId);

        List<TrxStopHistoryWithTrxNameResponse> histories = trxStopHistoryMapper.findByTrxId(trxId);

        log.info("Found {} histories for trxId: {}", histories.size(), trxId);

        return histories;
    }

    public byte[] exportExcel(TrxStopHistorySearchRequest searchDTO) {
        log.info("Exporting TrxStopHistory to Excel");

        List<TrxStopHistoryWithTrxNameResponse> data = trxStopHistoryMapper.findAllForExport(searchDTO);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("구분유형", 10, "gubunType"),
                new ExcelColumnDefinition("거래ID", 20, "trxId"),
                new ExcelColumnDefinition("거래명", 30, "trxName"),
                new ExcelColumnDefinition("중지/재개일시", 18, "trxStopUpdateDtime"),
                new ExcelColumnDefinition("중지사유", 40, "trxStopReason"),
                new ExcelColumnDefinition("거래중지여부", 12, "trxStopYn"),
                new ExcelColumnDefinition("최종수정자", 15, "lastUpdateUserId"));

        List<Map<String, Object>> rows = data.stream()
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("gubunType", item.getGubunType());
                    row.put("trxId", item.getTrxId());
                    row.put("trxName", item.getTrxName());
                    row.put("trxStopUpdateDtime", item.getTrxStopUpdateDtime());
                    row.put("trxStopReason", item.getTrxStopReason());
                    row.put("trxStopYn", item.getTrxStopYn());
                    row.put("lastUpdateUserId", item.getLastUpdateUserId());
                    return row;
                })
                .toList();

        log.info("Exporting {} TrxStopHistory records to Excel", rows.size());

        try {
            return ExcelExportUtil.createWorkbook("거래중지이력", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }
}

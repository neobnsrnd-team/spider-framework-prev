package com.example.spideradmin.domain.transaction.service;

import com.example.spideradmin.domain.transaction.dto.OperModeBatchRequest;
import com.example.spideradmin.domain.transaction.dto.TrxSimpleResponse;
import com.example.spideradmin.domain.transaction.dto.TrxStopBatchRequest;
import com.example.spideradmin.domain.transaction.dto.TrxStopListResponse;
import com.example.spideradmin.domain.transaction.dto.TrxStopSearchRequest;
import com.example.spideradmin.domain.transaction.mapper.TrxMapper;
import com.example.spideradmin.domain.transaction.mapper.TrxStopHistoryMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrxStopService {

    private static final Set<String> VALID_OPER_MODES = Set.of("D", "R", "T");

    private final TrxMapper trxMapper;
    private final TrxStopHistoryMapper trxStopHistoryMapper;

    public PageResponse<TrxStopListResponse> searchTrxStopList(
            PageRequest pageRequest, TrxStopSearchRequest searchDTO) {

        long total = trxMapper.countAllForTrxStop(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getOperModeTypeFilter(),
                searchDTO.getTrxStopYnFilter());

        List<TrxStopListResponse> list = trxMapper.findAllForTrxStop(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getOperModeTypeFilter(),
                searchDTO.getTrxStopYnFilter(),
                searchDTO.getSortBy(),
                searchDTO.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public byte[] exportTrxStop(
            String searchField,
            String searchValue,
            String operModeTypeFilter,
            String trxStopYnFilter,
            String sortBy,
            String sortDirection) {
        List<TrxStopListResponse> data = trxMapper.findAllForTrxStopExport(
                searchField, searchValue, operModeTypeFilter, trxStopYnFilter, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("거래ID", 15, "trxId"),
                new ExcelColumnDefinition("거래명", 25, "trxName"),
                new ExcelColumnDefinition("운영모드", 12, "operModeType"),
                new ExcelColumnDefinition("거래구분", 10, "trxType"),
                new ExcelColumnDefinition("재처리", 10, "retryTrxYn"),
                new ExcelColumnDefinition("상태", 10, "trxStopYn"),
                new ExcelColumnDefinition("중지사유", 30, "trxStopReason"),
                new ExcelColumnDefinition("접근허용자 수", 12, "accessUserCount"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (TrxStopListResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("trxId", item.getTrxId());
            row.put("trxName", item.getTrxName());
            row.put("operModeType", item.getOperModeType());
            row.put("trxType", item.getTrxType());
            row.put("retryTrxYn", item.getRetryTrxYn());
            row.put("trxStopYn", item.getTrxStopYn());
            row.put("trxStopReason", item.getTrxStopReason());
            row.put("accessUserCount", item.getAccessUserCount());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("거래중지", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    @Transactional
    public void batchUpdateTrxStop(TrxStopBatchRequest requestDTO) {
        List<String> trxIds = requestDTO.getTrxIds();
        String newStopYn = requestDTO.getTrxStopYn();

        // 1. IN 쿼리로 일괄 조회
        List<TrxSimpleResponse> trxList = trxMapper.selectSimpleByIds(trxIds);

        // 2. 존재하지 않는 ID 검증
        Map<String, TrxSimpleResponse> trxMap =
                trxList.stream().collect(Collectors.toMap(TrxSimpleResponse::getTrxId, Function.identity()));
        for (String trxId : trxIds) {
            if (!trxMap.containsKey(trxId)) {
                throw new NotFoundException("trxId: " + trxId);
            }
        }

        // 3. 상태 변경이 필요한 ID만 필터링
        List<String> targetIds = trxList.stream()
                .filter(trx -> !Objects.equals(trx.getTrxStopYn(), newStopYn))
                .map(TrxSimpleResponse::getTrxId)
                .toList();

        if (targetIds.isEmpty()) {
            return;
        }

        // 4. 일괄 UPDATE
        trxMapper.batchUpdateTrxStop(targetIds, newStopYn, requestDTO.getTrxStopReason());

        // 5. 이력 일괄 INSERT
        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();
        List<Map<String, String>> histories = new ArrayList<>();
        for (String trxId : targetIds) {
            Map<String, String> historyMap = new HashMap<>();
            historyMap.put("gubunType", "T");
            historyMap.put("trxId", trxId);
            historyMap.put("trxStopUpdateDtime", now);
            historyMap.put("trxStopYn", newStopYn);
            historyMap.put("trxStopReason", requestDTO.getTrxStopReason());
            historyMap.put("lastUpdateUserId", userId);
            histories.add(historyMap);
        }
        trxStopHistoryMapper.insertBatch(histories);

        log.debug("TrxStopYn batch changed to {} for {} transactions", newStopYn, targetIds.size());
    }

    @Transactional
    public void batchUpdateOperMode(OperModeBatchRequest requestDTO) {
        String newOperMode = requestDTO.getOperModeType();
        if (newOperMode != null && !VALID_OPER_MODES.contains(newOperMode)) {
            throw new InvalidInputException("operModeTypeCode: " + newOperMode);
        }
        trxMapper.updateAllOperMode(newOperMode);
        log.debug("All transactions OperModeType changed to: {}", newOperMode);
    }
}

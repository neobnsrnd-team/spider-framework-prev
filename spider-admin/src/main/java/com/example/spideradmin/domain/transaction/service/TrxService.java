package com.example.spideradmin.domain.transaction.service;

import com.example.spideradmin.domain.transaction.dto.TrxCreateRequest;
import com.example.spideradmin.domain.transaction.dto.TrxCreateResponse;
import com.example.spideradmin.domain.transaction.dto.TrxDetailResponse;
import com.example.spideradmin.domain.transaction.dto.TrxListResponse;
import com.example.spideradmin.domain.transaction.dto.TrxResponse;
import com.example.spideradmin.domain.transaction.dto.TrxSearchRequest;
import com.example.spideradmin.domain.transaction.dto.TrxSimpleResponse;
import com.example.spideradmin.domain.transaction.dto.TrxUpdateRequest;
import com.example.spideradmin.domain.transaction.dto.TrxWithMessagesResponse;
import com.example.spideradmin.domain.transaction.mapper.TrxHistoryMapper;
import com.example.spideradmin.domain.transaction.mapper.TrxMapper;
import com.example.spideradmin.domain.trxmessage.dto.TrxMessageResponse;
import com.example.spideradmin.global.aop.WorkListRecord;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
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
 * Service implementation for TRX management
 * Handles business logic for TRX operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrxService {

    private final TrxMapper trxMapper;
    private final TrxHistoryMapper trxHistoryMapper;

    /**
     * 거래 목록 조회 (페이징, 검색 포함)
     * FWKI0060 거래관리 화면용
     */
    public PageResponse<TrxListResponse> searchTrxWithPagination(PageRequest pageRequest, TrxSearchRequest searchDTO) {
        long total = trxMapper.countAllWithSearchAndMessages(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getTrxStopYnFilter(),
                searchDTO.getOrgIdFilter());

        List<TrxListResponse> trxList = trxMapper.findAllWithSearchAndMessages(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getTrxStopYnFilter(),
                searchDTO.getOrgIdFilter(),
                searchDTO.getSortBy(),
                searchDTO.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(trxList, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /**
     * 전체 거래 목록 조회 (select box용)
     * @return 간단한 TRX 목록 (trxId, trxName)
     */
    public List<TrxSimpleResponse> getAllTrxList() {
        return trxMapper.findAllSimple();
    }

    /**
     * Get TRX by ID
     */
    public TrxResponse getTrxById(String trxId) {
        TrxResponse trx = trxMapper.selectResponseById(trxId);
        if (trx == null) {
            throw new NotFoundException("trxId: " + trxId);
        }
        return trx;
    }

    /**
     * Create new TRX
     */
    @Transactional
    @WorkListRecord(workId = "Trx", crudType = "C", pkExpression = "#requestDTO.trxId", workName = "거래")
    public TrxCreateResponse createTrx(TrxCreateRequest requestDTO) {
        // 중복 체크
        int count = trxMapper.countByTrxId(requestDTO.getTrxId());
        if (count > 0) {
            throw new DuplicateException("trxId: " + requestDTO.getTrxId());
        }

        trxMapper.insertTrx(requestDTO);

        // 생성 후 조회하여 응답 반환
        TrxResponse created = trxMapper.selectResponseById(requestDTO.getTrxId());
        if (created == null) {
            throw new InternalException("거래 생성 후 조회 실패: trxId=" + requestDTO.getTrxId());
        }
        return TrxCreateResponse.builder()
                .trxId(created.getTrxId())
                .operModeType(created.getOperModeType())
                .trxStopYn(created.getTrxStopYn())
                .trxName(created.getTrxName())
                .trxDesc(created.getTrxDesc())
                .trxType(created.getTrxType())
                .retryTrxYn(created.getRetryTrxYn())
                .maxRetryCount(created.getMaxRetryCount())
                .retryMiCycle(created.getRetryMiCycle())
                .bizGroupId(created.getBizGroupId())
                .bizdayTrxYn(created.getBizdayTrxYn())
                .bizdayTrxStartTime(created.getBizdayTrxStartTime())
                .bizdayTrxEndTime(created.getBizdayTrxEndTime())
                .saturdayTrxYn(created.getSaturdayTrxYn())
                .saturdayTrxStartTime(created.getSaturdayTrxStartTime())
                .saturdayTrxEndTime(created.getSaturdayTrxEndTime())
                .holidayTrxYn(created.getHolidayTrxYn())
                .holidayTrxStartTime(created.getHolidayTrxStartTime())
                .holidayTrxEndTime(created.getHolidayTrxEndTime())
                .trxStopReason(created.getTrxStopReason())
                .build();
    }

    /**
     * Update TRX
     */
    @Transactional
    @WorkListRecord(workId = "Trx", crudType = "U", pkExpression = "#trxId", workName = "거래")
    public TrxResponse updateTrx(String trxId, TrxUpdateRequest dto) {
        int affected = trxMapper.updateTrx(trxId, dto);
        if (affected == 0) {
            throw new NotFoundException("trxId: " + trxId);
        }
        return trxMapper.selectResponseById(trxId);
    }

    /**
     * Delete TRX
     */
    @Transactional
    @WorkListRecord(workId = "Trx", crudType = "D", pkExpression = "#trxId", workName = "거래")
    public void deleteTrx(String trxId) {
        // 존재 여부 확인
        int count = trxMapper.countByTrxId(trxId);
        if (count == 0) {
            throw new NotFoundException("trxId: " + trxId);
        }

        // 1. Create history BEFORE deleting (INSERT ... SELECT from FWK_TRX)
        Integer nextVersion = trxHistoryMapper.getNextVersion(trxId);
        trxHistoryMapper.insertHistoryFromTrx(trxId, nextVersion, "Delete operation");
        log.debug("Created history for trxId: {} with version: {} before deletion", trxId, nextVersion);

        // 2. Delete the entity
        trxMapper.deleteTrxById(trxId);
    }

    /**
     * Get TRX detail with basic information
     */
    public TrxDetailResponse getTrxDetail(String trxId) {
        TrxDetailResponse detail = trxMapper.findTrxDetailById(trxId);
        if (detail == null) {
            throw new NotFoundException("trxId: " + trxId);
        }
        return detail;
    }

    /**
     * 거래관리 엑셀 내보내기
     */
    public byte[] exportTransactions(
            String searchField,
            String searchValue,
            String trxStopYnFilter,
            String orgIdFilter,
            String sortBy,
            String sortDirection) {
        List<TrxListResponse> data = trxMapper.findAllWithSearchAndMessagesForExport(
                searchField, searchValue, trxStopYnFilter, orgIdFilter, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("거래ID", 15, "trxId"),
                new ExcelColumnDefinition("거래명", 25, "trxName"),
                new ExcelColumnDefinition("기관ID", 10, "orgId"),
                new ExcelColumnDefinition("기관명", 20, "orgName"),
                new ExcelColumnDefinition("기동/수동", 10, "autoManualType"),
                new ExcelColumnDefinition("요청전문ID", 15, "reqMessageId"),
                new ExcelColumnDefinition("응답전문ID", 15, "resMessageId"),
                new ExcelColumnDefinition("현재 상태", 10, "trxStopYn"),
                new ExcelColumnDefinition("대응답", 10, "proxyResYn"),
                new ExcelColumnDefinition("다수 응답", 10, "multiResYn"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (TrxListResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("trxId", item.getTrxId());
            row.put("trxName", item.getTrxName());
            row.put("orgId", item.getOrgId());
            row.put("orgName", item.getOrgName());
            row.put("autoManualType", item.getAutoManualType());
            row.put("reqMessageId", item.getReqMessageId());
            row.put("resMessageId", item.getResMessageId());
            row.put("trxStopYn", item.getTrxStopYn());
            row.put("proxyResYn", item.getProxyResYn());
            row.put("multiResYn", item.getMultiResYn());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("거래", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 업무분류 옵션 목록 조회
     */
    public List<String> getBizGroupOptions() {
        return trxMapper.findDistinctBizGroups();
    }

    /**
     * Get TRX with related messages
     */
    public TrxWithMessagesResponse getTrxWithMessages(String trxId) {
        // 1. Get transaction detail
        TrxDetailResponse detail = trxMapper.findTrxDetailById(trxId);
        if (detail == null) {
            throw new NotFoundException("trxId: " + trxId);
        }

        // 2. Get related messages
        List<TrxMessageResponse> messages = trxMapper.findMessagesByTrxId(trxId);

        // 3. Combine into response
        return TrxWithMessagesResponse.builder()
                .trxId(detail.getTrxId())
                .operModeType(detail.getOperModeType())
                .trxStopYn(detail.getTrxStopYn())
                .trxName(detail.getTrxName())
                .trxDesc(detail.getTrxDesc())
                .trxType(detail.getTrxType())
                .retryTrxYn(detail.getRetryTrxYn())
                .maxRetryCount(detail.getMaxRetryCount())
                .retryMiCycle(detail.getRetryMiCycle())
                .bizGroupId(detail.getBizGroupId())
                .bizdayTrxYn(detail.getBizdayTrxYn())
                .bizdayTrxStartTime(detail.getBizdayTrxStartTime())
                .bizdayTrxEndTime(detail.getBizdayTrxEndTime())
                .saturdayTrxYn(detail.getSaturdayTrxYn())
                .saturdayTrxStartTime(detail.getSaturdayTrxStartTime())
                .saturdayTrxEndTime(detail.getSaturdayTrxEndTime())
                .holidayTrxYn(detail.getHolidayTrxYn())
                .holidayTrxStartTime(detail.getHolidayTrxStartTime())
                .holidayTrxEndTime(detail.getHolidayTrxEndTime())
                .trxStopReason(detail.getTrxStopReason())
                .messages(messages)
                .build();
    }
}

package com.example.spideradmin.domain.batch.service;

import com.example.spideradmin.domain.batch.dto.BatchAppCreateRequest;
import com.example.spideradmin.domain.batch.dto.BatchAppDetailResponse;
import com.example.spideradmin.domain.batch.dto.BatchAppResponse;
import com.example.spideradmin.domain.batch.dto.BatchAppSearchRequest;
import com.example.spideradmin.domain.batch.dto.BatchAppUpdateRequest;
import com.example.spideradmin.domain.batch.dto.WasExecBatchResponse;
import com.example.spideradmin.domain.batch.dto.WasInstanceSelectionResponse;
import com.example.spideradmin.domain.batch.mapper.BatchAppMapper;
import com.example.spideradmin.domain.batch.mapper.WasExecBatchMapper;
import com.example.spideradmin.domain.wasinstance.mapper.WasInstanceMapper;
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
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BatchApp 관리 Service 구현체
 *
 * Entity 없이 DTO 직접 사용 패턴:
 * - Mapper insert/update는 @Param("dto") RequestDTO + 감사 필드로 처리
 * - 조회는 ResponseDTO 직접 반환
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchAppService {

    private final BatchAppMapper batchAppMapper;
    private final WasExecBatchMapper wasExecBatchMapper;
    private final WasInstanceMapper wasInstanceMapper;

    // ==================== 조회 ====================

    public List<BatchAppResponse> getAllBatchApps() {
        return batchAppMapper.findAll();
    }

    public PageResponse<BatchAppResponse> getBatchApps(PageRequest pageRequest) {
        long total = batchAppMapper.countAll();

        List<BatchAppResponse> dtos = batchAppMapper.findAllPaged(pageRequest.getOffset(), pageRequest.getEndRow());

        return PageResponse.of(dtos, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public PageResponse<BatchAppResponse> getBatchAppsWithSearch(
            PageRequest pageRequest, BatchAppSearchRequest searchDTO) {
        long total = batchAppMapper.countAllWithSearch(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getInstanceIdFilter(),
                searchDTO.getBatchCycleFilter());

        List<BatchAppResponse> dtos = batchAppMapper.findAllWithSearch(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getInstanceIdFilter(),
                searchDTO.getBatchCycleFilter(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(dtos, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public BatchAppDetailResponse getBatchAppById(String batchAppId) {
        BatchAppResponse batchApp = batchAppMapper.selectResponseById(batchAppId);
        if (batchApp == null) {
            throw new NotFoundException("batchAppId: " + batchAppId);
        }

        // 현재 배치앱에 할당된 WAS 인스턴스 조회
        List<WasExecBatchResponse> assignedWasExecBatches =
                wasExecBatchMapper.selectByBatchAppIdWithDetails(batchAppId);
        Set<String> assignedInstanceIds = assignedWasExecBatches.stream()
                .map(WasExecBatchResponse::getInstanceId)
                .collect(Collectors.toSet());

        // UseYn 맵 생성
        Map<String, String> useYnMap = assignedWasExecBatches.stream()
                .collect(Collectors.toMap(
                        WasExecBatchResponse::getInstanceId, web -> web.getUseYn() != null ? web.getUseYn() : "Y"));

        // 전체 WAS 인스턴스 조회
        var allInstances = wasInstanceMapper.selectAll();

        // 할당된 인스턴스 목록 (좌측 패널)
        List<WasInstanceSelectionResponse> assignedInstances = allInstances.stream()
                .filter(instance -> assignedInstanceIds.contains(instance.getInstanceId()))
                .map(instance -> WasInstanceSelectionResponse.builder()
                        .instanceId(instance.getInstanceId())
                        .instanceName(instance.getInstanceName())
                        .instanceDesc(instance.getInstanceDesc())
                        .ip(instance.getIp())
                        .port(instance.getPort())
                        .instanceType(instance.getInstanceType())
                        .isAssigned(true)
                        .useYn(useYnMap.getOrDefault(instance.getInstanceId(), "Y"))
                        .build())
                .toList();

        // 전체 인스턴스 목록 with 할당 상태 (우측 패널)
        List<WasInstanceSelectionResponse> allInstancesWithStatus = allInstances.stream()
                .map(instance -> WasInstanceSelectionResponse.builder()
                        .instanceId(instance.getInstanceId())
                        .instanceName(instance.getInstanceName())
                        .instanceDesc(instance.getInstanceDesc())
                        .ip(instance.getIp())
                        .port(instance.getPort())
                        .instanceType(instance.getInstanceType())
                        .isAssigned(assignedInstanceIds.contains(instance.getInstanceId()))
                        .useYn(useYnMap.getOrDefault(instance.getInstanceId(), "Y"))
                        .build())
                .toList();

        return BatchAppDetailResponse.builder()
                .batchApp(batchApp)
                .assignedInstances(assignedInstances)
                .allInstances(allInstancesWithStatus)
                .build();
    }

    public boolean existsByBatchAppId(String batchAppId) {
        return batchAppMapper.countByBatchAppId(batchAppId) > 0;
    }

    public byte[] exportBatchApps(
            String searchField,
            String searchValue,
            String instanceIdFilter,
            String batchCycleFilter,
            String sortBy,
            String sortDirection) {
        List<BatchAppResponse> data = batchAppMapper.findAllForExport(
                searchField, searchValue, instanceIdFilter, batchCycleFilter, sortBy, sortDirection);
        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }
        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("배치 APP ID", 20, "batchAppId"),
                new ExcelColumnDefinition("배치 APP명", 25, "batchAppName"),
                new ExcelColumnDefinition("배치 APP FILE명", 25, "batchAppFileName"),
                new ExcelColumnDefinition("선행 배치APP ID", 20, "preBatchAppId"),
                new ExcelColumnDefinition("배치 실행주기", 12, "batchCycle"),
                new ExcelColumnDefinition("재시도", 10, "retryableYn"),
                new ExcelColumnDefinition("WAS별실행", 10, "perWasYn"),
                new ExcelColumnDefinition("중요도", 10, "importantType"));
        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (BatchAppResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("batchAppId", item.getBatchAppId());
            row.put("batchAppName", item.getBatchAppName());
            row.put("batchAppFileName", item.getBatchAppFileName());
            row.put("preBatchAppId", item.getPreBatchAppId());
            row.put("batchCycle", item.getBatchCycle());
            row.put("retryableYn", item.getRetryableYn());
            row.put("perWasYn", item.getPerWasYn());
            row.put("importantType", item.getImportantType());
            rows.add(row);
        }
        try {
            return ExcelExportUtil.createWorkbook("배치 앱", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    // ==================== 생성/수정/삭제 ====================

    @Transactional
    public BatchAppResponse createBatchApp(BatchAppCreateRequest requestDTO) {
        // 중복 체크
        if (batchAppMapper.countByBatchAppId(requestDTO.getBatchAppId()) > 0) {
            throw new DuplicateException("batchAppId: " + requestDTO.getBatchAppId());
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        // DTO 직접 Mapper로 전달
        batchAppMapper.insertBatchApp(requestDTO, now, userId);

        // WAS 인스턴스 할당
        if (requestDTO.getInstanceIds() != null && !requestDTO.getInstanceIds().isEmpty()) {
            doAssignInstances(requestDTO.getBatchAppId(), requestDTO.getInstanceIds());
        }

        return batchAppMapper.selectResponseById(requestDTO.getBatchAppId());
    }

    @Transactional
    public BatchAppResponse updateBatchApp(String batchAppId, BatchAppUpdateRequest requestDTO) {
        if (batchAppMapper.countByBatchAppId(batchAppId) == 0) {
            throw new NotFoundException("batchAppId: " + batchAppId);
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        // DTO 직접 Mapper로 전달
        batchAppMapper.updateBatchApp(batchAppId, requestDTO, now, userId);

        // WAS 인스턴스 할당 업데이트 (전체 삭제 후 재할당)
        wasExecBatchMapper.deleteByBatchAppId(batchAppId);
        if (requestDTO.getInstanceIds() != null && !requestDTO.getInstanceIds().isEmpty()) {
            doAssignInstances(batchAppId, requestDTO.getInstanceIds());
        }

        return batchAppMapper.selectResponseById(batchAppId);
    }

    @Transactional
    public void deleteBatchApp(String batchAppId) {
        if (batchAppMapper.countByBatchAppId(batchAppId) == 0) {
            throw new NotFoundException("batchAppId: " + batchAppId);
        }

        // WAS 인스턴스 할당 삭제
        wasExecBatchMapper.deleteByBatchAppId(batchAppId);

        // 배치 앱 삭제
        batchAppMapper.deleteBatchAppById(batchAppId);
    }

    // ==================== WAS Instance 할당 ====================

    @Transactional
    public void assignInstancesToBatchApp(String batchAppId, List<String> instanceIds) {
        doAssignInstances(batchAppId, instanceIds);
    }

    private void doAssignInstances(String batchAppId, List<String> instanceIds) {
        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        List<Map<String, String>> wasExecBatches = instanceIds.stream()
                .map(instanceId -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("batchAppId", batchAppId);
                    map.put("instanceId", instanceId);
                    map.put("useYn", "Y");
                    map.put("lastUpdateDtime", now);
                    map.put("lastUpdateUserId", userId);
                    return map;
                })
                .toList();

        if (!wasExecBatches.isEmpty()) {
            wasExecBatchMapper.insertWasExecBatchBatch(wasExecBatches);
        }
    }

    @Transactional
    public void addInstanceToBatchApp(String batchAppId, String instanceId) {
        // 배치 앱 존재 확인
        if (batchAppMapper.countByBatchAppId(batchAppId) == 0) {
            throw new NotFoundException("batchAppId: " + batchAppId);
        }

        // 인스턴스 존재 확인
        if (wasInstanceMapper.countById(instanceId) == 0) {
            throw new NotFoundException("instanceId: " + instanceId);
        }

        // 이미 할당되어 있는지 확인
        if (wasExecBatchMapper.countById(batchAppId, instanceId) > 0) {
            throw new DuplicateException("batchAppId: " + batchAppId + ", instanceId: " + instanceId);
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        wasExecBatchMapper.insertWasExecBatch(batchAppId, instanceId, "Y", now, userId);
    }

    @Transactional
    public void updateInstanceAssignment(String batchAppId, String instanceId, String useYn) {
        if (wasExecBatchMapper.countById(batchAppId, instanceId) == 0) {
            throw new NotFoundException("batchAppId: " + batchAppId + ", instanceId: " + instanceId);
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        wasExecBatchMapper.updateWasExecBatch(batchAppId, instanceId, useYn, now, userId);
    }

    @Transactional
    public void removeInstanceFromBatchApp(String batchAppId, String instanceId) {
        if (wasExecBatchMapper.countById(batchAppId, instanceId) == 0) {
            throw new NotFoundException("batchAppId: " + batchAppId + ", instanceId: " + instanceId);
        }

        wasExecBatchMapper.deleteWasExecBatchById(batchAppId, instanceId);
    }

    /**
     * FWK_BATCH_APP.CRON_TEXT를 단독으로 업데이트한다.
     * 스케줄 변경 API에서 전체 배치앱 정보를 수정하지 않고 Cron 표현식만 변경할 때 사용한다.
     *
     * @param batchAppId 배치 APP ID
     * @param cronText   새 Cron 표현식
     */
    @Transactional
    public void updateCronText(String batchAppId, String cronText) {
        if (batchAppMapper.countByBatchAppId(batchAppId) == 0) {
            throw new NotFoundException("batchAppId: " + batchAppId);
        }
        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();
        batchAppMapper.updateCronText(batchAppId, cronText, now, userId);
    }
}

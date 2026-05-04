package com.example.spideradmin.domain.wasgroup.service;

import com.example.spideradmin.domain.wasgroup.dto.WasGroupRequest;
import com.example.spideradmin.domain.wasgroup.dto.WasGroupResponse;
import com.example.spideradmin.domain.wasgroup.mapper.WasGroupMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WasGroupService {

    private final WasGroupMapper wasGroupMapper;

    // ==================== Group CRUD ====================

    /**
     * 전체 그룹 조회
     */
    public List<WasGroupResponse> getAllGroups() {
        log.info("Fetching all WAS groups");
        return wasGroupMapper.selectAll();
    }

    /**
     * 페이징된 그룹 조회
     */
    public PageResponse<WasGroupResponse> getGroups(
            PageRequest pageRequest, String wasGroupId, String wasGroupName, String wasGroupDesc) {
        int page = Math.max(pageRequest.getPage(), 0);
        int size = Math.max(pageRequest.getSize(), 1);
        int offset = page * size;

        List<WasGroupResponse> dtos = wasGroupMapper.findBySearchPaging(
                wasGroupId,
                wasGroupName,
                wasGroupDesc,
                offset,
                size,
                pageRequest.getSortBy(),
                pageRequest.getSortDirection());
        long totalCount = wasGroupMapper.countBySearch(wasGroupId, wasGroupName, wasGroupDesc);

        int totalPages = (int) Math.ceil((double) totalCount / size);
        return PageResponse.<WasGroupResponse>builder()
                .content(dtos)
                .currentPage(page + 1)
                .totalPages(totalPages)
                .totalElements(totalCount)
                .size(size)
                .hasNext(page + 1 < totalPages)
                .hasPrevious(page > 0)
                .build();
    }

    /**
     * 그룹 ID로 조회
     */
    public WasGroupResponse getGroupById(String wasGroupId) {
        log.info("Fetching WAS group by ID: {}", wasGroupId);

        WasGroupResponse group = wasGroupMapper.selectResponseById(wasGroupId);
        if (group == null) {
            throw new NotFoundException("wasGroupId: " + wasGroupId);
        }

        return group;
    }

    /**
     * 그룹 생성
     */
    @Transactional
    public WasGroupResponse createGroup(WasGroupRequest dto) {
        log.info("Creating new WAS group: {}", dto.getWasGroupId());

        if (wasGroupMapper.countById(dto.getWasGroupId()) > 0) {
            throw new DuplicateException("wasGroupId: " + dto.getWasGroupId());
        }

        wasGroupMapper.insert(dto);

        log.info("WAS group created successfully: {}", dto.getWasGroupId());
        return wasGroupMapper.selectResponseById(dto.getWasGroupId());
    }

    /**
     * 그룹 수정
     */
    @Transactional
    public WasGroupResponse updateGroup(String wasGroupId, WasGroupRequest dto) {
        log.info("Updating WAS group: {}", wasGroupId);

        if (wasGroupMapper.countById(wasGroupId) == 0) {
            throw new NotFoundException("wasGroupId: " + wasGroupId);
        }

        wasGroupMapper.update(wasGroupId, dto);

        log.info("WAS group updated successfully: {}", wasGroupId);
        return wasGroupMapper.selectResponseById(wasGroupId);
    }

    /**
     * 그룹 삭제
     */
    @Transactional
    public void deleteGroup(String wasGroupId) {
        log.info("Deleting WAS group: {}", wasGroupId);

        if (wasGroupMapper.countById(wasGroupId) == 0) {
            throw new NotFoundException("wasGroupId: " + wasGroupId);
        }

        // 먼저 매핑된 인스턴스들을 모두 제거
        wasGroupMapper.deleteAllGroupInstances(wasGroupId);

        // 그룹 삭제
        wasGroupMapper.deleteById(wasGroupId);
        log.info("WAS group deleted successfully: {}", wasGroupId);
    }

    public byte[] exportWasGroups(
            String wasGroupId, String wasGroupName, String wasGroupDesc, String sortBy, String sortDirection) {
        List<WasGroupResponse> data =
                wasGroupMapper.findAllForExport(wasGroupId, wasGroupName, wasGroupDesc, sortBy, sortDirection);
        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }
        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("WAS 그룹 ID", 20, "wasGroupId"),
                new ExcelColumnDefinition("WAS 그룹명", 25, "wasGroupName"),
                new ExcelColumnDefinition("WAS 그룹설명", 40, "wasGroupDesc"));
        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (WasGroupResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("wasGroupId", item.getWasGroupId());
            row.put("wasGroupName", item.getWasGroupName());
            row.put("wasGroupDesc", item.getWasGroupDesc());
            rows.add(row);
        }
        try {
            return ExcelExportUtil.createWorkbook("WAS 그룹", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    // ==================== Group-Instance 매핑 관리 ====================

    /**
     * 그룹에 여러 인스턴스 추가
     */
    @Transactional
    public void addInstancesToGroup(String wasGroupId, List<String> instanceIds) {
        log.info("Adding {} instances to group {}", instanceIds.size(), wasGroupId);

        for (String instanceId : instanceIds) {
            // 이미 존재하는 매핑은 건너뛰기
            if (wasGroupMapper.existsGroupInstance(wasGroupId, instanceId) == 0) {
                wasGroupMapper.insertGroupInstance(wasGroupId, instanceId);
            }
        }

        log.info("Instances added to group {} successfully", wasGroupId);
    }

    /**
     * 그룹의 모든 인스턴스 제거
     */
    @Transactional
    public void removeAllInstancesFromGroup(String wasGroupId) {
        log.info("Removing all instances from group {}", wasGroupId);

        wasGroupMapper.deleteAllGroupInstances(wasGroupId);
        log.info("All instances removed from group {} successfully", wasGroupId);
    }

    /**
     * 그룹에 속한 인스턴스 ID 목록 조회
     */
    public List<String> getInstanceIdsByGroup(String wasGroupId) {
        log.info("Fetching instance IDs for group: {}", wasGroupId);

        return wasGroupMapper.selectInstanceIdsByGroupId(wasGroupId);
    }

    /**
     * 그룹에 속한 인스턴스 전체 정보 조회 (프론트엔드용)
     */
    public List<Map<String, Object>> getInstanceDetailsByGroup(String wasGroupId) {
        log.info("Fetching instance details for group: {}", wasGroupId);

        return wasGroupMapper.selectInstanceDetailsByGroupId(wasGroupId);
    }

    /**
     * 그룹의 인스턴스 개수 조회
     */
    public long countInstancesByGroup(String wasGroupId) {
        return wasGroupMapper.countInstancesByGroupId(wasGroupId);
    }
}

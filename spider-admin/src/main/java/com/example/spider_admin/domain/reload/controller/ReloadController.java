package com.example.spider_admin.domain.reload.controller;

import com.example.spider_admin.domain.reload.dto.ReloadExecuteRequest;
import com.example.spider_admin.domain.reload.dto.ReloadResultResponse;
import com.example.spider_admin.domain.reload.dto.ReloadTypeResponse;
import com.example.spider_admin.domain.reload.service.ReloadService;
import com.example.spider_admin.domain.wasgroup.dto.WasGroupResponse;
import com.example.spider_admin.domain.wasgroup.service.WasGroupService;
import com.example.spider_admin.domain.wasinstance.dto.WasInstanceResponse;
import com.example.spider_admin.domain.wasinstance.service.WasInstanceService;
import com.example.spider_admin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 운영정보 Reload Controller
 */
@RestController
@RequestMapping("/api/reload")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('RELOAD:R')")
@Slf4j
public class ReloadController {

    private final ReloadService reloadService;
    private final WasInstanceService wasInstanceService;
    private final WasGroupService wasGroupService;

    /**
     * Reload 대상 항목 목록 조회
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<ReloadTypeResponse>>> getReloadTypes() {
        return ResponseEntity.ok(ApiResponse.success(reloadService.getReloadTypes()));
    }

    /**
     * WAS 그룹 목록 조회 (드롭다운용)
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<WasGroupResponse>>> getWasGroups() {
        return ResponseEntity.ok(ApiResponse.success(wasGroupService.getAllGroups()));
    }

    /**
     * WAS 인스턴스 목록 조회 (전체 또는 그룹 필터링)
     */
    @GetMapping("/instances")
    public ResponseEntity<ApiResponse<List<WasInstanceResponse>>> getInstances(
            @RequestParam(required = false) String wasGroupId) {
        if (wasGroupId == null || wasGroupId.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(wasInstanceService.getAllInstances()));
        }
        // 그룹별 인스턴스 조회 후 WasInstanceDTO로 변환
        List<String> instanceIds = wasGroupService.getInstanceIdsByGroup(wasGroupId);
        List<WasInstanceResponse> instances = wasInstanceService.getAllInstances().stream()
                .filter(dto -> instanceIds.contains(dto.getInstanceId()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    /**
     * Reload 실행 (WRITE 권한 필요)
     */
    @PostMapping("/execute")
    @PreAuthorize("hasAnyAuthority('RELOAD:W', 'TRX:W', 'APP_MAPPING:W')")
    public ResponseEntity<ApiResponse<ReloadResultResponse>> executeReload(
            @Valid @RequestBody ReloadExecuteRequest request) {
        log.info("Reload 실행 요청: type={}, instances={}", request.getReloadType(), request.getInstanceIds());
        return ResponseEntity.ok(ApiResponse.success(reloadService.executeReload(request)));
    }
}

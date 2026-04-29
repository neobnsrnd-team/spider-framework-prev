package com.example.spider_admin.domain.property.controller;

import com.example.spider_admin.domain.property.dto.PropertyBackupRequest;
import com.example.spider_admin.domain.property.dto.PropertyHistoryResponse;
import com.example.spider_admin.domain.property.dto.PropertyHistoryVersionResponse;
import com.example.spider_admin.domain.property.service.PropertyHistoryService;
import com.example.spider_admin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 프로퍼티 백업/복원 REST Controller
 */
@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PROPERTY_DB:R')")
public class PropertyHistoryController {

    private final PropertyHistoryService propertyHistoryService;

    /**
     * 프로퍼티 그룹 전체 백업
     * POST /api/properties/groups/{propertyGroupId}/backup
     */
    @PostMapping("/groups/{propertyGroupId}/backup")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<Void>> backupPropertyGroup(
            @PathVariable String propertyGroupId, @Valid @RequestBody PropertyBackupRequest requestDTO) {
        propertyHistoryService.backupPropertyGroup(propertyGroupId, requestDTO.getReason());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 프로퍼티 그룹의 히스토리 버전 목록 조회
     * GET /api/properties/groups/{propertyGroupId}/history/versions
     */
    @GetMapping("/groups/{propertyGroupId}/history/versions")
    public ResponseEntity<ApiResponse<List<PropertyHistoryVersionResponse>>> getHistoryVersions(
            @PathVariable String propertyGroupId) {
        List<PropertyHistoryVersionResponse> versions = propertyHistoryService.getHistoryVersions(propertyGroupId);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    /**
     * 프로퍼티 그룹의 특정 버전 히스토리 데이터 조회
     * GET /api/properties/groups/{propertyGroupId}/history/{version}
     */
    @GetMapping("/groups/{propertyGroupId}/history/{version}")
    public ResponseEntity<ApiResponse<List<PropertyHistoryResponse>>> getHistoryByVersion(
            @PathVariable String propertyGroupId, @PathVariable int version) {
        List<PropertyHistoryResponse> histories = propertyHistoryService.getHistoryByVersion(propertyGroupId, version);
        return ResponseEntity.ok(ApiResponse.success(histories));
    }

    /**
     * 프로퍼티 그룹 전체 복원
     * POST /api/properties/groups/{propertyGroupId}/restore/{version}
     */
    @PostMapping("/groups/{propertyGroupId}/restore/{version}")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<Void>> restorePropertyGroup(
            @PathVariable String propertyGroupId, @PathVariable int version) {
        propertyHistoryService.restorePropertyGroup(propertyGroupId, version);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

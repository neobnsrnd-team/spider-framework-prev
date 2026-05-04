package com.example.spideradmin.domain.wasgroup.controller;

import com.example.spideradmin.domain.wasgroup.dto.WasGroupRequest;
import com.example.spideradmin.domain.wasgroup.dto.WasGroupResponse;
import com.example.spideradmin.domain.wasgroup.service.WasGroupService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/was/group")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WAS_GROUP:R')")
public class WasGroupController {

    private final WasGroupService wasGroupService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WasGroupResponse>>> getAllGroups() {
        List<WasGroupResponse> groups = wasGroupService.getAllGroups();
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<WasGroupResponse>>> getGroupsWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String wasGroupId,
            @RequestParam(required = false) String wasGroupName,
            @RequestParam(required = false) String wasGroupDesc,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<WasGroupResponse> response =
                wasGroupService.getGroups(pageRequest, wasGroupId, wasGroupName, wasGroupDesc);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{wasGroupId}")
    public ResponseEntity<ApiResponse<WasGroupResponse>> getGroupById(@PathVariable String wasGroupId) {
        WasGroupResponse group = wasGroupService.getGroupById(wasGroupId);
        return ResponseEntity.ok(ApiResponse.success(group));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WAS_GROUP:W')")
    public ResponseEntity<ApiResponse<WasGroupResponse>> createGroup(@Valid @RequestBody WasGroupRequest dto) {
        WasGroupResponse created = wasGroupService.createGroup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("그룹이 생성되었습니다", created));
    }

    @PutMapping("/{wasGroupId}")
    @PreAuthorize("hasAuthority('WAS_GROUP:W')")
    public ResponseEntity<ApiResponse<WasGroupResponse>> updateGroup(
            @PathVariable String wasGroupId, @Valid @RequestBody WasGroupRequest dto) {
        WasGroupResponse updated = wasGroupService.updateGroup(wasGroupId, dto);

        if (dto.getInstanceIds() != null) {
            wasGroupService.removeAllInstancesFromGroup(wasGroupId);
            if (!dto.getInstanceIds().isEmpty()) {
                wasGroupService.addInstancesToGroup(wasGroupId, dto.getInstanceIds());
            }
        }

        return ResponseEntity.ok(ApiResponse.success("그룹이 수정되었습니다", updated));
    }

    @DeleteMapping("/{wasGroupId}")
    @PreAuthorize("hasAuthority('WAS_GROUP:W')")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable String wasGroupId) {
        wasGroupService.deleteGroup(wasGroupId);
        return ResponseEntity.ok(ApiResponse.success("그룹이 삭제되었습니다", null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportWasGroups(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String wasGroupId,
            @RequestParam(required = false) String wasGroupName,
            @RequestParam(required = false) String wasGroupDesc) {
        byte[] excelBytes =
                wasGroupService.exportWasGroups(wasGroupId, wasGroupName, wasGroupDesc, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("WasGroup", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    // ==================== Group-Instance 매핑 관리 ====================

    @PostMapping("/{wasGroupId}/instances")
    @PreAuthorize("hasAuthority('WAS_GROUP:W')")
    public ResponseEntity<ApiResponse<Void>> addInstancesToGroup(
            @PathVariable String wasGroupId, @RequestBody List<String> instanceIds) {
        wasGroupService.addInstancesToGroup(wasGroupId, instanceIds);
        return ResponseEntity.ok(ApiResponse.success("인스턴스들이 그룹에 추가되었습니다", null));
    }

    @DeleteMapping("/{wasGroupId}/instances")
    @PreAuthorize("hasAuthority('WAS_GROUP:W')")
    public ResponseEntity<ApiResponse<Void>> removeAllInstancesFromGroup(@PathVariable String wasGroupId) {
        wasGroupService.removeAllInstancesFromGroup(wasGroupId);
        return ResponseEntity.ok(ApiResponse.success("모든 인스턴스가 그룹에서 제거되었습니다", null));
    }

    @GetMapping("/{wasGroupId}/instances")
    public ResponseEntity<ApiResponse<List<String>>> getInstanceIdsByGroup(@PathVariable String wasGroupId) {
        List<String> instanceIds = wasGroupService.getInstanceIdsByGroup(wasGroupId);
        return ResponseEntity.ok(ApiResponse.success(instanceIds));
    }

    @GetMapping("/{wasGroupId}/instances/details")
    public ResponseEntity<ApiResponse<List<?>>> getInstanceDetailsByGroup(@PathVariable String wasGroupId) {
        List<?> instances = wasGroupService.getInstanceDetailsByGroup(wasGroupId);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @GetMapping("/{wasGroupId}/instances/count")
    public ResponseEntity<ApiResponse<Long>> countInstancesByGroup(@PathVariable String wasGroupId) {
        long count = wasGroupService.countInstancesByGroup(wasGroupId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}

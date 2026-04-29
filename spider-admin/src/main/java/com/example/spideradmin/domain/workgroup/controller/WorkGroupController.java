package com.example.spideradmin.domain.workgroup.controller;

import com.example.spideradmin.domain.workgroup.dto.WorkGroupCreateRequest;
import com.example.spideradmin.domain.workgroup.dto.WorkGroupResponse;
import com.example.spideradmin.domain.workgroup.dto.WorkGroupUpdateRequest;
import com.example.spideradmin.domain.workgroup.service.WorkGroupService;
import com.example.spideradmin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 작업 그룹 REST Controller.
 *
 * <p>FWK_WORK_GROUP 테이블 대상 그룹 CRUD API를 제공합니다.
 */
@RestController
@RequestMapping("/api/work-groups")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORK_TASK:R')")
public class WorkGroupController {

    private final WorkGroupService workGroupService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkGroupResponse>>> getGroups(@RequestParam String userId) {
        return ResponseEntity.ok(ApiResponse.success(workGroupService.getGroups(userId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<WorkGroupResponse>> createGroup(
            @Valid @RequestBody WorkGroupCreateRequest request) {
        WorkGroupResponse response = workGroupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("작업 그룹이 생성되었습니다.", response));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<WorkGroupResponse>> updateGroup(
            @Valid @RequestBody WorkGroupUpdateRequest request) {
        WorkGroupResponse response = workGroupService.updateGroup(request);
        return ResponseEntity.ok(ApiResponse.success("작업 그룹이 수정되었습니다.", response));
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable String groupId) {
        workGroupService.deleteGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success("작업 그룹이 삭제되었습니다.", null));
    }
}

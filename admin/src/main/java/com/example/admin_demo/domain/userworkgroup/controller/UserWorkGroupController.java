package com.example.admin_demo.domain.userworkgroup.controller;

import com.example.admin_demo.domain.userworkgroup.dto.UserWorkGroupCreateRequest;
import com.example.admin_demo.domain.userworkgroup.dto.UserWorkGroupResponse;
import com.example.admin_demo.domain.userworkgroup.dto.UserWorkGroupUpdateRequest;
import com.example.admin_demo.domain.userworkgroup.service.UserWorkGroupService;
import com.example.admin_demo.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <h3>사용자 작업 그룹 REST Controller</h3>
 * <p>FWK_USER_WORK_GROUP 테이블 대상 그룹 CRUD API를 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/user-work-groups")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORK_TASK:R')")
public class UserWorkGroupController {

    private final UserWorkGroupService userWorkGroupService;

    /**
     * 사용자 ID 기준으로 작업 그룹 목록을 조회합니다.
     * @param userId 사용자 ID
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserWorkGroupResponse>>> getUserWorkGroups(@RequestParam String userId) {
        List<UserWorkGroupResponse> response = userWorkGroupService.getUserWorkGroups(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<UserWorkGroupResponse>> createUserWorkGroup(
            @Valid @RequestBody UserWorkGroupCreateRequest dto) {
        UserWorkGroupResponse response = userWorkGroupService.createUserWorkGroup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("작업 그룹이 생성되었습니다.", response));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<UserWorkGroupResponse>> updateUserWorkGroup(
            @Valid @RequestBody UserWorkGroupUpdateRequest dto) {
        UserWorkGroupResponse response = userWorkGroupService.updateUserWorkGroup(dto);
        return ResponseEntity.ok(ApiResponse.success("작업 그룹이 수정되었습니다.", response));
    }

    /**
     * 작업 그룹을 삭제합니다.
     * @param userId 사용자 ID
     * @param groupId 그룹 ID
     */
    @DeleteMapping("/{userId}/{groupId}")
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<Void>> deleteUserWorkGroup(
            @PathVariable String userId, @PathVariable String groupId) {
        userWorkGroupService.deleteUserWorkGroup(userId, groupId);
        return ResponseEntity.ok(ApiResponse.success("작업 그룹이 삭제되었습니다.", null));
    }
}

package com.example.admin_demo.domain.worktask.controller;

import com.example.admin_demo.domain.worktask.dto.WorkTaskGroupMoveRequest;
import com.example.admin_demo.domain.worktask.dto.WorkTaskResponse;
import com.example.admin_demo.domain.worktask.dto.WorkTaskTransferRequest;
import com.example.admin_demo.domain.worktask.service.WorkTaskService;
import com.example.admin_demo.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <h3>작업함 REST Controller</h3>
 * <p>작업함 목록 조회, 그룹 이동, 권한이양 API를 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/work-tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORK_TASK:R')")
public class WorkTaskController {

    private final WorkTaskService workTaskService;

    /**
     * 사용자의 작업함 목록을 조회합니다.
     * @param userId      사용자 ID
     * @param workGroupId 그룹 필터 (선택, 미입력 시 전체 조회)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkTaskResponse>>> getWorkTasks(
            @RequestParam String userId, @RequestParam(required = false) String workGroupId) {
        List<WorkTaskResponse> response = workTaskService.getWorkTasks(userId, workGroupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 선택한 작업함 항목들을 다른 그룹으로 이동합니다.
     */
    @PutMapping("/group-move")
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<Void>> moveGroup(@Valid @RequestBody WorkTaskGroupMoveRequest dto) {
        workTaskService.moveGroup(dto);
        return ResponseEntity.ok(ApiResponse.success("그룹 이동이 완료되었습니다.", null));
    }

    /**
     * 선택한 메뉴 권한을 다른 사용자에게 이양합니다.
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<Void>> transferAuthority(@Valid @RequestBody WorkTaskTransferRequest dto) {
        workTaskService.transferAuthority(dto);
        return ResponseEntity.ok(ApiResponse.success("권한이양이 완료되었습니다.", null));
    }
}

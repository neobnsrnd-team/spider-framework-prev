package com.example.admin_demo.domain.worklist.controller;

import com.example.admin_demo.domain.worklist.dto.WorkListApprovalRequest;
import com.example.admin_demo.domain.worklist.dto.WorkListGroupMoveRequest;
import com.example.admin_demo.domain.worklist.dto.WorkListResponse;
import com.example.admin_demo.domain.worklist.dto.WorkListScriptResponse;
import com.example.admin_demo.domain.worklist.dto.WorkListTransferRequest;
import com.example.admin_demo.domain.worklist.service.WorkListScriptService;
import com.example.admin_demo.domain.worklist.service.WorkListService;
import com.example.admin_demo.global.dto.ApiResponse;
import com.example.admin_demo.global.security.CustomUserDetails;
import com.example.admin_demo.global.util.SecurityUtil;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 작업함 목록 REST Controller.
 *
 * <p>FWK_WORK_LIST 기반 목록 조회, 그룹 이동, 권한이양 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/work-list")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORK_TASK:R')")
public class WorkListController {

    private final WorkListService workListService;
    private final WorkListScriptService workListScriptService;

    /**
     * 작업함 목록 조회.
     *
     * @param userId  사용자 ID (필수)
     * @param groupId 그룹 필터 (선택 — 없으면 전체 조회)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkListResponse>>> getWorkList(
            @RequestParam String userId, @RequestParam(required = false) String groupId) {
        return ResponseEntity.ok(ApiResponse.success(workListService.getWorkList(userId, groupId)));
    }

    /** 선택 항목을 지정 그룹으로 이동. */
    @PutMapping("/group-move")
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<Void>> moveGroup(@Valid @RequestBody WorkListGroupMoveRequest request) {
        workListService.moveGroup(request);
        return ResponseEntity.ok(ApiResponse.success("그룹 이동이 완료되었습니다.", null));
    }

    /** 권한이양 — 선택 항목을 다른 사용자의 기본 그룹으로 이전. */
    @PutMapping("/transfer")
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<Void>> transfer(@Valid @RequestBody WorkListTransferRequest request) {
        workListService.transfer(request);
        return ResponseEntity.ok(ApiResponse.success("권한이양이 완료되었습니다.", null));
    }

    /** 결재요청 — FWK_SETTLEMENT 레코드 생성 후 선택 항목 APPROVAL_SEQ 갱신. */
    @PostMapping("/approval")
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<Void>> createApproval(@Valid @RequestBody WorkListApprovalRequest request) {
        CustomUserDetails currentUser = SecurityUtil.getCurrentUser();
        workListService.createApproval(request, currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("결재요청이 완료되었습니다.", null));
    }

    /** 이행스크립트 생성 — 원본 테이블 데이터로 DELETE+INSERT SQL 파일 생성 후 FILE_NAME 등록. */
    @PostMapping("/script/{workSeq}/generate")
    @PreAuthorize("hasAuthority('WORK_TASK:W')")
    public ResponseEntity<ApiResponse<WorkListScriptResponse>> generateScript(@PathVariable int workSeq) {
        WorkListScriptResponse result = workListScriptService.generateAndSave(workSeq);
        return ResponseEntity.ok(ApiResponse.success("이행스크립트가 생성되었습니다.", result));
    }

    /** 이행스크립트 내용 조회. */
    @GetMapping("/script/{workSeq}")
    public ResponseEntity<ApiResponse<WorkListScriptResponse>> getScript(@PathVariable int workSeq) {
        return ResponseEntity.ok(ApiResponse.success(workListScriptService.getContent(workSeq)));
    }

    /** 이행스크립트 파일 다운로드. */
    @GetMapping("/script/{workSeq}/download")
    public ResponseEntity<Resource> downloadScript(@PathVariable int workSeq) {
        Path filePath = workListScriptService.getFilePath(workSeq);
        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                .body(resource);
    }
}

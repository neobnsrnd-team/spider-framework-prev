package com.example.spider_admin.domain.reactcmsadminapproval.controller;

import com.example.spider_admin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalApproveRequest;
import com.example.spider_admin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalHistoryResponse;
import com.example.spider_admin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalListRequest;
import com.example.spider_admin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalListResponse;
import com.example.spider_admin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalPublicStateRequest;
import com.example.spider_admin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalRejectRequest;
import com.example.spider_admin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalRollbackRequest;
import com.example.spider_admin.domain.reactcmsadminapproval.service.ReactCmsAdminApprovalService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.security.CustomUserDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * React CMS Admin 승인 관리 API 컨트롤러
 *
 * <p>API 엔드포인트:
 * <ul>
 *   <li>GET   /api/react-cms-admin/approval                              — 승인 관리 목록 조회</li>
 *   <li>POST  /api/react-cms-admin/pages/{pageId}/approval/approve       — 승인</li>
 *   <li>POST  /api/react-cms-admin/pages/{pageId}/approval/reject        — 반려</li>
 *   <li>PATCH /api/react-cms-admin/pages/{pageId}/public-state           — 공개 상태 변경</li>
 *   <li>GET   /api/react-cms-admin/pages/{pageId}/approval-history       — 승인 이력 조회</li>
 *   <li>POST  /api/react-cms-admin/pages/{pageId}/rollback               — 롤백</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReactCmsAdminApprovalController {

    private final ReactCmsAdminApprovalService reactCmsAdminApprovalService;

    /** 승인 관리 목록 조회 (REACT_CMS:R) */
    @GetMapping("/api/react-cms-admin/approval")
    @PreAuthorize("hasAuthority('REACT_CMS:R')")
    public ResponseEntity<ApiResponse<PageResponse<ReactCmsAdminApprovalListResponse>>> findPageList(
            @ModelAttribute ReactCmsAdminApprovalListRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest =
                PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();

        return ResponseEntity.ok(ApiResponse.success(reactCmsAdminApprovalService.findPageList(req, pageRequest)));
    }

    /** 승인 확정 — APPROVE_STATE: PENDING → APPROVED (REACT_CMS:W) */
    @PostMapping("/api/react-cms-admin/pages/{pageId}/approval/approve")
    @PreAuthorize("hasAuthority('REACT_CMS:W')")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable String pageId,
            @RequestBody ReactCmsAdminApprovalApproveRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        reactCmsAdminApprovalService.approve(pageId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("승인이 완료되었습니다.", null));
    }

    /** 반려 — APPROVE_STATE: PENDING → REJECTED (REACT_CMS:W) */
    @PostMapping("/api/react-cms-admin/pages/{pageId}/approval/reject")
    @PreAuthorize("hasAuthority('REACT_CMS:W')")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable String pageId,
            @RequestBody ReactCmsAdminApprovalRejectRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        reactCmsAdminApprovalService.reject(pageId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("반려가 완료되었습니다.", null));
    }

    /** 공개 상태 변경 (REACT_CMS:W) */
    @PatchMapping("/api/react-cms-admin/pages/{pageId}/public-state")
    @PreAuthorize("hasAuthority('REACT_CMS:W')")
    public ResponseEntity<ApiResponse<Void>> updatePublicState(
            @PathVariable String pageId,
            @RequestBody ReactCmsAdminApprovalPublicStateRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        reactCmsAdminApprovalService.updatePublicState(pageId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("공개 상태가 변경되었습니다.", null));
    }

    /** 승인 이력 조회 (REACT_CMS:R) */
    @GetMapping("/api/react-cms-admin/pages/{pageId}/approval-history")
    @PreAuthorize("hasAuthority('REACT_CMS:R')")
    public ResponseEntity<ApiResponse<List<ReactCmsAdminApprovalHistoryResponse>>> findHistoryList(
            @PathVariable String pageId) {

        return ResponseEntity.ok(ApiResponse.success(reactCmsAdminApprovalService.findHistoryList(pageId)));
    }

    /** 롤백 — 지정 버전으로 복원 후 APPROVE_STATE → WORK (REACT_CMS:W) */
    @PostMapping("/api/react-cms-admin/pages/{pageId}/rollback")
    @PreAuthorize("hasAuthority('REACT_CMS:W')")
    public ResponseEntity<ApiResponse<Void>> rollback(
            @PathVariable String pageId,
            @RequestBody ReactCmsAdminApprovalRollbackRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        reactCmsAdminApprovalService.rollback(pageId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("롤백이 완료되었습니다.", null));
    }
}

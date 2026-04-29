package com.example.spider_admin.domain.cmsapproval.controller;

import com.example.spider_admin.domain.cmsapproval.dto.CmsApprovalHistoryResponse;
import com.example.spider_admin.domain.cmsapproval.dto.CmsApprovalListRequest;
import com.example.spider_admin.domain.cmsapproval.dto.CmsApprovalPageResponse;
import com.example.spider_admin.domain.cmsapproval.dto.CmsApproveRequest;
import com.example.spider_admin.domain.cmsapproval.dto.CmsDisplayPeriodRequest;
import com.example.spider_admin.domain.cmsapproval.dto.CmsPublicStateRequest;
import com.example.spider_admin.domain.cmsapproval.dto.CmsRejectRequest;
import com.example.spider_admin.domain.cmsapproval.dto.CmsRollbackRequest;
import com.example.spider_admin.domain.cmsapproval.service.CmsApprovalService;
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
 * CMS 승인 관리 API 컨트롤러
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *   <li>GET  /api/cms-admin/approvals                              — 승인 관리 목록</li>
 *   <li>POST /api/cms-admin/pages/{pageId}/approval/approve        — 승인</li>
 *   <li>POST /api/cms-admin/pages/{pageId}/approval/reject         — 반려</li>
 *   <li>GET  /api/cms-admin/pages/{pageId}/approval-history        — 이력 조회</li>
 *   <li>POST /api/cms-admin/pages/{pageId}/rollback                — 롤백</li>
 *   <li>PATCH /api/cms-admin/pages/{pageId}/public-state           — 공개 상태 변경</li>
 *   <li>PATCH /api/cms-admin/pages/{pageId}/display-period         — 노출 기간 수정</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CmsApprovalController {

    private final CmsApprovalService cmsApprovalService;

    /** 승인 관리 목록 조회 (CMS:W) */
    @GetMapping("/api/cms-admin/approvals")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<PageResponse<CmsApprovalPageResponse>>> findPageList(
            @ModelAttribute CmsApprovalListRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest =
                PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();

        return ResponseEntity.ok(ApiResponse.success(cmsApprovalService.findPageList(req, pageRequest)));
    }

    /** 승인 확정 — APPROVE_STATE: PENDING → APPROVED (CMS:W) */
    @PostMapping("/api/cms-admin/pages/{pageId}/approval/approve")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable String pageId,
            @RequestBody CmsApproveRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsApprovalService.approve(pageId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("승인이 완료되었습니다.", null));
    }

    /** 반려 — APPROVE_STATE: PENDING → REJECTED (CMS:W) */
    @PostMapping("/api/cms-admin/pages/{pageId}/approval/reject")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable String pageId,
            @RequestBody CmsRejectRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsApprovalService.reject(pageId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("반려가 완료되었습니다.", null));
    }

    /** 승인 이력 조회 (CMS:W) */
    @GetMapping("/api/cms-admin/pages/{pageId}/approval-history")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<List<CmsApprovalHistoryResponse>>> findHistoryList(@PathVariable String pageId) {

        return ResponseEntity.ok(ApiResponse.success(cmsApprovalService.findHistoryList(pageId)));
    }

    /** 롤백 — 지정 버전으로 복원 후 APPROVE_STATE → WORK (CMS:W) */
    @PostMapping("/api/cms-admin/pages/{pageId}/rollback")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> rollback(
            @PathVariable String pageId,
            @RequestBody CmsRollbackRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsApprovalService.rollback(pageId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("롤백이 완료되었습니다.", null));
    }

    /** 공개 상태 변경 (CMS:W) */
    @PatchMapping("/api/cms-admin/pages/{pageId}/public-state")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> updatePublicState(
            @PathVariable String pageId,
            @RequestBody CmsPublicStateRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsApprovalService.updatePublicState(pageId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("공개 상태가 변경되었습니다.", null));
    }

    /** 노출 기간 수정 (CMS:W) */
    @PatchMapping("/api/cms-admin/pages/{pageId}/display-period")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> updateDisplayPeriod(
            @PathVariable String pageId,
            @RequestBody CmsDisplayPeriodRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsApprovalService.updateDisplayPeriod(pageId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("노출 기간이 수정되었습니다.", null));
    }
}

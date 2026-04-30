package com.example.spideradmin.domain.reactcmsdashboard.controller;

import com.example.spideradmin.domain.reactcmsdashboard.dto.ReactCmsApprovalStatusResponse;
import com.example.spideradmin.domain.reactcmsdashboard.dto.ReactCmsDashboardApproveRequestDto;
import com.example.spideradmin.domain.reactcmsdashboard.dto.ReactCmsDashboardListRequest;
import com.example.spideradmin.domain.reactcmsdashboard.dto.ReactCmsDashboardPageResponse;
import com.example.spideradmin.domain.reactcmsdashboard.service.ReactCmsDashboardService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * React CMS 사용자 대시보드 API 컨트롤러
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *   <li>GET    /api/react-cms-dashboard/pages                                  — 내 페이지 목록</li>
 *   <li>GET    /api/react-cms-dashboard/pages/{pageId}/approval-status         — 승인 상태 조회</li>
 *   <li>DELETE /api/react-cms-dashboard/pages/{pageId}                         — 페이지 삭제</li>
 *   <li>PATCH  /api/react-cms-dashboard/pages/{pageId}/approve-request         — 승인 요청</li>
 * </ul>
 *
 * <p>새 페이지 생성은 react-cms 빌더(/react-cms/builder)에서 직접 수행하므로 POST 엔드포인트가 없다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReactCmsDashboardController {

    private final ReactCmsDashboardService reactCmsDashboardService;

    /** 내 페이지 목록 조회 (REACT_CMS:R) */
    @GetMapping("/api/react-cms-dashboard/pages")
    @PreAuthorize("hasAuthority('REACT_CMS:R')")
    public ResponseEntity<ApiResponse<PageResponse<ReactCmsDashboardPageResponse>>> findMyPageList(
            @ModelAttribute ReactCmsDashboardListRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PageRequest pageRequest =
                PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();

        return ResponseEntity.ok(ApiResponse.success(
                reactCmsDashboardService.findMyPageList(req, userDetails.getUserId(), pageRequest)));
    }

    /** 승인 상태 조회 — react-cms 빌더가 편집 모드 진입 시 호출 (REACT_CMS:R) */
    @GetMapping("/api/react-cms-dashboard/pages/{pageId}/approval-status")
    @PreAuthorize("hasAuthority('REACT_CMS:R')")
    public ResponseEntity<ApiResponse<ReactCmsApprovalStatusResponse>> findApprovalStatus(@PathVariable String pageId) {

        return ResponseEntity.ok(ApiResponse.success(reactCmsDashboardService.findApprovalStatus(pageId)));
    }

    /**
     * 페이지 삭제 (REACT_CMS:W)
     * 이력 있으면 소프트(USE_YN='N'), 없으면 하드 삭제.
     */
    @DeleteMapping("/api/react-cms-dashboard/pages/{pageId}")
    @PreAuthorize("hasAuthority('REACT_CMS:W')")
    public ResponseEntity<ApiResponse<Void>> deletePage(
            @PathVariable String pageId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        reactCmsDashboardService.deletePage(pageId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("페이지가 삭제되었습니다.", null));
    }

    /** 승인 요청 — APPROVE_STATE → PENDING (REACT_CMS:W) */
    @PatchMapping("/api/react-cms-dashboard/pages/{pageId}/approve-request")
    @PreAuthorize("hasAuthority('REACT_CMS:W')")
    public ResponseEntity<ApiResponse<Void>> requestApproval(
            @PathVariable String pageId,
            @RequestBody ReactCmsDashboardApproveRequestDto req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        reactCmsDashboardService.requestApproval(pageId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("승인 요청이 완료되었습니다.", null));
    }
}

package com.example.spider_admin.domain.reactcmsadmindeployment.controller;

import com.example.spider_admin.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployHistoryRequest;
import com.example.spider_admin.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployHistoryResponse;
import com.example.spider_admin.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployPageRequest;
import com.example.spider_admin.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployPageResponse;
import com.example.spider_admin.domain.reactcmsadmindeployment.dto.ReactCmsAdminDeployPushRequest;
import com.example.spider_admin.domain.reactcmsadmindeployment.service.ReactCmsAdminDeployService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * React CMS Admin 배포 관리 API 컨트롤러
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *   <li>GET  /api/react-cms-admin/deployments/pages  — 배포 대상 페이지 목록 (REACT, APPROVED)</li>
 *   <li>GET  /api/react-cms-admin/deployments        — 배포 이력 조회 (모달용)</li>
 *   <li>POST /api/react-cms-admin/deployments/push   — 배포 실행</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReactCmsAdminDeployController {

    private final ReactCmsAdminDeployService reactCmsAdminDeployService;

    /** 배포 대상 페이지 목록 조회 — PAGE_TYPE='REACT', APPROVE_STATE='APPROVED' (REACT_CMS:R) */
    @GetMapping("/api/react-cms-admin/deployments/pages")
    @PreAuthorize("hasAuthority('REACT_CMS:R')")
    public ResponseEntity<ApiResponse<PageResponse<ReactCmsAdminDeployPageResponse>>> findApprovedPageList(
            @ModelAttribute ReactCmsAdminDeployPageRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest =
                PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();

        return ResponseEntity.ok(
                ApiResponse.success(reactCmsAdminDeployService.findApprovedPageList(req, pageRequest)));
    }

    /** 배포 이력 조회 (모달용, pageId 필터) (REACT_CMS:R) */
    @GetMapping("/api/react-cms-admin/deployments")
    @PreAuthorize("hasAuthority('REACT_CMS:R')")
    public ResponseEntity<ApiResponse<PageResponse<ReactCmsAdminDeployHistoryResponse>>> findHistoryList(
            @ModelAttribute ReactCmsAdminDeployHistoryRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest =
                PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();

        return ResponseEntity.ok(ApiResponse.success(reactCmsAdminDeployService.findHistoryList(req, pageRequest)));
    }

    /** 배포 실행 (REACT_CMS:W) */
    @PostMapping("/api/react-cms-admin/deployments/push")
    @PreAuthorize("hasAuthority('REACT_CMS:W')")
    public ResponseEntity<ApiResponse<Void>> push(
            @Valid @RequestBody ReactCmsAdminDeployPushRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        reactCmsAdminDeployService.push(req.getPageId(), userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("배포가 완료되었습니다.", null));
    }
}

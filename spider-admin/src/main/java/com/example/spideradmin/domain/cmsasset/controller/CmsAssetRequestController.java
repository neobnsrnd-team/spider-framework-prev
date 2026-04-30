package com.example.spideradmin.domain.cmsasset.controller;

import com.example.spideradmin.domain.cmsasset.dto.CmsAssetListResponse;
import com.example.spideradmin.domain.cmsasset.dto.CmsAssetRequestListRequest;
import com.example.spideradmin.domain.cmsasset.service.CmsAssetService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현업 관리자용 — 이미지 승인 요청 API 컨트롤러.
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *   <li>GET  /api/cms-admin/asset-requests                  — 내 이미지 목록</li>
 *   <li>POST /api/cms-admin/asset-requests/{assetId}/request — WORK → PENDING</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CmsAssetRequestController {

    private final CmsAssetService cmsAssetService;

    /** 내 이미지 목록 (CREATE_USER_ID = 인증 주체) */
    @GetMapping("/api/cms-admin/asset-requests")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<PageResponse<CmsAssetListResponse>>> findMyList(
            @ModelAttribute CmsAssetRequestListRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PageRequest pageRequest =
                PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();

        return ResponseEntity.ok(
                ApiResponse.success(cmsAssetService.findMyRequestList(userDetails.getUserId(), req, pageRequest)));
    }

    /** 승인 요청 — WORK → PENDING */
    @PostMapping("/api/cms-admin/asset-requests/{assetId}/request")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> requestApproval(
            @PathVariable String assetId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsAssetService.requestApproval(assetId, userDetails.getUserId(), userDetails.getDisplayName());
        return ResponseEntity.ok(ApiResponse.success("승인 요청이 등록되었습니다.", null));
    }
}

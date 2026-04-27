package com.example.admin_demo.domain.cmsasset.controller;

import com.example.admin_demo.domain.cmsasset.client.CmsBuilderClient;
import com.example.admin_demo.domain.cmsasset.dto.CmsAssetApprovalListRequest;
import com.example.admin_demo.domain.cmsasset.dto.CmsAssetDetailResponse;
import com.example.admin_demo.domain.cmsasset.dto.CmsAssetListResponse;
import com.example.admin_demo.domain.cmsasset.dto.CmsAssetRejectRequest;
import com.example.admin_demo.domain.cmsasset.dto.CmsAssetUploadResponse;
import com.example.admin_demo.domain.cmsasset.dto.CmsAssetVisibilityUpdateRequest;
import com.example.admin_demo.domain.cmsasset.service.CmsAssetService;
import com.example.admin_demo.global.dto.ApiResponse;
import com.example.admin_demo.global.dto.PageRequest;
import com.example.admin_demo.global.dto.PageResponse;
import com.example.admin_demo.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 결재자용 — 이미지 승인 관리 API 컨트롤러.
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *   <li>GET  /api/cms-admin/asset-approvals                        — PENDING 기본 필터 목록</li>
 *   <li>GET  /api/cms-admin/asset-approvals/{assetId}              — 모달 프리뷰용 상세</li>
 *   <li>GET  /api/cms-admin/asset-approvals/{assetId}/image        — CMS 이미지 프록시 (썸네일·미리보기)</li>
 *   <li>POST /api/cms-admin/asset-approvals/{assetId}/approve      — PENDING → APPROVED</li>
 *   <li>POST /api/cms-admin/asset-approvals/{assetId}/reject       — PENDING → REJECTED + 반려 사유(선택)</li>
 *   <li>POST /api/cms-admin/asset-approvals/{assetId}/visibility   — 노출 여부(USE_YN) 변경</li>
 *   <li>POST /api/cms-admin/asset-approvals/upload                 — 관리자 직접 업로드 (즉시 APPROVED)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CmsAssetApprovalController {

    private final CmsAssetService cmsAssetService;
    private final CmsBuilderClient cmsBuilderClient;

    /** 승인 담당자가 검토할 자산 목록을 상태·검색 조건과 함께 조회한다. */
    @GetMapping("/api/cms-admin/asset-approvals")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<PageResponse<CmsAssetListResponse>>> findApprovalList(
            @ModelAttribute CmsAssetApprovalListRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest =
                PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();
        return ResponseEntity.ok(ApiResponse.success(cmsAssetService.findApprovalList(req, pageRequest)));
    }

    /**
     * CMS 이미지 프록시 — 썸네일·미리보기용.
     *
     * <p>CMS의 {@code GET /cms/api/assets/{assetId}/image} 를 중계한다.
     * CMS가 현재 파일 위치로 302 redirect 하므로 미승인·승인 상태와 무관하게 동일 URL로 동작한다.
     * 브라우저에서 직접 CMS를 호출하면 x-deploy-token 인증이 불가하므로 Admin이 중계한다.
     */
    @GetMapping("/api/cms-admin/asset-approvals/{assetId}/image")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<byte[]> proxyImage(@PathVariable String assetId) {
        ResponseEntity<byte[]> cmsResponse = cmsBuilderClient.fetchImage(assetId);
        byte[] body = cmsResponse.getBody();
        if (body == null || !cmsResponse.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.notFound().build();
        }
        String contentType = cmsResponse.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(body);
    }

    /** 이미지 상세 (프리뷰 모달용) */
    @GetMapping("/api/cms-admin/asset-approvals/{assetId}")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<CmsAssetDetailResponse>> findDetail(@PathVariable String assetId) {
        return ResponseEntity.ok(ApiResponse.success(cmsAssetService.findById(assetId)));
    }

    /** 승인 대기 자산을 승인하고 CMS Builder 배포까지 연계한다. */
    @PostMapping("/api/cms-admin/asset-approvals/{assetId}/approve")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable String assetId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsAssetService.approve(assetId, userDetails.getUserId(), userDetails.getDisplayName());
        return ResponseEntity.ok(ApiResponse.success("승인이 완료되었습니다.", null));
    }

    /** 승인 대기 자산을 반려하고 검토 사유를 남긴다. */
    @PostMapping("/api/cms-admin/asset-approvals/{assetId}/reject")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable String assetId,
            @RequestBody CmsAssetRejectRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsAssetService.reject(assetId, req.getRejectedReason(), userDetails.getUserId(), userDetails.getDisplayName());
        return ResponseEntity.ok(ApiResponse.success("반려가 완료되었습니다.", null));
    }

    /** 자산의 실제 노출 가능 여부를 켜거나 끈다. */
    @PostMapping("/api/cms-admin/asset-approvals/{assetId}/visibility")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> updateVisibility(
            @PathVariable String assetId,
            @RequestBody CmsAssetVisibilityUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsAssetService.updateVisibility(
                assetId, req.getUseYn(), userDetails.getUserId(), userDetails.getDisplayName());
        return ResponseEntity.ok(ApiResponse.success("노출 여부가 변경되었습니다.", null));
    }

    /**
     * 관리자가 즉시 사용 가능한 자산을 업로드한다.
     *
     * <p>일반 사용자 업로드와 달리 승인 대기 없이 바로 승인 및 배포 흐름으로 연결된다.
     */
    @PostMapping(value = "/api/cms-admin/asset-approvals/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<CmsAssetUploadResponse>> uploadApproved(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "businessCategory", required = false) String businessCategory,
            @RequestParam(value = "assetDesc", required = false) String assetDesc,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        CmsAssetUploadResponse response = cmsAssetService.uploadApprovedAsset(
                file, businessCategory, assetDesc, userDetails.getUserId(), userDetails.getDisplayName());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("관리자 이미지 업로드가 완료되었습니다.", response));
    }
}

package com.example.admin_demo.domain.cmsasset.controller;

import com.example.admin_demo.domain.cmsasset.dto.CmsAssetUploadResponse;
import com.example.admin_demo.domain.cmsasset.service.CmsAssetService;
import com.example.admin_demo.global.dto.ApiResponse;
import com.example.admin_demo.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 현업 관리자용 — 이미지 업로드 API 컨트롤러 (Issue #65).
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *   <li>POST /api/cms-admin/asset-uploads (multipart/form-data) — CMS Builder 로 포워딩</li>
 * </ul>
 *
 * <p>Admin 은 파일을 저장하지 않고 CMS 로 위임한다. 업로더 정보(userId/userName)는
 * {@code @AuthenticationPrincipal} 에서 추출하며 클라이언트 입력은 신뢰하지 않는다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CmsAssetUploadController {

    private final CmsAssetService cmsAssetService;

    /** 이미지 업로드 (CMS Builder 포워딩) */
    @PostMapping(value = "/api/cms-admin/asset-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<CmsAssetUploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "assetName", required = false) String assetName,
            @RequestParam(value = "businessCategory", required = false) String businessCategory,
            @RequestParam(value = "assetDesc", required = false) String assetDesc,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // assetName이 blank이면 원본 파일명으로 폴백하여 CMS에 항상 non-null 값을 전달
        String resolvedAssetName = (assetName != null && !assetName.isBlank()) ? assetName : file.getOriginalFilename();

        CmsAssetUploadResponse response = cmsAssetService.uploadAsset(
                file,
                resolvedAssetName,
                businessCategory,
                assetDesc,
                userDetails.getUserId(),
                userDetails.getDisplayName());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("이미지 업로드가 완료되었습니다.", response));
    }
}

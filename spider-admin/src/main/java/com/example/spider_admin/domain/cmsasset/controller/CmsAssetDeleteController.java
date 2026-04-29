package com.example.spider_admin.domain.cmsasset.controller;

import com.example.spider_admin.domain.cmsasset.service.CmsAssetService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현업 관리자용 — 이미지 자산 삭제 API 컨트롤러 (Issue #88).
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *   <li>DELETE /api/cms-admin/asset-requests/{assetId} — WORK/REJECTED 상태 자산 삭제</li>
 * </ul>
 *
 * <p>Admin 은 DB 를 조작하지 않고 CMS 의 DELETE API 로 위임한다. 삭제 수행자 정보는
 * {@code @AuthenticationPrincipal} 에서 추출하며 클라이언트 입력은 신뢰하지 않는다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CmsAssetDeleteController {

    private final CmsAssetService cmsAssetService;

    /** 이미지 자산 삭제 — WORK/REJECTED 상태에서만 가능 */
    @DeleteMapping("/api/cms-admin/asset-requests/{assetId}")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String assetId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsAssetService.deleteMyAsset(assetId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("이미지가 삭제되었습니다.", null));
    }
}

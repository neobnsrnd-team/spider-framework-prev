package com.example.spideradmin.domain.cmsasset.controller;

import com.example.spideradmin.domain.cmsasset.service.CmsAssetService;
import com.example.spideradmin.domain.code.dto.CodeResponse;
import com.example.spideradmin.global.dto.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CMS 자산 업로드 화면에서 사용하는 카테고리 코드를 제공하는 컨트롤러.
 *
 * <p>카테고리 정의를 별도 하드코딩하지 않고 공통 코드 체계에서 조회해
 * 업로드 화면과 운영 정책이 동일한 기준을 사용하도록 하기 위해 필요하다.
 */
@RestController
@RequiredArgsConstructor
public class CmsAssetCategoryController {

    private final CmsAssetService cmsAssetService;

    /** 활성화된 CMS 자산 카테고리 목록을 조회한다. */
    @GetMapping("/api/cms-admin/asset-categories")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<List<CodeResponse>>> getAssetCategories() {
        return ResponseEntity.ok(ApiResponse.success(cmsAssetService.getAssetCategoryCodes()));
    }
}

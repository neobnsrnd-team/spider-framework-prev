package com.example.spider_admin.domain.cmsabtest.controller;

import com.example.spider_admin.domain.cmsabtest.dto.CmsAbGroupResponse;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbGroupSaveRequest;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbPromoteRequest;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbTestDashboardResponse;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbTestListRequest;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbWeightUpdateRequest;
import com.example.spider_admin.domain.cmsabtest.service.CmsAbTestService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CMS A/B 테스트 그룹을 조회하고 편집하는 관리 API 컨트롤러.
 *
 * <p>승인된 CMS 페이지를 실험군으로 묶어 운영하려면 목록 조회, 그룹 저장, 가중치 조정,
 * 우승안 승격 같은 작업을 한 흐름으로 제공해야 한다.
 * 이 컨트롤러는 CMS 운영 화면과 A/B 테스트 도메인 서비스를 연결하는 진입점이다.
 */
@RestController
@RequiredArgsConstructor
public class CmsAbTestController {

    private final CmsAbTestService cmsAbTestService;

    /** A/B 테스트 대시보드에 필요한 페이지 목록과 그룹 현황을 함께 조회한다. */
    @GetMapping("/api/cms-admin/ab-tests")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<CmsAbTestDashboardResponse>> findDashboard(
            @ModelAttribute CmsAbTestListRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest =
                PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();
        return ResponseEntity.ok(ApiResponse.success(cmsAbTestService.findDashboard(req, pageRequest)));
    }

    /** 특정 그룹의 구성 페이지와 가중치를 조회해 편집 화면의 초기값으로 사용한다. */
    @GetMapping("/api/cms-admin/ab-tests/{groupId}")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<CmsAbGroupResponse>> findGroup(@PathVariable String groupId) {
        return ResponseEntity.ok(ApiResponse.success(cmsAbTestService.findGroup(groupId)));
    }

    /** 그룹을 신규 생성하거나 기존 그룹 구성을 요청 값으로 덮어쓴다. */
    @PostMapping("/api/cms-admin/ab-tests")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> saveGroup(
            @RequestBody CmsAbGroupSaveRequest req, @AuthenticationPrincipal CustomUserDetails userDetails) {
        cmsAbTestService.saveGroup(req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("A/B group saved.", null));
    }

    /** 기존 그룹에 속한 페이지들의 노출 가중치만 수정한다. */
    @PatchMapping("/api/cms-admin/ab-tests/{groupId}/weights")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> updateWeights(
            @PathVariable String groupId,
            @RequestBody CmsAbWeightUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        cmsAbTestService.updateWeights(groupId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("A/B weights updated.", null));
    }

    /** 실험 종료 후 우승 페이지를 지정해 최종안으로 승격한다. */
    @PostMapping("/api/cms-admin/ab-tests/{groupId}/promote")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> promote(
            @PathVariable String groupId,
            @RequestBody CmsAbPromoteRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        cmsAbTestService.promote(groupId, req.getWinnerPageId(), userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("A/B winner promoted.", null));
    }

    /** 그룹 단위로 A/B 연결을 모두 해제해 실험을 초기화한다. */
    @DeleteMapping(value = "/api/cms-admin/ab-tests", params = "groupId")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> clearGroup(
            @RequestParam String groupId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        cmsAbTestService.clearGroup(groupId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("A/B group cleared.", null));
    }

    /** 특정 페이지만 그룹에서 제외해 부분 정리를 수행한다. */
    @DeleteMapping(value = "/api/cms-admin/ab-tests", params = "pageId")
    @PreAuthorize("hasAuthority('CMS:W')")
    public ResponseEntity<ApiResponse<Void>> clearPage(
            @RequestParam String pageId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        cmsAbTestService.clearPage(pageId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("A/B page cleared.", null));
    }
}

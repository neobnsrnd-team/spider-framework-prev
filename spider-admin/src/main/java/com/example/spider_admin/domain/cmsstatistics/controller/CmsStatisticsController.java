package com.example.spider_admin.domain.cmsstatistics.controller;

import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsDetailRequest;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsDetailResponse;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsRequest;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsResponse;
import com.example.spider_admin.domain.cmsstatistics.service.CmsStatisticsService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CMS 통계 조회 API 컨트롤러
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *   <li>GET /api/cms-admin/statistics        — 페이지별 조회/클릭 통계 목록 (페이지네이션)</li>
 *   <li>GET /api/cms-admin/statistics/detail — 컴포넌트별 클릭 수 상세 (모달용)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class CmsStatisticsController {

    private final CmsStatisticsService cmsStatisticsService;

    /** 페이지별 통계 목록 조회 (CMS:R) */
    @GetMapping("/api/cms-admin/statistics")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<PageResponse<CmsStatisticsResponse>>> findStatList(
            @ModelAttribute CmsStatisticsRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest =
                PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();
        return ResponseEntity.ok(ApiResponse.success(cmsStatisticsService.findStatList(req, pageRequest)));
    }

    /** 컴포넌트별 클릭 수 상세 조회 (모달용, CMS:R) */
    @GetMapping("/api/cms-admin/statistics/detail")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<List<CmsStatisticsDetailResponse>>> findDetailList(
            @ModelAttribute CmsStatisticsDetailRequest req) {

        return ResponseEntity.ok(ApiResponse.success(cmsStatisticsService.findDetailList(req)));
    }
}

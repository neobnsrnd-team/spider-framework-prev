package com.example.spideradmin.domain.cmsdashboard.controller;

import com.example.spideradmin.domain.cmsdashboard.dto.CmsDashboardApproveRequestDto;
import com.example.spideradmin.domain.cmsdashboard.dto.CmsDashboardCreateRequest;
import com.example.spideradmin.domain.cmsdashboard.dto.CmsDashboardCreateResponse;
import com.example.spideradmin.domain.cmsdashboard.dto.CmsDashboardListRequest;
import com.example.spideradmin.domain.cmsdashboard.dto.CmsDashboardPageResponse;
import com.example.spideradmin.domain.cmsdashboard.dto.CmsTemplateResponse;
import com.example.spideradmin.domain.cmsdashboard.service.CmsDashboardService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.security.CustomUserDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * CMS 사용자 대시보드 API 컨트롤러
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *   <li>GET    /api/cms-dashboard/templates                          — 템플릿 목록</li>
 *   <li>GET    /api/cms-dashboard/pages                              — 내 페이지 목록</li>
 *   <li>POST   /api/cms-dashboard/pages                              — 새 페이지 생성</li>
 *   <li>DELETE /api/cms-dashboard/pages/{pageId}                     — 페이지 삭제</li>
 *   <li>PATCH  /api/cms-dashboard/pages/{pageId}/approve-request     — 승인 요청</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CmsDashboardController {

    private final CmsDashboardService cmsDashboardService;

    @Value("${cms.user-url}")
    private String cmsUserUrl;

    /** 템플릿 목록 조회 — 페이지 생성 모달 선택 목록용 (PAGE_TYPE = 'TEMPLATE', USE_YN = 'Y') */
    @GetMapping("/api/cms-dashboard/templates")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<List<CmsTemplateResponse>>> findTemplateList() {
        return ResponseEntity.ok(ApiResponse.success(cmsDashboardService.findTemplateList()));
    }

    /** 내 페이지 목록 조회 (CMS:R 권한 보유 사용자) */
    @GetMapping("/api/cms-dashboard/pages")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<PageResponse<CmsDashboardPageResponse>>> findMyPageList(
            @ModelAttribute CmsDashboardListRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PageRequest pageRequest =
                PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();

        return ResponseEntity.ok(
                ApiResponse.success(cmsDashboardService.findMyPageList(req, userDetails.getUserId(), pageRequest)));
    }

    /**
     * 새 페이지 생성 (CMS:R 권한 보유 사용자)
     * 생성 후 editorUrl({CMS_USER_URL}/cms/edit?bank={pageId})을 반환한다.
     */
    @PostMapping("/api/cms-dashboard/pages")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<CmsDashboardCreateResponse>> createPage(
            @RequestBody CmsDashboardCreateRequest req, @AuthenticationPrincipal CustomUserDetails userDetails) {

        String pageId = cmsDashboardService.createPage(req, userDetails.getUserId(), userDetails.getDisplayName());

        // 말미 슬래시 정규화 후 편집 URL 조합
        String editorUrl = cmsUserUrl.replaceAll("/$", "") + "/cms/edit?bank=" + pageId;

        return ResponseEntity.ok(ApiResponse.success(
                "페이지가 생성되었습니다.",
                CmsDashboardCreateResponse.builder()
                        .pageId(pageId)
                        .editorUrl(editorUrl)
                        .build()));
    }

    /**
     * 페이지 삭제 (CMS:R 권한 보유 사용자, 본인 페이지 한정)
     * 이력 있으면 소프트(USE_YN='N'), 없으면 하드 삭제.
     */
    @DeleteMapping("/api/cms-dashboard/pages/{pageId}")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<Void>> deletePage(
            @PathVariable String pageId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsDashboardService.deletePage(pageId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("페이지가 삭제되었습니다.", null));
    }

    /** 승인 요청 — APPROVE_STATE → PENDING (CMS:R 권한 보유 사용자, 본인 페이지 한정) */
    @PatchMapping("/api/cms-dashboard/pages/{pageId}/approve-request")
    @PreAuthorize("hasAuthority('CMS:R')")
    public ResponseEntity<ApiResponse<Void>> requestApproval(
            @PathVariable String pageId,
            @RequestBody CmsDashboardApproveRequestDto req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        cmsDashboardService.requestApproval(pageId, req, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("승인 요청이 완료되었습니다.", null));
    }
}

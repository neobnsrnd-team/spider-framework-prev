package com.example.spider_admin.domain.cmsapproval.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.cmsapproval.dto.CmsApprovalHistoryResponse;
import com.example.spider_admin.domain.cmsapproval.dto.CmsApprovalPageResponse;
import com.example.spider_admin.domain.cmsapproval.service.CmsApprovalService;
import com.example.spider_admin.domain.user.enums.UserState;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.security.CustomUserDetails;
import com.example.spider_admin.global.security.dto.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CmsApprovalController.class)
@DisplayName("CmsApprovalController 테스트")
class CmsApprovalControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CmsApprovalService cmsApprovalService;

    private static final String PAGE_ID = "PAGE-001";
    private static final String APPROVALS_URL = "/api/cms-admin/approvals";
    private static final String PAGE_URL = "/api/cms-admin/pages/" + PAGE_ID;

    // ─── GET /api/cms-admin/approvals ─────────────────────────────────

    @Test
    @WithMockUser(authorities = "CMS:W")
    @DisplayName("[조회] CMS:W 권한으로 목록 조회 시 200과 PageResponse를 반환한다")
    void findPageList_withCmsW_returns200() throws Exception {
        PageResponse<CmsApprovalPageResponse> page = PageResponse.of(List.of(buildPageResponse()), 1L, 0, 10);
        given(cmsApprovalService.findPageList(any(), any())).willReturn(page);

        mockMvc.perform(get(APPROVALS_URL).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "CMS:W")
    @DisplayName("[조회] CMS:W 권한으로 결과가 없으면 200과 빈 목록을 반환한다")
    void findPageList_emptyResult_returns200() throws Exception {
        given(cmsApprovalService.findPageList(any(), any())).willReturn(PageResponse.of(List.of(), 0L, 0, 10));

        mockMvc.perform(get(APPROVALS_URL).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("[인증] 비인증 목록 조회 시 401을 반환한다")
    void findPageList_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(APPROVALS_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"CMS:R", "CMS:W"})
    @DisplayName("[인가] CMS:R과 CMS:W를 함께 가진 경우 목록 조회 시 200을 반환한다")
    void findPageList_withDerivedCmsW_returns200() throws Exception {
        given(cmsApprovalService.findPageList(any(), any())).willReturn(PageResponse.of(List.of(), 0L, 0, 10));

        mockMvc.perform(get(APPROVALS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ─── GET /api/cms-admin/pages/{pageId}/approval-history ──────────

    @Test
    @WithMockUser(authorities = "CMS:W")
    @DisplayName("[이력조회] CMS:W 권한으로 이력 조회 시 200을 반환한다")
    void findHistoryList_withCmsW_returns200() throws Exception {
        given(cmsApprovalService.findHistoryList(PAGE_ID)).willReturn(List.of(buildHistoryResponse()));

        mockMvc.perform(get(PAGE_URL + "/approval-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(authorities = "CMS:W")
    @DisplayName("[이력조회] CMS:W 권한으로 페이지가 없으면 404를 반환한다")
    void findHistoryList_pageNotFound_returns404() throws Exception {
        willThrow(new NotFoundException("pageId: " + PAGE_ID))
                .given(cmsApprovalService)
                .findHistoryList(PAGE_ID);

        mockMvc.perform(get(PAGE_URL + "/approval-history")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[인증] 비인증 이력 조회 시 401을 반환한다")
    void findHistoryList_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(PAGE_URL + "/approval-history")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[권한] CMS:R 권한으로 목록 조회 시 403을 반환한다")
    void findPageList_withCmsR_returns403() throws Exception {
        mockMvc.perform(get(APPROVALS_URL)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[권한] CMS:R 권한으로 이력 조회 시 403을 반환한다")
    void findHistoryList_withCmsR_returns403() throws Exception {
        mockMvc.perform(get(PAGE_URL + "/approval-history")).andExpect(status().isForbidden());
    }

    // ─── POST /api/cms-admin/pages/{pageId}/approval/approve ─────────

    @Test
    @DisplayName("[승인] CMS:W 권한으로 승인 시 200을 반환한다")
    void approve_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsApprovalService).approve(any(), any(), any());

        mockMvc.perform(post(PAGE_URL + "/approval/approve")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("beginningDate", "2099-04-17", "expiredDate", "2099-04-18"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("[승인] 페이지가 없으면 404를 반환한다")
    void approve_pageNotFound_returns404() throws Exception {
        willThrow(new NotFoundException("pageId: " + PAGE_ID))
                .given(cmsApprovalService)
                .approve(any(), any(), any());

        mockMvc.perform(post(PAGE_URL + "/approval/approve")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("beginningDate", "2099-04-17", "expiredDate", "2099-04-18"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[인증] 비인증 승인 시 401을 반환한다")
    void approve_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(PAGE_URL + "/approval/approve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("beginningDate", "2099-04-17", "expiredDate", "2099-04-18"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[권한] CMS:R 권한으로 승인 시 403을 반환한다")
    void approve_withCmsR_returns403() throws Exception {
        mockMvc.perform(post(PAGE_URL + "/approval/approve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isForbidden());
    }

    // ─── POST /api/cms-admin/pages/{pageId}/approval/reject ──────────

    @Test
    @DisplayName("[반려] CMS:W 권한으로 반려 시 200을 반환한다")
    void reject_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsApprovalService).reject(any(), any(), any());

        mockMvc.perform(post(PAGE_URL + "/approval/reject")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rejectedReason", "내용 부적합"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("[반려] 페이지가 없으면 404를 반환한다")
    void reject_pageNotFound_returns404() throws Exception {
        willThrow(new NotFoundException("pageId: " + PAGE_ID))
                .given(cmsApprovalService)
                .reject(any(), any(), any());

        mockMvc.perform(post(PAGE_URL + "/approval/reject")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rejectedReason", "이유"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[인증] 비인증 반려 요청 시 401을 반환한다")
    void reject_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(PAGE_URL + "/approval/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rejectedReason", "이유"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[권한] CMS:R 권한으로 반려 요청 시 403을 반환한다")
    void reject_withCmsR_returns403() throws Exception {
        mockMvc.perform(post(PAGE_URL + "/approval/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rejectedReason", "이유"))))
                .andExpect(status().isForbidden());
    }

    // ─── POST /api/cms-admin/pages/{pageId}/rollback ─────────────────

    @Test
    @DisplayName("[롤백] CMS:W 권한으로 롤백 시 200을 반환한다")
    void rollback_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsApprovalService).rollback(any(), any(), any());

        mockMvc.perform(post(PAGE_URL + "/rollback")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("version", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("[롤백] 버전이 없으면 404를 반환한다")
    void rollback_historyNotFound_returns404() throws Exception {
        willThrow(new NotFoundException("version not found"))
                .given(cmsApprovalService)
                .rollback(any(), any(), any());

        mockMvc.perform(post(PAGE_URL + "/rollback")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("version", 99))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[인증] 비인증 롤백 요청 시 401을 반환한다")
    void rollback_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post(PAGE_URL + "/rollback")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("version", 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[권한] CMS:R 권한으로 롤백 요청 시 403을 반환한다")
    void rollback_withCmsR_returns403() throws Exception {
        mockMvc.perform(post(PAGE_URL + "/rollback")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("version", 1))))
                .andExpect(status().isForbidden());
    }

    // ─── PATCH /api/cms-admin/pages/{pageId}/public-state ────────────

    @Test
    @DisplayName("[공개상태] CMS:W 권한으로 변경 시 200을 반환한다")
    void updatePublicState_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsApprovalService).updatePublicState(any(), any(), any());

        mockMvc.perform(patch(PAGE_URL + "/public-state")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isPublic", "Y"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("[공개상태] 페이지가 없으면 404를 반환한다")
    void updatePublicState_pageNotFound_returns404() throws Exception {
        willThrow(new NotFoundException("pageId: " + PAGE_ID))
                .given(cmsApprovalService)
                .updatePublicState(any(), any(), any());

        mockMvc.perform(patch(PAGE_URL + "/public-state")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isPublic", "N"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[인증] 비인증 공개상태 변경 시 401을 반환한다")
    void updatePublicState_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch(PAGE_URL + "/public-state")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isPublic", "Y"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[인가] CMS:R 권한으로 공개상태 변경 시 403을 반환한다")
    void updatePublicState_withCmsR_returns403() throws Exception {
        mockMvc.perform(patch(PAGE_URL + "/public-state")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("isPublic", "Y"))))
                .andExpect(status().isForbidden());
    }

    // ─── PATCH /api/cms-admin/pages/{pageId}/display-period ──────────

    @Test
    @DisplayName("[노출기간] CMS:W 권한으로 수정 시 200을 반환한다")
    void updateDisplayPeriod_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsApprovalService).updateDisplayPeriod(any(), any(), any());

        mockMvc.perform(patch(PAGE_URL + "/display-period")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("beginningDate", "2026-04-01", "expiredDate", "2026-12-31"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("[노출기간] 페이지가 없으면 404를 반환한다")
    void updateDisplayPeriod_pageNotFound_returns404() throws Exception {
        willThrow(new NotFoundException("pageId: " + PAGE_ID))
                .given(cmsApprovalService)
                .updateDisplayPeriod(any(), any(), any());

        mockMvc.perform(patch(PAGE_URL + "/display-period")
                        .with(csrf())
                        .with(user(customUserDetails("CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("beginningDate", "2026-04-01", "expiredDate", "2026-12-31"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[인증] 비인증 노출기간 수정 시 401을 반환한다")
    void updateDisplayPeriod_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch(PAGE_URL + "/display-period")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("beginningDate", "2026-04-01", "expiredDate", "2026-12-31"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[권한] CMS:R 권한으로 노출 기간 수정 요청 시 403을 반환한다")
    void updateDisplayPeriod_withCmsR_returns403() throws Exception {
        mockMvc.perform(patch(PAGE_URL + "/display-period")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("beginningDate", "2026-04-01", "expiredDate", "2026-12-31"))))
                .andExpect(status().isForbidden());
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private CustomUserDetails customUserDetails(String authority) {
        AuthenticatedUser user = new AuthenticatedUser("test-admin", "테스트 관리자", "ROLE_ADMIN", "pwd", UserState.NORMAL);
        return new CustomUserDetails(user, Set.of(new SimpleGrantedAuthority(authority)));
    }

    private CmsApprovalPageResponse buildPageResponse() {
        return CmsApprovalPageResponse.builder()
                .pageId(PAGE_ID)
                .pageName("테스트 페이지")
                .approveState("PENDING")
                .displayState("PENDING")
                .beginningDate("2099-04-17")
                .expiredDate("2099-04-18")
                .isPublic("Y")
                .build();
    }

    private CmsApprovalHistoryResponse buildHistoryResponse() {
        return CmsApprovalHistoryResponse.builder()
                .version(1)
                .approveState("APPROVED")
                .lastModifierName("홍길동")
                .approveDate("2026-04-01 10:00:00")
                .build();
    }
}

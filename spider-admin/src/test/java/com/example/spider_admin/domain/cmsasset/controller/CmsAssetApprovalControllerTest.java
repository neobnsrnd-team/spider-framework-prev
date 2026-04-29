package com.example.spider_admin.domain.cmsasset.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.cmsasset.client.CmsBuilderClient;
import com.example.spider_admin.domain.cmsasset.dto.CmsAssetDetailResponse;
import com.example.spider_admin.domain.cmsasset.dto.CmsAssetListResponse;
import com.example.spider_admin.domain.cmsasset.service.CmsAssetService;
import com.example.spider_admin.domain.user.enums.UserState;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InvalidStateException;
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

@WebMvcTest(CmsAssetApprovalController.class)
@DisplayName("CmsAssetApprovalController 테스트")
class CmsAssetApprovalControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CmsAssetService cmsAssetService;

    /** @WebMvcTest 컨텍스트에서 CmsBuilderClient의 RestClient 빈을 사용할 수 없으므로 mock 처리 */
    @MockitoBean
    private CmsBuilderClient cmsBuilderClient;

    private static final String ASSET_ID = "ASSET-001";
    private static final String BASE_URL = "/api/cms-admin/asset-approvals";

    // ─── GET /asset-approvals ──────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[조회] CMS:R 권한 시 200 + PageResponse 반환")
    void findApprovalList_withCmsR_returns200() throws Exception {
        PageResponse<CmsAssetListResponse> page = PageResponse.of(
                List.of(CmsAssetListResponse.builder()
                        .assetId(ASSET_ID)
                        .assetState("PENDING")
                        .build()),
                1L,
                0,
                10);
        given(cmsAssetService.findApprovalList(any(), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "OTHER:R")
    @DisplayName("[조회] 권한 없으면 403")
    void findApprovalList_noCmsR_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isForbidden());
    }

    // ─── GET /{assetId} ────────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[상세 조회] 정상 시 200")
    void findDetail_returns200() throws Exception {
        given(cmsAssetService.findById(ASSET_ID))
                .willReturn(CmsAssetDetailResponse.builder()
                        .assetId(ASSET_ID)
                        .assetName("test.png")
                        .assetState("PENDING")
                        .build());

        mockMvc.perform(get(BASE_URL + "/" + ASSET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetId").value(ASSET_ID));
    }

    @Test
    @WithMockUser(authorities = "CMS:R")
    @DisplayName("[상세 조회] 존재하지 않으면 404")
    void findDetail_notFound_returns404() throws Exception {
        given(cmsAssetService.findById(any())).willThrow(new NotFoundException("not found"));

        mockMvc.perform(get(BASE_URL + "/" + ASSET_ID)).andExpect(status().isNotFound());
    }

    // ─── POST /{assetId}/approve ───────────────────────────────────────

    @Test
    @DisplayName("[승인] CMS:W 권한 시 200")
    void approve_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsAssetService).approve(eq(ASSET_ID), eq("cmsAdmin01"), any());

        mockMvc.perform(post(BASE_URL + "/" + ASSET_ID + "/approve")
                        .with(csrf())
                        .with(user(customUserDetails("cmsAdmin01", "CMS:W"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("[승인] CMS:R 만 있으면 403")
    void approve_onlyCmsR_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + ASSET_ID + "/approve")
                        .with(csrf())
                        .with(user(customUserDetails("cmsAdmin01", "CMS:R"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[승인] 이미 승인된 건이면 409")
    void approve_alreadyApproved_returns409() throws Exception {
        willThrow(new InvalidStateException("already approved"))
                .given(cmsAssetService)
                .approve(any(), any(), any());

        mockMvc.perform(post(BASE_URL + "/" + ASSET_ID + "/approve")
                        .with(csrf())
                        .with(user(customUserDetails("cmsAdmin01", "CMS:W"))))
                .andExpect(status().isConflict());
    }

    // ─── POST /{assetId}/reject ────────────────────────────────────────

    @Test
    @DisplayName("[반려] 반려 사유 포함 시 200")
    void reject_withReason_returns200() throws Exception {
        willDoNothing().given(cmsAssetService).reject(eq(ASSET_ID), eq("사유"), eq("cmsAdmin01"), any());

        mockMvc.perform(post(BASE_URL + "/" + ASSET_ID + "/reject")
                        .with(csrf())
                        .with(user(customUserDetails("cmsAdmin01", "CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rejectedReason", "사유"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("[반려] 빈 바디(JSON {}) 도 허용 (반려 사유는 선택)")
    void reject_emptyBody_returns200() throws Exception {
        willDoNothing().given(cmsAssetService).reject(eq(ASSET_ID), any(), eq("cmsAdmin01"), any());

        mockMvc.perform(post(BASE_URL + "/" + ASSET_ID + "/reject")
                        .with(csrf())
                        .with(user(customUserDetails("cmsAdmin01", "CMS:W")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[반려] CMS:R만 있으면 403")
    void reject_onlyCmsR_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + ASSET_ID + "/reject")
                        .with(csrf())
                        .with(user(customUserDetails("cmsAdmin01", "CMS:R")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private CustomUserDetails customUserDetails(String userId, String authority) {
        AuthenticatedUser user = new AuthenticatedUser(userId, userId + "님", "ROLE_ADMIN", "pwd", UserState.NORMAL);
        return new CustomUserDetails(user, Set.of(new SimpleGrantedAuthority(authority)));
    }
}

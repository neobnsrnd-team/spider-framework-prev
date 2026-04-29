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

import com.example.spider_admin.domain.cmsasset.dto.CmsAssetListResponse;
import com.example.spider_admin.domain.cmsasset.service.CmsAssetService;
import com.example.spider_admin.domain.user.enums.UserState;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InvalidStateException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.security.CustomUserDetails;
import com.example.spider_admin.global.security.dto.AuthenticatedUser;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CmsAssetRequestController.class)
@DisplayName("CmsAssetRequestController 테스트")
class CmsAssetRequestControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CmsAssetService cmsAssetService;

    private static final String ASSET_ID = "ASSET-001";
    private static final String BASE_URL = "/api/cms-admin/asset-requests";

    // ─── GET /api/cms-admin/asset-requests ─────────────────────────────

    @Test
    @DisplayName("[조회] CMS:R 권한 + 인증주체 주입 시 200과 PageResponse 반환")
    void findMyList_withCmsR_returns200() throws Exception {
        PageResponse<CmsAssetListResponse> page = PageResponse.of(
                List.of(CmsAssetListResponse.builder()
                        .assetId(ASSET_ID)
                        .assetState("WORK")
                        .build()),
                1L,
                0,
                10);
        given(cmsAssetService.findMyRequestList(eq("cmsUser01"), any(), any())).willReturn(page);

        mockMvc.perform(get(BASE_URL).with(user(customUserDetails("cmsUser01", "CMS:R"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "OTHER:R")
    @DisplayName("[조회] 권한 없으면 403")
    void findMyList_noCmsR_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL)).andExpect(status().isForbidden());
    }

    // ─── POST /api/cms-admin/asset-requests/{assetId}/request ──────────

    @Test
    @DisplayName("[승인 요청] CMS:W 권한이면 200")
    void requestApproval_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsAssetService).requestApproval(eq(ASSET_ID), eq("cmsUser01"), any());

        mockMvc.perform(post(BASE_URL + "/" + ASSET_ID + "/request")
                        .with(csrf())
                        .with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("승인 요청이 등록되었습니다."));
    }

    @Test
    @DisplayName("[승인 요청] CMS:R 만 있으면 403")
    void requestApproval_onlyCmsR_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + ASSET_ID + "/request")
                        .with(csrf())
                        .with(user(customUserDetails("cmsUser01", "CMS:R"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[승인 요청] 존재하지 않는 asset이면 404")
    void requestApproval_notFound_returns404() throws Exception {
        willThrow(new NotFoundException("not found")).given(cmsAssetService).requestApproval(any(), any(), any());

        mockMvc.perform(post(BASE_URL + "/" + ASSET_ID + "/request")
                        .with(csrf())
                        .with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[승인 요청] 상태 전이 불가 시 409")
    void requestApproval_invalidState_returns409() throws Exception {
        willThrow(new InvalidStateException("already pending"))
                .given(cmsAssetService)
                .requestApproval(any(), any(), any());

        mockMvc.perform(post(BASE_URL + "/" + ASSET_ID + "/request")
                        .with(csrf())
                        .with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isConflict());
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private CustomUserDetails customUserDetails(String userId, String authority) {
        AuthenticatedUser user = new AuthenticatedUser(userId, userId + "님", "ROLE_USER", "pwd", UserState.NORMAL);
        return new CustomUserDetails(user, Set.of(new SimpleGrantedAuthority(authority)));
    }
}

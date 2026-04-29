package com.example.spider_admin.domain.cmsasset.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.cmsasset.service.CmsAssetService;
import com.example.spider_admin.domain.user.enums.UserState;
import com.example.spider_admin.global.exception.ErrorType;
import com.example.spider_admin.global.exception.InvalidStateException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.exception.base.BaseException;
import com.example.spider_admin.global.security.CustomUserDetails;
import com.example.spider_admin.global.security.dto.AuthenticatedUser;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CmsAssetDeleteController Web 계층 테스트 — Issue #88.
 *
 * <p>엔드포인트: DELETE /api/cms-admin/asset-requests/{assetId}
 * <p>권한 게이트(CMS:W), 상태 가드, 미존재, CMS 외부 오류의 HTTP 응답을 검증한다.
 */
@WebMvcTest(CmsAssetDeleteController.class)
@DisplayName("CmsAssetDeleteController 테스트 (#88)")
class CmsAssetDeleteControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CmsAssetService cmsAssetService;

    private static final String BASE_URL = "/api/cms-admin/asset-requests/";
    private static final String ASSET_ID = "uuid-del-1";

    @Test
    @DisplayName("[삭제] CMS:W 권한 + 정상 상태 → 200")
    void delete_withCmsW_returns200() throws Exception {
        willDoNothing().given(cmsAssetService).deleteMyAsset(eq(ASSET_ID), eq("cmsUser01"));

        mockMvc.perform(delete(BASE_URL + ASSET_ID).with(csrf()).with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("이미지가 삭제되었습니다."));
    }

    @Test
    @DisplayName("[삭제] CMS:R 만 있으면 403")
    void delete_onlyCmsR_returns403() throws Exception {
        mockMvc.perform(delete(BASE_URL + ASSET_ID).with(csrf()).with(user(customUserDetails("cmsUser01", "CMS:R"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[삭제] 삭제 불가 상태(PENDING/APPROVED) → 409 Conflict")
    void delete_invalidState_returns409() throws Exception {
        willThrow(new InvalidStateException("현재 상태에서는 삭제할 수 없습니다."))
                .given(cmsAssetService)
                .deleteMyAsset(any(), any());

        mockMvc.perform(delete(BASE_URL + ASSET_ID).with(csrf()).with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("[삭제] 소유자가 아닌 요청 → 403 Forbidden")
    void delete_notOwner_returns403() throws Exception {
        willThrow(new BaseException(ErrorType.FORBIDDEN, "본인이 업로드한 이미지만 삭제할 수 있습니다."))
                .given(cmsAssetService)
                .deleteMyAsset(any(), any());

        mockMvc.perform(delete(BASE_URL + ASSET_ID).with(csrf()).with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[삭제] 존재하지 않는 assetId → 404")
    void delete_notFound_returns404() throws Exception {
        willThrow(new NotFoundException("이미지를 찾을 수 없습니다."))
                .given(cmsAssetService)
                .deleteMyAsset(any(), any());

        mockMvc.perform(delete(BASE_URL + ASSET_ID).with(csrf()).with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[삭제] CMS 서버 오류 → 502")
    void delete_externalServiceError_returns502() throws Exception {
        willThrow(new BaseException(ErrorType.EXTERNAL_SERVICE_ERROR, "CMS 통신 오류"))
                .given(cmsAssetService)
                .deleteMyAsset(any(), any());

        mockMvc.perform(delete(BASE_URL + ASSET_ID).with(csrf()).with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isBadGateway());
    }

    private CustomUserDetails customUserDetails(String userId, String authority) {
        AuthenticatedUser user = new AuthenticatedUser(userId, userId + "님", "ROLE_USER", "pwd", UserState.NORMAL);
        return new CustomUserDetails(user, Set.of(new SimpleGrantedAuthority(authority)));
    }
}

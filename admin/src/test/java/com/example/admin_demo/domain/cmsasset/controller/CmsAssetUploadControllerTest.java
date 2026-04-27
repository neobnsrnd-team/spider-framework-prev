package com.example.admin_demo.domain.cmsasset.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.admin_demo.domain.cmsasset.dto.CmsAssetUploadResponse;
import com.example.admin_demo.domain.cmsasset.service.CmsAssetService;
import com.example.admin_demo.domain.user.enums.UserState;
import com.example.admin_demo.global.exception.ErrorType;
import com.example.admin_demo.global.exception.InvalidInputException;
import com.example.admin_demo.global.exception.base.BaseException;
import com.example.admin_demo.global.security.CustomUserDetails;
import com.example.admin_demo.global.security.dto.AuthenticatedUser;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CmsAssetUploadController.class)
@DisplayName("CmsAssetUploadController 테스트")
class CmsAssetUploadControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CmsAssetService cmsAssetService;

    private static final String URL = "/api/cms-admin/asset-uploads";

    @Test
    @DisplayName("[업로드] CMS:W 권한 + 정상 파일 → 201")
    void upload_withCmsW_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2, 3});
        given(cmsAssetService.uploadAsset(any(), any(), any(), any(), eq("cmsUser01"), any()))
                .willReturn(CmsAssetUploadResponse.builder()
                        .assetId("uuid-1")
                        .url("/static/a.png")
                        .build());

        mockMvc.perform(multipart(URL)
                        .file(file)
                        .param("assetName", "배너이미지.png")
                        .param("businessCategory", "마케팅")
                        .with(csrf())
                        .with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assetId").value("uuid-1"))
                .andExpect(jsonPath("$.data.url").value("/static/a.png"));
    }

    @Test
    @DisplayName("[업로드] assetName 미전달 시 원본 파일명으로 폴백 → 201")
    void upload_withoutAssetName_fallsBackToOriginalFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2, 3});
        // assetName이 없으면 컨트롤러에서 getOriginalFilename()("a.png")로 폴백
        given(cmsAssetService.uploadAsset(any(), eq("a.png"), any(), any(), eq("cmsUser01"), any()))
                .willReturn(CmsAssetUploadResponse.builder()
                        .assetId("uuid-2")
                        .url("/static/a.png")
                        .build());

        mockMvc.perform(multipart(URL).file(file).with(csrf()).with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("[업로드] CMS:R 만 있으면 403")
    void upload_onlyCmsR_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1});

        mockMvc.perform(multipart(URL).file(file).with(csrf()).with(user(customUserDetails("cmsUser01", "CMS:R"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[업로드] Validator 실패 → 400")
    void upload_invalidInput_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/x-msdownload", new byte[] {1});
        willThrow(new InvalidInputException("허용하지 않는 형식"))
                .given(cmsAssetService)
                .uploadAsset(any(), any(), any(), any(), any(), any());

        mockMvc.perform(multipart(URL).file(file).with(csrf()).with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[업로드] CMS 서버 오류 → 502")
    void upload_externalServiceError_returns502() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1});
        willThrow(new BaseException(ErrorType.EXTERNAL_SERVICE_ERROR, "CMS 통신 오류"))
                .given(cmsAssetService)
                .uploadAsset(any(), any(), any(), any(), any(), any());

        mockMvc.perform(multipart(URL).file(file).with(csrf()).with(user(customUserDetails("cmsUser01", "CMS:W"))))
                .andExpect(status().isBadGateway());
    }

    private CustomUserDetails customUserDetails(String userId, String authority) {
        AuthenticatedUser user = new AuthenticatedUser(userId, userId + "님", "ROLE_USER", "pwd", UserState.NORMAL);
        return new CustomUserDetails(user, Set.of(new SimpleGrantedAuthority(authority)));
    }
}

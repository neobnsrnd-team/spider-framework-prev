package com.example.spider_admin.global.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spider_admin.domain.user.enums.UserState;
import com.example.spider_admin.domain.user.mapper.UserMapper;
import com.example.spider_admin.domain.user.service.UserService;
import com.example.spider_admin.global.auth.dto.CmsApproverResponse;
import com.example.spider_admin.global.auth.service.AuthService;
import com.example.spider_admin.global.security.CustomUserDetails;
import com.example.spider_admin.global.security.dto.AuthenticatedUser;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController current user API")
class AuthControllerCurrentUserTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    @DisplayName("GET /api/auth/me returns the authenticated user and authorities")
    void me_returnsCurrentUser() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(
                new AuthenticatedUser("admin", "Admin", "ADMIN", "{noop}password", UserState.NORMAL),
                Set.of(new SimpleGrantedAuthority("CMS:W"), new SimpleGrantedAuthority("CMS:R")));

        mockMvc.perform(get("/api/auth/me").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("admin"))
                .andExpect(jsonPath("$.data.userName").value("Admin"))
                .andExpect(jsonPath("$.data.roleId").value("ADMIN"))
                .andExpect(jsonPath("$.data.authorities[0]").value("CMS:R"))
                .andExpect(jsonPath("$.data.authorities[1]").value("CMS:W"));
    }

    @Test
    @DisplayName("GET /api/auth/me requires authentication")
    void me_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/auth/cms-approvers returns CMS approvers")
    void cmsApprovers_returnsApproverList() throws Exception {
        CustomUserDetails userDetails = new CustomUserDetails(
                new AuthenticatedUser("worker", "Worker", "cms_user", "{noop}password", UserState.NORMAL),
                Set.of(new SimpleGrantedAuthority("CMS:W")));
        given(userMapper.findCmsApprovers())
                .willReturn(List.of(CmsApproverResponse.builder()
                        .userId("cmsAdmin01")
                        .userName("CMS 관리자")
                        .build()));

        mockMvc.perform(get("/api/auth/cms-approvers").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value("cmsAdmin01"))
                .andExpect(jsonPath("$.data[0].userName").value("CMS 관리자"));
    }
}

package com.example.spider_admin.global.page.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.spider_admin.domain.user.enums.UserState;
import com.example.spider_admin.global.security.CustomUserDetails;
import com.example.spider_admin.global.security.dto.AuthenticatedUser;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CmsRedirectController.class)
@TestPropertySource(
        properties = {"cms.app-base-url=http://localhost:9000/cms", "cms.user-url=http://133.186.135.23:3001/"})
@DisplayName("CMS root redirect")
class CmsRedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("cms_admin role redirects /cms/user-dashboard to /cms-admin/approvals")
    void cmsRoot_cmsAdminRole_redirectsApprovals() throws Exception {
        mockMvc.perform(get("/cms/user-dashboard").with(user(userDetails("admin", "cms_admin", "CMS:W"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cms-admin/approvals"));
    }

    @Test
    @DisplayName("ADMIN role redirects /cms/user-dashboard to /cms-admin/approvals")
    void cmsRoot_adminRole_redirectsApprovals() throws Exception {
        mockMvc.perform(get("/cms/user-dashboard").with(user(userDetails("admin", "ADMIN", "CMS:W"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cms-admin/approvals"));
    }

    @Test
    @DisplayName("cms_user role — 직접 브라우저 접근 시 CMS_USER_URL로 redirect")
    void cmsRoot_cmsUserRole_directAccess_redirectsConfiguredCmsUserUrl() throws Exception {
        mockMvc.perform(get("/cms/user-dashboard").with(user(userDetails("worker", "cms_user", "CMS:R"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://133.186.135.23:3001/"));
    }

    @Test
    @DisplayName("cms_user role — 탭 AJAX 요청 시 cms-user-redirect 뷰 반환")
    void cmsRoot_cmsUserRole_tabRequest_returnsCmsUserRedirectView() throws Exception {
        mockMvc.perform(get("/cms/user-dashboard")
                        .header("X-Tab-Request", "true")
                        .with(user(userDetails("worker", "cms_user", "CMS:R"))))
                .andExpect(status().isOk())
                .andExpect(view().name("cms-user-redirect"));
    }

    @Test
    @DisplayName("/cms/user-dashboard requires authentication")
    void cmsRoot_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/cms/user-dashboard")).andExpect(status().isUnauthorized());
    }

    private CustomUserDetails userDetails(String userId, String roleId, String authority) {
        return new CustomUserDetails(
                new AuthenticatedUser(userId, userId, roleId, "{noop}password", UserState.NORMAL),
                Set.of(new SimpleGrantedAuthority(authority)));
    }
}

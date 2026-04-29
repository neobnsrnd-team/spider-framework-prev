package com.example.spider_admin.global.auth.dto;

import com.example.spider_admin.domain.menu.dto.MenuResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for login response
 * Contains user info and accessible menus
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String userId;
    private String userName;
    private String email;
    private String roleId;
    private List<MenuResponse> menus; // 권한별 메뉴 목록
    private String accessToken; // JWT 토큰 (필요시)
}

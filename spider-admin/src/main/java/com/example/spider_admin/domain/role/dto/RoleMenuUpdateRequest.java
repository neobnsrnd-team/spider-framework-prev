package com.example.spider_admin.domain.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuUpdateRequest {

    @NotNull(message = "메뉴 권한 목록은 필수입니다")
    private List<MenuPermissionRequest> menuPermissions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuPermissionRequest {

        @NotBlank(message = "Menu ID는 필수입니다")
        private String menuId;

        private Boolean read;

        private Boolean write;
    }
}

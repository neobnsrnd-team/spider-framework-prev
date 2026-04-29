package com.example.spideradmin.domain.menu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMenuBatchSaveRequest {

    @NotNull
    private List<MenuItem> menus;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuItem {
        private String menuId;

        @Pattern(regexp = "^[RrWw]$", message = "권한코드는 R 또는 W여야 합니다")
        private String authCode;
    }
}

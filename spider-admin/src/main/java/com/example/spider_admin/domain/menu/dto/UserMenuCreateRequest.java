package com.example.spider_admin.domain.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating user-menu mapping
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMenuCreateRequest {

    @NotBlank(message = "사용자 ID는 필수입니다.")
    @Size(max = 50, message = "사용자 ID는 50자 이내여야 합니다.")
    private String userId;

    @NotBlank(message = "메뉴 ID는 필수입니다.")
    @Size(max = 40, message = "메뉴 ID는 40자 이내여야 합니다.")
    private String menuId;

    @NotBlank(message = "권한코드는 필수입니다.")
    @Pattern(regexp = "^[RrWw]$", message = "권한코드는 R 또는 W만 허용됩니다.")
    private String authCode;

    private Integer favorMenuOrder;
}

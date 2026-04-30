package com.example.spideradmin.domain.role.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Role-Menu Permission
 * Represents menu permission status for a specific role
 * Used in role-menu management modal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuPermissionResponse {

    /**
     * Menu ID
     */
    private String menuId;

    /**
     * Menu name (from FwkMenu)
     */
    private String menuName;

    /**
     * Has Read permission (AUTH_CODE = 'R')
     */
    private Boolean hasRead;

    /**
     * Has Write permission (AUTH_CODE = 'W')
     */
    private Boolean hasWrite;

    /**
     * Menu description (from FwkMenu.menuUrl or other field)
     */
    private String description;

    /**
     * Menu hierarchy path (e.g. "시스템관리 > 사용자관리 > 사용자목록")
     * Built from SYS_CONNECT_BY_PATH
     */
    private String menuPath;

    /**
     * Leaf node flag ('Y' = leaf menu, 'N' = parent menu)
     */
    private String leafYn;
}

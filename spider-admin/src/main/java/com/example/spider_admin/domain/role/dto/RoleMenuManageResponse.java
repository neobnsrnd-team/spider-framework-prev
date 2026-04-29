package com.example.spider_admin.domain.role.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Role-Menu Management Response
 * Contains all menu permissions for a specific role
 * Used to populate the role-menu management modal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMenuManageResponse {

    /**
     * Role ID
     */
    private String roleId;

    /**
     * Role name
     */
    private String roleName;

    /**
     * Left table: Menus with at least one permission assigned
     * These are the menus currently assigned to this role
     */
    private List<RoleMenuPermissionResponse> assignedMenus;

    /**
     * Right table: All available menus with their permission status
     * Shows all menus in the system with checkboxes for Read/Write permissions
     */
    private List<RoleMenuPermissionResponse> allMenusWithPermissions;
}

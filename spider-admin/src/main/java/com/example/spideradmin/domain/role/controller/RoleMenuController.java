package com.example.spideradmin.domain.role.controller;

import com.example.spideradmin.domain.role.dto.RoleMenuManageResponse;
import com.example.spideradmin.domain.role.dto.RoleMenuUpdateRequest;
import com.example.spideradmin.domain.role.service.RoleMenuService;
import com.example.spideradmin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/role-menus")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE:R')")
public class RoleMenuController {

    private final RoleMenuService roleMenuService;

    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleMenuManageResponse>> getRoleMenuPermissions(@PathVariable String roleId) {
        return ResponseEntity.ok(ApiResponse.success(roleMenuService.getRoleMenuPermissions(roleId)));
    }

    @PostMapping("/{roleId}")
    @PreAuthorize("hasAuthority('ROLE:W')")
    public ResponseEntity<ApiResponse<Void>> updateRoleMenuPermissions(
            @PathVariable String roleId, @Valid @RequestBody RoleMenuUpdateRequest request) {
        roleMenuService.updateRoleMenuPermissions(roleId, request);
        return ResponseEntity.ok(ApiResponse.success("권한이 수정되었습니다", null));
    }
}

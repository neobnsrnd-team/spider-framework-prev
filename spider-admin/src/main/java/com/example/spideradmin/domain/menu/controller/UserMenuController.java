package com.example.spideradmin.domain.menu.controller;

import com.example.spideradmin.domain.menu.dto.MenuHierarchyResponse;
import com.example.spideradmin.domain.menu.dto.UserMenuBatchSaveRequest;
import com.example.spideradmin.domain.menu.dto.UserMenuCreateRequest;
import com.example.spideradmin.domain.menu.dto.UserMenuResponse;
import com.example.spideradmin.domain.menu.service.MenuService;
import com.example.spideradmin.domain.menu.service.UserMenuService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.util.AuditUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for User-Menu mapping management
 */
@RestController
@RequestMapping("/api/user-menus")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER:R')")
public class UserMenuController {

    private final UserMenuService userMenuService;
    private final MenuService menuService;

    /**
     * Get current user's menu hierarchy (사이드바용)
     * GET /api/user-menus
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MenuHierarchyResponse>>> getUserMenus() {
        String currentUserId = AuditUtil.currentUserId();
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenusByUserId(currentUserId)));
    }

    /**
     * Get menu hierarchy by role
     * GET /api/user-menus/by-role/{roleId}
     */
    @GetMapping("/by-role/{roleId}")
    @PreAuthorize("hasAuthority('ROLE:R')")
    public ResponseEntity<ApiResponse<List<MenuHierarchyResponse>>> getMenusByRole(@PathVariable String roleId) {
        List<MenuHierarchyResponse> menus = menuService.getMenusByRoleId(roleId);
        return ResponseEntity.ok(ApiResponse.success(menus));
    }

    /**
     * Get user's menu assignments
     * GET /api/user-menus/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<UserMenuResponse>>> getUserMenusByUserId(@PathVariable String userId) {
        List<UserMenuResponse> userMenus = userMenuService.getUserMenusByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(userMenus));
    }

    /**
     * Create user-menu mapping
     * POST /api/user-menus
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER:W')")
    public ResponseEntity<ApiResponse<UserMenuResponse>> createUserMenu(
            @Valid @RequestBody UserMenuCreateRequest requestDTO) {
        UserMenuResponse created = userMenuService.createUserMenu(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("사용자 메뉴 매핑이 생성되었습니다.", created));
    }

    /**
     * Batch save user menu assignments (메뉴 권한 일괄 저장)
     * PUT /api/user-menus/{userId}/batch
     */
    @PutMapping("/{userId}/batch")
    @PreAuthorize("hasAuthority('USER:W')")
    public ResponseEntity<ApiResponse<Void>> batchSaveUserMenus(
            @PathVariable String userId, @RequestBody UserMenuBatchSaveRequest requestDTO) {
        userMenuService.batchSaveUserMenus(userId, requestDTO.getMenus());
        return ResponseEntity.ok(ApiResponse.success("메뉴 권한이 저장되었습니다.", null));
    }

    /**
     * Delete all menu assignments for a user (메뉴 초기화)
     * DELETE /api/user-menus/{userId}
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER:W')")
    public ResponseEntity<ApiResponse<Void>> deleteAllUserMenus(@PathVariable String userId) {
        userMenuService.deleteAllUserMenus(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 메뉴가 초기화되었습니다.", null));
    }

    /**
     * Delete user-menu mapping
     * DELETE /api/user-menus/{userId}/{menuId}
     */
    @DeleteMapping("/{userId}/{menuId}")
    @PreAuthorize("hasAuthority('USER:W')")
    public ResponseEntity<ApiResponse<Void>> deleteUserMenu(@PathVariable String userId, @PathVariable String menuId) {
        userMenuService.deleteUserMenu(userId, menuId);
        return ResponseEntity.ok(ApiResponse.success("사용자 메뉴 매핑이 삭제되었습니다.", null));
    }
}

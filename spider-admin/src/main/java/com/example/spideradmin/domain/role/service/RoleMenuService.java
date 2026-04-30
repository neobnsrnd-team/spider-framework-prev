package com.example.spideradmin.domain.role.service;

import com.example.spideradmin.domain.menu.mapper.MenuMapper;
import com.example.spideradmin.domain.role.dto.RoleMenuManageResponse;
import com.example.spideradmin.domain.role.dto.RoleMenuPermissionResponse;
import com.example.spideradmin.domain.role.dto.RoleMenuResponse;
import com.example.spideradmin.domain.role.dto.RoleMenuUpdateRequest;
import com.example.spideradmin.domain.role.dto.RoleResponse;
import com.example.spideradmin.domain.role.mapper.RoleMapper;
import com.example.spideradmin.domain.role.mapper.RoleMenuMapper;
import com.example.spideradmin.global.exception.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleMenuService {

    private final RoleMenuMapper roleMenuMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;

    public RoleMenuManageResponse getRoleMenuPermissions(String roleId) {
        log.info("Fetching role-menu permissions for roleId: {}", roleId);

        RoleResponse role = roleMapper.selectResponseById(roleId);
        if (role == null) {
            throw new NotFoundException("roleId: " + roleId);
        }

        List<RoleMenuPermissionResponse> allMenusWithPermissions = roleMenuMapper.findAllMenusWithPermissions(roleId);

        List<RoleMenuPermissionResponse> assignedMenus = allMenusWithPermissions.stream()
                .filter(menu -> Boolean.TRUE.equals(menu.getHasRead()) || Boolean.TRUE.equals(menu.getHasWrite()))
                .toList();

        return RoleMenuManageResponse.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .assignedMenus(assignedMenus)
                .allMenusWithPermissions(allMenusWithPermissions)
                .build();
    }

    @Transactional
    public void updateRoleMenuPermissions(String roleId, RoleMenuUpdateRequest request) {
        log.info("Updating role-menu permissions for roleId: {}", roleId);

        if (roleMapper.countByRoleId(roleId) == 0) {
            throw new NotFoundException("roleId: " + roleId);
        }

        roleMenuMapper.deleteByRoleId(roleId);

        Set<String> menuIds = request.getMenuPermissions().stream()
                .map(RoleMenuUpdateRequest.MenuPermissionRequest::getMenuId)
                .collect(Collectors.toSet());

        Set<String> existingMenuIds =
                menuIds.isEmpty() ? new HashSet<>() : new HashSet<>(menuMapper.findExistingMenuIds(menuIds));

        if (existingMenuIds.size() != menuIds.size()) {
            Set<String> notFound = new HashSet<>(menuIds);
            notFound.removeAll(existingMenuIds);
            throw new NotFoundException("menuIds: " + notFound);
        }

        List<RoleMenuResponse> newPermissions = new ArrayList<>();

        for (RoleMenuUpdateRequest.MenuPermissionRequest menuPerm : request.getMenuPermissions()) {
            String authCode;
            if (Boolean.TRUE.equals(menuPerm.getWrite())) {
                authCode = "W";
            } else if (Boolean.TRUE.equals(menuPerm.getRead())) {
                authCode = "R";
            } else {
                continue;
            }

            RoleMenuResponse permission = RoleMenuResponse.builder()
                    .roleId(roleId)
                    .menuId(menuPerm.getMenuId())
                    .authCode(authCode)
                    .build();
            newPermissions.add(permission);
        }

        if (!newPermissions.isEmpty()) {
            roleMenuMapper.insertBatch(newPermissions);
        }

        log.info("Created {} new permissions for roleId: {}", newPermissions.size(), roleId);
    }
}

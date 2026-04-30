package com.example.spideradmin.domain.menu.service;

import com.example.spideradmin.domain.menu.dto.UserMenuBatchSaveRequest;
import com.example.spideradmin.domain.menu.dto.UserMenuCreateRequest;
import com.example.spideradmin.domain.menu.dto.UserMenuResponse;
import com.example.spideradmin.domain.menu.mapper.MenuMapper;
import com.example.spideradmin.domain.menu.mapper.UserMenuMapper;
import com.example.spideradmin.domain.role.dto.RoleMenuResponse;
import com.example.spideradmin.domain.role.mapper.RoleMenuMapper;
import com.example.spideradmin.domain.user.dto.UserResponse;
import com.example.spideradmin.domain.user.mapper.UserMapper;
import com.example.spideradmin.global.common.enums.AuthCode;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.ErrorType;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.exception.base.BaseException;
import com.example.spideradmin.global.security.CustomUserDetails;
import com.example.spideradmin.global.util.AuditUtil;
import com.example.spideradmin.global.util.SecurityUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for User-Menu mapping management
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserMenuService {

    private final UserMenuMapper userMenuMapper;
    private final UserMapper userMapper;
    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;

    @Transactional
    public UserMenuResponse createUserMenu(UserMenuCreateRequest requestDTO) {
        // 사용자 존재 확인
        if (userMapper.countByUserId(requestDTO.getUserId()) == 0) {
            throw new NotFoundException("사용자를 찾을 수 없습니다: " + requestDTO.getUserId());
        }

        // 메뉴 존재 확인
        if (menuMapper.countById(requestDTO.getMenuId()) == 0) {
            throw new NotFoundException("메뉴를 찾을 수 없습니다: " + requestDTO.getMenuId());
        }

        // 중복 확인
        if (userMenuMapper.countByUserIdAndMenuId(requestDTO.getUserId(), requestDTO.getMenuId()) > 0) {
            throw new DuplicateException("이미 존재하는 사용자-메뉴 매핑입니다.");
        }

        // 권한 코드 유효성 검사
        AuthCode authCode = AuthCode.fromCode(requestDTO.getAuthCode());
        if (authCode == null) {
            throw new InvalidInputException("유효하지 않은 권한코드입니다: " + requestDTO.getAuthCode());
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        int favorMenuOrder = requestDTO.getFavorMenuOrder() != null ? requestDTO.getFavorMenuOrder() : 0;

        userMenuMapper.insert(
                requestDTO.getUserId(), requestDTO.getMenuId(), authCode.getCode(), favorMenuOrder, now, currentUserId);

        return userMenuMapper.selectResponseById(requestDTO.getUserId(), requestDTO.getMenuId());
    }

    public List<UserMenuResponse> getUserMenusByUserId(String userId) {
        return userMenuMapper.selectByUserIdWithDetails(userId);
    }

    @Transactional
    public void deleteUserMenu(String userId, String menuId) {
        if (userMenuMapper.countByUserIdAndMenuId(userId, menuId) == 0) {
            throw new NotFoundException("사용자-메뉴 매핑을 찾을 수 없습니다: " + userId + " / " + menuId);
        }

        userMenuMapper.deleteById(userId, menuId);
    }

    @Transactional
    public void deleteAllUserMenus(String userId) {
        // 사용자 존재 확인
        UserResponse user = userMapper.selectResponseById(userId);
        if (user == null) {
            throw new NotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }

        CustomUserDetails currentUser = SecurityUtil.getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUserId() : null;
        String currentUserRoleId = currentUser != null ? currentUser.getRoleId() : null;

        if (!userId.equals(currentUserId) && !"superadmin".equals(currentUserRoleId)) {
            throw new BaseException(ErrorType.FORBIDDEN, "본인의 메뉴만 초기화할 수 있습니다.");
        }

        // 1. 기존 사용자 메뉴 전체 삭제
        userMenuMapper.deleteByUserId(userId);

        // 2. 사용자의 역할(Role) 기본 메뉴로 재설정
        List<RoleMenuResponse> roleMenus = roleMenuMapper.selectByRoleId(user.getRoleId());
        String now = AuditUtil.now();
        String auditUserId = AuditUtil.currentUserId();

        for (RoleMenuResponse roleMenu : roleMenus) {
            userMenuMapper.insert(userId, roleMenu.getMenuId(), roleMenu.getAuthCode(), 0, now, auditUserId);
        }
    }

    @Transactional
    public void batchSaveUserMenus(String userId, List<UserMenuBatchSaveRequest.MenuItem> menus) {
        if (userMapper.countByUserId(userId) == 0) {
            throw new NotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }

        userMenuMapper.deleteByUserId(userId);

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        for (UserMenuBatchSaveRequest.MenuItem item : menus) {
            AuthCode authCode = AuthCode.fromCode(item.getAuthCode());
            if (authCode == null) continue;

            userMenuMapper.insert(userId, item.getMenuId(), authCode.getCode(), 0, now, currentUserId);
        }
    }
}

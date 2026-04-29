package com.example.spider_admin.global.security.mapper;

import com.example.spider_admin.global.security.dto.MenuPermission;
import java.util.List;

public interface AuthorityMapper {

    List<MenuPermission> selectMenuPermissionsByUserId(String userId);

    List<MenuPermission> selectMenuPermissionsByRoleId(String roleId);
}

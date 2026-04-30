package com.example.spideradmin.global.security.mapper;

import com.example.spideradmin.global.security.dto.MenuPermission;
import java.util.List;

public interface AuthorityMapper {

    List<MenuPermission> selectMenuPermissionsByUserId(String userId);

    List<MenuPermission> selectMenuPermissionsByRoleId(String roleId);
}

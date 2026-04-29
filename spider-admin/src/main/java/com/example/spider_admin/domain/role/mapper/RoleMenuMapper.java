package com.example.spider_admin.domain.role.mapper;

import com.example.spider_admin.domain.role.dto.RoleMenuPermissionResponse;
import com.example.spider_admin.domain.role.dto.RoleMenuResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleMenuMapper {

    void insert(@Param("roleId") String roleId, @Param("menuId") String menuId, @Param("authCode") String authCode);

    void update(@Param("roleId") String roleId, @Param("menuId") String menuId, @Param("authCode") String authCode);

    void deleteById(@Param("roleId") String roleId, @Param("menuId") String menuId);

    void deleteByRoleId(String roleId);

    void insertBatch(List<RoleMenuResponse> roleMenus);

    List<RoleMenuResponse> selectByRoleId(String roleId);

    List<RoleMenuPermissionResponse> findAllMenusWithPermissions(@Param("roleId") String roleId);
}

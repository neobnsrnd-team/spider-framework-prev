package com.example.spideradmin.domain.role.mapper;

import com.example.spideradmin.domain.role.dto.RoleCreateRequest;
import com.example.spideradmin.domain.role.dto.RoleResponse;
import com.example.spideradmin.domain.role.dto.RoleUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleMapper {

    RoleResponse selectResponseById(String roleId);

    void insert(
            @Param("dto") RoleCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void update(
            @Param("roleId") String roleId,
            @Param("dto") RoleUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteById(String roleId);

    int countByRoleId(String roleId);

    int countByRoleName(String roleName);

    int countByRoleNameExcluding(@Param("roleName") String roleName, @Param("roleId") String roleId);

    int countUsersByRoleId(String roleId);

    List<RoleResponse> findAll();

    List<RoleResponse> findAllWithSearch(
            @Param("roleName") String roleName,
            @Param("roleId") String roleId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    long countAllWithSearch(@Param("roleName") String roleName, @Param("roleId") String roleId);

    List<RoleResponse> findAllForExport(
            @Param("roleName") String roleName,
            @Param("roleId") String roleId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}

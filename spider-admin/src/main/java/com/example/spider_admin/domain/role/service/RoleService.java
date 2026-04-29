package com.example.spider_admin.domain.role.service;

import com.example.spider_admin.domain.role.dto.RoleCreateRequest;
import com.example.spider_admin.domain.role.dto.RoleResponse;
import com.example.spider_admin.domain.role.dto.RoleSearchRequest;
import com.example.spider_admin.domain.role.dto.RoleUpdateRequest;
import com.example.spider_admin.domain.role.mapper.RoleMapper;
import com.example.spider_admin.domain.role.mapper.RoleMenuMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import com.example.spider_admin.global.util.ExcelColumnDefinition;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;

    public List<RoleResponse> getAllRoles() {
        return roleMapper.findAll();
    }

    public PageResponse<RoleResponse> getRoles(PageRequest pageRequest, RoleSearchRequest searchDTO) {
        long total = roleMapper.countAllWithSearch(searchDTO.getRoleName(), searchDTO.getRoleId());
        List<RoleResponse> roles = roleMapper.findAllWithSearch(
                searchDTO.getRoleName(), searchDTO.getRoleId(),
                pageRequest.getSortBy(), pageRequest.getSortDirection(),
                pageRequest.getOffset(), pageRequest.getEndRow());
        return PageResponse.of(roles, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public RoleResponse getRoleById(String roleId) {
        RoleResponse role = roleMapper.selectResponseById(roleId);
        if (role == null) {
            throw new NotFoundException("roleId: " + roleId);
        }
        role.setUserCount(roleMapper.countUsersByRoleId(roleId));
        return role;
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest dto) {
        if (roleMapper.countByRoleName(dto.getRoleName()) > 0) {
            throw new DuplicateException("roleName: " + dto.getRoleName());
        }
        if (dto.getUseYn() == null) {
            dto.setUseYn("Y");
        }
        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        roleMapper.insert(dto, now, currentUserId);
        return roleMapper.selectResponseById(dto.getRoleId());
    }

    @Transactional
    public RoleResponse updateRole(String roleId, RoleUpdateRequest dto) {
        if (roleMapper.countByRoleId(roleId) == 0) {
            throw new NotFoundException("roleId: " + roleId);
        }
        if (roleMapper.countByRoleNameExcluding(dto.getRoleName(), roleId) > 0) {
            throw new DuplicateException("roleName: " + dto.getRoleName());
        }
        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        roleMapper.update(roleId, dto, now, currentUserId);
        return roleMapper.selectResponseById(roleId);
    }

    @Transactional
    public void deleteRole(String roleId) {
        if (roleMapper.countByRoleId(roleId) == 0) {
            throw new NotFoundException("roleId: " + roleId);
        }
        int userCount = roleMapper.countUsersByRoleId(roleId);
        if (userCount > 0) {
            throw new InvalidInputException("roleId: " + roleId + ", userCount: " + userCount);
        }
        roleMenuMapper.deleteByRoleId(roleId);
        roleMapper.deleteById(roleId);
    }

    public byte[] exportRoles(String roleName, String roleId, String sortBy, String sortDirection) {
        List<RoleResponse> data = roleMapper.findAllForExport(roleName, roleId, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("권한ID", 15, "roleId"),
                new ExcelColumnDefinition("권한명", 25, "roleName"),
                new ExcelColumnDefinition("사용여부", 10, "useYn"),
                new ExcelColumnDefinition("권한 설명", 30, "roleDesc"),
                new ExcelColumnDefinition("Ranking", 10, "ranking"),
                new ExcelColumnDefinition("사용자 수", 10, "userCount"),
                new ExcelColumnDefinition("최종 수정 일시", 20, "lastUpdateDtime"),
                new ExcelColumnDefinition("최종 수정자", 15, "lastUpdateUserId"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (RoleResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("roleId", item.getRoleId());
            row.put("roleName", item.getRoleName());
            row.put("useYn", item.getUseYn());
            row.put("roleDesc", item.getRoleDesc());
            row.put("ranking", item.getRanking());
            row.put("userCount", item.getUserCount());
            row.put("lastUpdateDtime", item.getLastUpdateDtime());
            row.put("lastUpdateUserId", item.getLastUpdateUserId());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("권한", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    @Transactional
    public BatchResult batchOperations(
            List<RoleCreateRequest> newRoles, List<RoleUpdateRequest> updatedRoles, List<String> deletedRoleIds) {
        int created = batchCreate(newRoles);
        int updated = batchUpdate(updatedRoles);
        int deleted = batchDelete(deletedRoleIds);
        return new BatchResult(created, updated, deleted);
    }

    private int batchCreate(List<RoleCreateRequest> newRoles) {
        if (newRoles == null || newRoles.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (RoleCreateRequest dto : newRoles) {
            if (roleMapper.countByRoleName(dto.getRoleName()) > 0) {
                throw new DuplicateException("roleName: " + dto.getRoleName());
            }
            if (dto.getUseYn() == null) {
                dto.setUseYn("Y");
            }
            String now = AuditUtil.now();
            String currentUserId = AuditUtil.currentUserId();
            roleMapper.insert(dto, now, currentUserId);
            count++;
        }
        return count;
    }

    private int batchUpdate(List<RoleUpdateRequest> updatedRoles) {
        if (updatedRoles == null || updatedRoles.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (RoleUpdateRequest dto : updatedRoles) {
            if (roleMapper.countByRoleId(dto.getRoleId()) == 0) {
                throw new NotFoundException("roleId: " + dto.getRoleId());
            }
            if (roleMapper.countByRoleNameExcluding(dto.getRoleName(), dto.getRoleId()) > 0) {
                throw new DuplicateException("roleName: " + dto.getRoleName());
            }
            String now = AuditUtil.now();
            String currentUserId = AuditUtil.currentUserId();
            roleMapper.update(dto.getRoleId(), dto, now, currentUserId);
            count++;
        }
        return count;
    }

    private int batchDelete(List<String> deletedRoleIds) {
        if (deletedRoleIds == null || deletedRoleIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String roleId : deletedRoleIds) {
            if (roleMapper.countByRoleId(roleId) == 0) {
                throw new NotFoundException("roleId: " + roleId);
            }
            int userCount = roleMapper.countUsersByRoleId(roleId);
            if (userCount > 0) {
                throw new InvalidInputException("roleId: " + roleId + ", userCount: " + userCount);
            }
            roleMenuMapper.deleteByRoleId(roleId);
            roleMapper.deleteById(roleId);
            count++;
        }
        return count;
    }

    public record BatchResult(int created, int updated, int deleted) {
        public String toMessage() {
            return String.format("배치 작업 완료 - 생성: %d개, 수정: %d개, 삭제: %d개", created, updated, deleted);
        }
    }
}

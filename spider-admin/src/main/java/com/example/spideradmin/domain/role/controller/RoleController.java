package com.example.spideradmin.domain.role.controller;

import com.example.spideradmin.domain.role.dto.BatchRoleRequest;
import com.example.spideradmin.domain.role.dto.RoleResponse;
import com.example.spideradmin.domain.role.dto.RoleSearchRequest;
import com.example.spideradmin.domain.role.service.RoleService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Role management
 * Provides AJAX endpoints for role CRUD operations
 *
 * Note: Exception handling is delegated to GlobalExceptionHandler
 *
 * @PreAuthorize 적용: 역할 관리 메뉴(ROLE) 권한 검사
 * - 클래스 레벨: 읽기 권한(ROLE:R) 기본 적용
 * - 생성/수정/삭제 API: 쓰기 권한(ROLE:W) 메서드 레벨 오버라이드
 */
@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE:R')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleService.getAllRoles()));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<RoleResponse>>> getRolesWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleId) {

        log.info("GET /api/roles/page - page: {}, size: {}, roleName: {}, roleId: {}", page, size, roleName, roleId);

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        RoleSearchRequest searchDTO =
                RoleSearchRequest.builder().roleName(roleName).roleId(roleId).build();

        return ResponseEntity.ok(ApiResponse.success(roleService.getRoles(pageRequest, searchDTO)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportRoles(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleId) {

        byte[] excelBytes = roleService.exportRoles(roleName, roleId, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("Role", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('ROLE:W')")
    public ResponseEntity<ApiResponse<String>> batchOperations(@Valid @RequestBody BatchRoleRequest request) {
        RoleService.BatchResult result = roleService.batchOperations(
                request.getNewRoles(), request.getUpdatedRoles(), request.getDeletedRoleIds());
        String message = result.toMessage();
        return ResponseEntity.ok(ApiResponse.success(message, message));
    }
}

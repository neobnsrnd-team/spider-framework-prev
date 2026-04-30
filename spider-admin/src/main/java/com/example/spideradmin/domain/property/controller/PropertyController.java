package com.example.spideradmin.domain.property.controller;

import com.example.spideradmin.domain.property.dto.PropertyGroupCreateRequest;
import com.example.spideradmin.domain.property.dto.PropertyGroupResponse;
import com.example.spideradmin.domain.property.dto.PropertyResponse;
import com.example.spideradmin.domain.property.dto.PropertySaveRequest;
import com.example.spideradmin.domain.property.service.PropertyService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 프로퍼티 관리 REST Controller
 */
@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PROPERTY_DB:R')")
public class PropertyController {

    private final PropertyService propertyService;

    /**
     * 프로퍼티 그룹 목록 조회 (페이징)
     * GET /api/properties/groups/page
     */
    @GetMapping("/groups/page")
    public ResponseEntity<ApiResponse<PageResponse<PropertyGroupResponse>>> getPropertyGroupsWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .searchField(searchField)
                .searchValue(searchValue)
                .build();

        PageResponse<PropertyGroupResponse> response = propertyService.getPropertyGroups(pageRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 프로퍼티 그룹 엑셀 내보내기
     * GET /api/properties/groups/export
     */
    @GetMapping("/groups/export")
    public ResponseEntity<byte[]> exportPropertyGroups(
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        byte[] excelBytes = propertyService.exportPropertyGroups(searchField, searchValue, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("Property", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * 프로퍼티 그룹 전체 목록 조회
     * GET /api/properties/groups
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<PropertyGroupResponse>>> getAllPropertyGroups() {
        List<PropertyGroupResponse> propertyGroups = propertyService.getAllPropertyGroups();
        return ResponseEntity.ok(ApiResponse.success(propertyGroups));
    }

    /**
     * 전체 프로퍼티 목록 조회
     * GET /api/properties/properties
     */
    @GetMapping("/properties")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getAllProperties(
            @RequestParam(required = false) String searchField, @RequestParam(required = false) String searchValue) {
        List<PropertyResponse> properties = propertyService.getPropertiesByGroupId(null, searchField, searchValue);
        return ResponseEntity.ok(ApiResponse.success(properties));
    }

    /**
     * 프로퍼티 그룹 ID로 프로퍼티 목록 조회
     * GET /api/properties/groups/{propertyGroupId}/properties
     */
    @GetMapping("/groups/{propertyGroupId}/properties")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getPropertiesByGroupId(
            @PathVariable String propertyGroupId,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue) {
        List<PropertyResponse> properties =
                propertyService.getPropertiesByGroupId(propertyGroupId, searchField, searchValue);
        return ResponseEntity.ok(ApiResponse.success(properties));
    }

    /**
     * 프로퍼티 그룹 ID 중복 확인
     * GET /api/properties/groups/{propertyGroupId}/exists
     */
    @GetMapping("/groups/{propertyGroupId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkPropertyGroupExists(@PathVariable String propertyGroupId) {
        boolean exists = propertyService.existsPropertyGroup(propertyGroupId);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    /**
     * 신규 프로퍼티 그룹 생성
     * POST /api/properties/groups
     */
    @PostMapping("/groups")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<Void>> createPropertyGroup(
            @Valid @RequestBody PropertyGroupCreateRequest requestDTO) {
        propertyService.createPropertyGroup(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    /**
     * 프로퍼티 일괄 저장 (생성/수정/삭제)
     * POST /api/properties/save
     */
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<Integer>> saveProperties(
            @Valid @RequestBody List<PropertySaveRequest> properties) {
        int processedCount = propertyService.saveProperties(properties);
        return ResponseEntity.ok(ApiResponse.success(processedCount));
    }

    /**
     * 프로퍼티 그룹 전체 삭제
     * DELETE /api/properties/groups/{propertyGroupId}
     */
    @DeleteMapping("/groups/{propertyGroupId}")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<Void>> deletePropertyGroup(@PathVariable String propertyGroupId) {
        propertyService.deletePropertyGroup(propertyGroupId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

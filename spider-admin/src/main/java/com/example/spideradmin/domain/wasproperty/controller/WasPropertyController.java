package com.example.spideradmin.domain.wasproperty.controller;

import com.example.spideradmin.domain.wasproperty.dto.*;
import com.example.spideradmin.domain.wasproperty.dto.WasPropertyBackupRequest;
import com.example.spideradmin.domain.wasproperty.dto.WasPropertyBatchSaveRequest;
import com.example.spideradmin.domain.wasproperty.dto.WasPropertyCompareRequest;
import com.example.spideradmin.domain.wasproperty.dto.WasPropertyCopyRequest;
import com.example.spideradmin.domain.wasproperty.dto.WasPropertyCreateRequest;
import com.example.spideradmin.domain.wasproperty.dto.WasPropertySaveRequest;
import com.example.spideradmin.domain.wasproperty.dto.WasPropertyUpdateRequest;
import com.example.spideradmin.domain.wasproperty.service.WasPropertyService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.SecurityUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/was/property")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PROPERTY_DB:R')")
public class WasPropertyController {

    private final WasPropertyService wasPropertyService;

    @GetMapping("/{instanceId}/{propertyGroupId}/{propertyId}")
    public ResponseEntity<ApiResponse<WasPropertyResponse>> getPropertyById(
            @PathVariable String instanceId, @PathVariable String propertyGroupId, @PathVariable String propertyId) {
        WasPropertyResponse property = wasPropertyService.getPropertyById(instanceId, propertyGroupId, propertyId);
        return ResponseEntity.ok(ApiResponse.success(property));
    }

    @GetMapping("/instance/{instanceId}")
    public ResponseEntity<ApiResponse<List<?>>> getPropertiesByInstance(@PathVariable String instanceId) {
        List<?> properties = wasPropertyService.getPropertiesByInstanceWithDefaults(instanceId);
        return ResponseEntity.ok(ApiResponse.success(properties));
    }

    @GetMapping("/instance/{instanceId}/page")
    public ResponseEntity<ApiResponse<PageResponse<WasPropertyWithDefaultResponse>>> getPropertiesByInstancePaging(
            @PathVariable String instanceId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String propertyGroupId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<WasPropertyWithDefaultResponse> response =
                wasPropertyService.getPropertiesByInstancePaging(instanceId, propertyGroupId, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<WasPropertyResponse>> createProperty(
            @Valid @RequestBody WasPropertyCreateRequest dto, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : SecurityUtil.SYSTEM_USER_ID;
        WasPropertyResponse created = wasPropertyService.createProperty(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("프로퍼티가 생성되었습니다", created));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<WasPropertyResponse>> updateProperty(
            @Valid @RequestBody WasPropertyUpdateRequest dto,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : SecurityUtil.SYSTEM_USER_ID;
        WasPropertyResponse updated = wasPropertyService.updateProperty(
                dto.getInstanceId(), dto.getPropertyGroupId(), dto.getPropertyId(), dto, reason, userId);
        return ResponseEntity.ok(ApiResponse.success("프로퍼티가 수정되었습니다", updated));
    }

    @PutMapping("/{instanceId}/{propertyGroupId}/{propertyId}")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<WasPropertyResponse>> updatePropertyByPath(
            @PathVariable String instanceId,
            @PathVariable String propertyGroupId,
            @PathVariable String propertyId,
            @Valid @RequestBody WasPropertyUpdateRequest dto,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : SecurityUtil.SYSTEM_USER_ID;
        WasPropertyResponse updated =
                wasPropertyService.updateProperty(instanceId, propertyGroupId, propertyId, dto, reason, userId);
        return ResponseEntity.ok(ApiResponse.success("프로퍼티가 수정되었습니다", updated));
    }

    @DeleteMapping("/{instanceId}/{propertyGroupId}/{propertyId}")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(
            @PathVariable String instanceId, @PathVariable String propertyGroupId, @PathVariable String propertyId) {
        wasPropertyService.deleteProperty(instanceId, propertyGroupId, propertyId);
        return ResponseEntity.ok(ApiResponse.success("프로퍼티가 삭제되었습니다", null));
    }

    @GetMapping("/instance/{instanceId}/export")
    public void exportPropertiesToExcel(
            @PathVariable String instanceId,
            @RequestParam(required = false) String groupId,
            jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {

        List<?> properties = wasPropertyService.getPropertiesByInstanceWithDefaults(instanceId);

        if (groupId != null && !groupId.isEmpty()) {
            properties = properties.stream()
                    .filter(prop -> {
                        if (prop instanceof java.util.Map) {
                            java.util.Map<?, ?> map = (java.util.Map<?, ?>) prop;
                            String propGroupId = String.valueOf(map.get("PROPERTYGROUPID"));
                            return groupId.equals(propGroupId);
                        }
                        return false;
                    })
                    .toList();
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"WAS_Properties_" + instanceId + "_"
                        + java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + ".csv\"");

        java.io.PrintWriter writer = response.getWriter();
        writer.write('\ufeff');
        writer.println("인스턴스ID,프로퍼티그룹ID,프로퍼티ID,프로퍼티명,초기값,설정된값,특이사항");

        for (Object prop : properties) {
            if (prop instanceof java.util.Map) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) prop;
                writer.println(String.format(
                        "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                        nvl(map.get("INSTANCEID")),
                        nvl(map.get("PROPERTYGROUPID")),
                        nvl(map.get("PROPERTYID")),
                        nvl(map.get("PROPERTYNAME")),
                        nvl(map.get("DEFAULTVALUE")),
                        nvl(map.get("PROPERTYVALUE")),
                        nvl(map.get("PROPERTYDESC"))));
            }
        }
        writer.flush();
    }

    private String nvl(Object obj) {
        return obj != null ? String.valueOf(obj).replace("\"", "\"\"") : "";
    }

    @PostMapping("/compare")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<WasPropertyCompareResponse>> compareProperties(
            @Valid @RequestBody WasPropertyCompareRequest request) {
        WasPropertyCompareResponse compareResponse = wasPropertyService.compareProperties(
                request.getInstanceId1(), request.getInstanceId2(), request.getPropertyGroupIds());
        return ResponseEntity.ok(ApiResponse.success(compareResponse));
    }

    @PostMapping("/copy")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<Integer>> copyProperties(
            @Valid @RequestBody WasPropertyCopyRequest request, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : SecurityUtil.SYSTEM_USER_ID;
        int copiedCount = wasPropertyService.copyProperties(
                request.getSourceInstanceId(),
                request.getTargetInstanceId(),
                request.getPropertyGroupIds(),
                request.getOverwrite(),
                request.getReason(),
                userId);
        return ResponseEntity.ok(ApiResponse.success(copiedCount + "개의 프로퍼티가 복사되었습니다", copiedCount));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<Integer>> batchSaveProperties(
            @Valid @RequestBody List<WasPropertyBatchSaveRequest> requests, Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : SecurityUtil.SYSTEM_USER_ID;
        int processedCount = wasPropertyService.batchSaveProperties(requests, userId);
        return ResponseEntity.ok(ApiResponse.success(processedCount + "개의 프로퍼티가 처리되었습니다", processedCount));
    }

    // ============================ Property 도메인에서 이동된 API [S] ============================

    @GetMapping("/by-property/{propertyGroupId}/{propertyId}")
    public ResponseEntity<ApiResponse<List<WasPropertyForPropertyResponse>>> getWasPropertiesByProperty(
            @PathVariable String propertyGroupId, @PathVariable String propertyId) {
        List<WasPropertyForPropertyResponse> wasProperties =
                wasPropertyService.getWasPropertiesByProperty(propertyGroupId, propertyId);
        return ResponseEntity.ok(ApiResponse.success(wasProperties));
    }

    @PostMapping("/merge")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<Integer>> saveWasProperties(
            @Valid @RequestBody List<WasPropertySaveRequest> wasProperties) {
        int processedCount = wasPropertyService.saveWasProperties(wasProperties);
        return ResponseEntity.ok(ApiResponse.success(processedCount));
    }

    // ============================ WAS 인스턴스 목록 ============================

    @GetMapping("/instances")
    public ResponseEntity<ApiResponse<List<WasInstanceSimpleResponse>>> getWasInstances() {
        List<WasInstanceSimpleResponse> instances = wasPropertyService.getWasInstances();
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    // ============================ WAS별 설정 백업/복원 [S] ============================

    @PostMapping("/groups/{propertyGroupId}/backup")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<Void>> backupWasProperties(
            @PathVariable String propertyGroupId, @Valid @RequestBody WasPropertyBackupRequest requestDTO) {
        wasPropertyService.backupWasProperties(propertyGroupId, requestDTO.getInstanceIds(), requestDTO.getReason());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/groups/{propertyGroupId}/instances/{instanceId}/history/versions")
    public ResponseEntity<ApiResponse<List<WasPropertyHistoryVersionResponse>>> getWasHistoryVersions(
            @PathVariable String propertyGroupId, @PathVariable String instanceId) {
        List<WasPropertyHistoryVersionResponse> versions =
                wasPropertyService.getWasHistoryVersions(propertyGroupId, instanceId);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    @GetMapping("/groups/{propertyGroupId}/instances/{instanceId}/history/{version}")
    public ResponseEntity<ApiResponse<List<WasPropertyHistoryResponse>>> getWasHistoryByVersion(
            @PathVariable String propertyGroupId, @PathVariable String instanceId, @PathVariable int version) {
        List<WasPropertyHistoryResponse> histories =
                wasPropertyService.getWasHistoryByVersion(propertyGroupId, instanceId, version);
        return ResponseEntity.ok(ApiResponse.success(histories));
    }

    @GetMapping("/groups/{propertyGroupId}/instances/{instanceId}/current")
    public ResponseEntity<ApiResponse<List<WasPropertyHistoryResponse>>> getCurrentWasProperties(
            @PathVariable String propertyGroupId, @PathVariable String instanceId) {
        List<WasPropertyHistoryResponse> props =
                wasPropertyService.getCurrentWasProperties(instanceId, propertyGroupId);
        return ResponseEntity.ok(ApiResponse.success(props));
    }

    @PostMapping("/groups/{propertyGroupId}/instances/{instanceId}/restore/{version}")
    @PreAuthorize("hasAuthority('PROPERTY_DB:W')")
    public ResponseEntity<ApiResponse<Void>> restoreWasProperties(
            @PathVariable String propertyGroupId, @PathVariable String instanceId, @PathVariable int version) {
        wasPropertyService.restoreWasProperties(instanceId, propertyGroupId, version);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ============================ WAS별 설정 백업/복원 [E] ============================
}

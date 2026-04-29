package com.example.spider_admin.domain.wasinstance.controller;

import com.example.spider_admin.domain.wasinstance.dto.PoolStatusResponse;
import com.example.spider_admin.domain.wasinstance.dto.WasInstanceBatchSaveRequest;
import com.example.spider_admin.domain.wasinstance.dto.WasInstanceRequest;
import com.example.spider_admin.domain.wasinstance.dto.WasInstanceResponse;
import com.example.spider_admin.domain.wasinstance.service.WasInstanceService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/was/instance")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WAS_INSTANCE:R')")
public class WasInstanceController {

    private final WasInstanceService wasInstanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WasInstanceResponse>>> getAllInstances() {
        List<WasInstanceResponse> instances = wasInstanceService.getAllInstances();
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<WasInstanceResponse>>> getInstancesWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String instanceName,
            @RequestParam(required = false) String instanceType,
            @RequestParam(required = false) String operModeType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<WasInstanceResponse> response =
                wasInstanceService.getInstances(pageRequest, instanceName, instanceType, operModeType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{instanceId}")
    public ResponseEntity<ApiResponse<WasInstanceResponse>> getInstanceById(@PathVariable String instanceId) {
        WasInstanceResponse instance = wasInstanceService.getInstanceById(instanceId);
        return ResponseEntity.ok(ApiResponse.success(instance));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WAS_INSTANCE:W')")
    public ResponseEntity<ApiResponse<WasInstanceResponse>> createInstance(@Valid @RequestBody WasInstanceRequest dto) {
        WasInstanceResponse created = wasInstanceService.createInstance(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("인스턴스가 생성되었습니다", created));
    }

    @PutMapping("/{instanceId}")
    @PreAuthorize("hasAuthority('WAS_INSTANCE:W')")
    public ResponseEntity<ApiResponse<WasInstanceResponse>> updateInstance(
            @PathVariable String instanceId, @Valid @RequestBody WasInstanceRequest dto) {
        WasInstanceResponse updated = wasInstanceService.updateInstance(instanceId, dto);
        return ResponseEntity.ok(ApiResponse.success("인스턴스가 수정되었습니다", updated));
    }

    @DeleteMapping("/{instanceId}")
    @PreAuthorize("hasAuthority('WAS_INSTANCE:W')")
    public ResponseEntity<ApiResponse<Void>> deleteInstance(@PathVariable String instanceId) {
        wasInstanceService.deleteInstance(instanceId);
        return ResponseEntity.ok(ApiResponse.success("인스턴스가 삭제되었습니다", null));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('WAS_INSTANCE:W')")
    public ResponseEntity<ApiResponse<Integer>> batchSave(
            @Valid @RequestBody List<WasInstanceBatchSaveRequest> requests) {
        int processedCount = wasInstanceService.batchSave(requests);
        return ResponseEntity.ok(ApiResponse.success(processedCount + "개의 인스턴스가 처리되었습니다", processedCount));
    }

    @GetMapping("/{instanceId}/pool-status")
    public ResponseEntity<ApiResponse<PoolStatusResponse>> getPoolStatus(@PathVariable String instanceId) {
        PoolStatusResponse status = wasInstanceService.getPoolStatus(instanceId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportWasInstances(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String instanceName,
            @RequestParam(required = false) String instanceType,
            @RequestParam(required = false) String operModeType) {
        byte[] excelBytes =
                wasInstanceService.exportWasInstances(instanceName, instanceType, operModeType, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("WasInstance", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}

package com.example.spideradmin.domain.listener.controller;

import com.example.spideradmin.domain.listener.dto.WasGatewayConnectionTestResponse;
import com.example.spideradmin.domain.listener.dto.WasGatewayStatusOptionsResponse;
import com.example.spideradmin.domain.listener.dto.WasGatewayStatusResponse;
import com.example.spideradmin.domain.listener.service.WasGatewayStatusService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/was/gateway-status")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WAS_GATEWAY_STATUS:R')")
public class WasGatewayStatusController {

    private final WasGatewayStatusService wasGatewayStatusService;

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<WasGatewayStatusResponse>>> getStatusPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String instanceId,
            @RequestParam(required = false) String gwId,
            @RequestParam(required = false) String operModeType,
            @RequestParam(required = false) String stopYn,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<WasGatewayStatusResponse> response =
                wasGatewayStatusService.getStatusPage(pageRequest, instanceId, gwId, operModeType, stopYn);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportGatewayStatus(
            @RequestParam(required = false) String instanceId,
            @RequestParam(required = false) String gwId,
            @RequestParam(required = false) String operModeType,
            @RequestParam(required = false) String stopYn,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        byte[] excelBytes = wasGatewayStatusService.exportGatewayStatus(
                instanceId, gwId, operModeType, stopYn, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("WasGatewayStatus", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping("/export-monitor")
    public ResponseEntity<byte[]> exportGatewayMonitor(
            @RequestParam(required = false) String instanceId,
            @RequestParam(required = false) String gwId,
            @RequestParam(required = false) String operModeType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        byte[] excelBytes =
                wasGatewayStatusService.exportGatewayMonitor(instanceId, gwId, operModeType, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("WasGatewayMonitor", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<WasGatewayStatusOptionsResponse>> getOptions() {
        WasGatewayStatusOptionsResponse response = wasGatewayStatusService.getOptions();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{instanceId}/{gwId}/{systemId}/test")
    @PreAuthorize("hasAuthority('WAS_GATEWAY_STATUS:W')")
    public ResponseEntity<ApiResponse<WasGatewayConnectionTestResponse>> testConnection(
            @PathVariable String instanceId, @PathVariable String gwId, @PathVariable String systemId) {
        WasGatewayConnectionTestResponse response = wasGatewayStatusService.testConnection(instanceId, gwId, systemId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

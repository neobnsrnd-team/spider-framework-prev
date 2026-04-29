package com.example.spider_admin.domain.transport.controller;

import com.example.spider_admin.domain.transport.dto.OptionResponse;
import com.example.spider_admin.domain.transport.dto.TransportBatchRequest;
import com.example.spider_admin.domain.transport.dto.TransportResponse;
import com.example.spider_admin.domain.transport.dto.TrxTypeOptionResponse;
import com.example.spider_admin.domain.transport.service.TransportService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transports")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    @GetMapping
    @PreAuthorize("hasAuthority('GATEWAY_MAPPING:R')")
    public ResponseEntity<ApiResponse<PageResponse<TransportResponse>>> searchTransports(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String trxType,
            @RequestParam(required = false) String ioType,
            @RequestParam(required = false) String reqResType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<TransportResponse> response =
                transportService.searchTransports(pageRequest, orgId, trxType, ioType, reqResType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping(params = "gatewayId")
    @PreAuthorize("hasAuthority('GATEWAY_MANAGEMENT:R')")
    public ResponseEntity<ApiResponse<List<TransportResponse>>> getByGatewayId(@RequestParam String gatewayId) {
        return ResponseEntity.ok(ApiResponse.success(transportService.getByGatewayId(gatewayId)));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('GATEWAY_MAPPING:R')")
    public ResponseEntity<byte[]> exportTransports(
            @RequestParam(required = false) String trxType,
            @RequestParam(required = false) String ioType,
            @RequestParam(required = false) String reqResType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        byte[] excelBytes = transportService.exportTransports(null, trxType, ioType, reqResType, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("GatewayMapping", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('GATEWAY_MAPPING:W')")
    public ResponseEntity<ApiResponse<Void>> saveBatch(
            @RequestParam String orgId, @Valid @RequestBody TransportBatchRequest request) {
        if (request.getUpserts() != null) {
            request.getUpserts().forEach(item -> item.setOrgId(orgId));
        }
        if (request.getDeletes() != null) {
            request.getDeletes().forEach(item -> item.setOrgId(orgId));
        }
        transportService.saveBatch(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/options/trx-types")
    @PreAuthorize("hasAuthority('GATEWAY_MANAGEMENT:R')")
    public ResponseEntity<ApiResponse<List<TrxTypeOptionResponse>>> getTrxTypes() {
        return ResponseEntity.ok(ApiResponse.success(transportService.getTrxTypeOptions()));
    }

    @GetMapping("/options/oper-modes")
    @PreAuthorize("hasAuthority('MESSAGE_HANDLER:R')")
    public ResponseEntity<ApiResponse<List<OptionResponse>>> getOperModes() {
        return ResponseEntity.ok(ApiResponse.success(transportService.getOperModeOptions()));
    }
}

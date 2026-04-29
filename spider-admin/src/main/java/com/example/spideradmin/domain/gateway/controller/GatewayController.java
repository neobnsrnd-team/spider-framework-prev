package com.example.spideradmin.domain.gateway.controller;

import com.example.spideradmin.domain.gateway.dto.GatewayDetailResponse;
import com.example.spideradmin.domain.gateway.dto.GatewayResponse;
import com.example.spideradmin.domain.gateway.dto.GatewayWithSystemsRequest;
import com.example.spideradmin.domain.gateway.service.GatewayService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gateways")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('GATEWAY_MANAGEMENT:R')")
public class GatewayController {

    private final GatewayService gatewayService;

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<GatewayResponse>>> getGatewaysPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String ioType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<GatewayResponse> response =
                gatewayService.searchGateways(pageRequest, searchField, searchValue, ioType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportGateways(
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String ioType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        byte[] excelBytes = gatewayService.exportGateways(searchField, searchValue, ioType, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("Gateway", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping("/{gwId}")
    public ResponseEntity<ApiResponse<GatewayDetailResponse>> getGatewayDetail(@PathVariable String gwId) {
        GatewayDetailResponse response = gatewayService.getGatewayDetail(gwId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/with-systems")
    @PreAuthorize("hasAuthority('GATEWAY_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> saveGatewayWithSystems(
            @Valid @RequestBody GatewayWithSystemsRequest request) {
        gatewayService.saveGatewayWithSystems(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

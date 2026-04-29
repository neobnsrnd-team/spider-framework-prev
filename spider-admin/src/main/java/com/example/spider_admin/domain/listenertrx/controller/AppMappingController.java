package com.example.spider_admin.domain.listenertrx.controller;

import com.example.spider_admin.domain.listenertrx.dto.AppMappingResponse;
import com.example.spider_admin.domain.listenertrx.dto.AppMappingUpsertRequest;
import com.example.spider_admin.domain.listenertrx.service.AppMappingService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interface-mnt/app-mappings")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('APP_MAPPING:R')")
public class AppMappingController {

    private final AppMappingService appMappingService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AppMappingResponse>>> searchMappings(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String gwId,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String reqIdCode,
            @RequestParam(required = false) String trxKeyword,
            @RequestParam(required = false) String bizAppId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<AppMappingResponse> response =
                appMappingService.searchMappings(pageRequest, gwId, orgId, reqIdCode, trxKeyword, bizAppId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{gwId}/{reqIdCode}")
    public ResponseEntity<ApiResponse<AppMappingResponse>> getMappingDetail(
            @PathVariable String gwId, @PathVariable String reqIdCode) {

        AppMappingResponse response = appMappingService.getMappingByPk(gwId, reqIdCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('APP_MAPPING:W')")
    public ResponseEntity<ApiResponse<Void>> createMapping(@Valid @RequestBody AppMappingUpsertRequest request) {

        appMappingService.createMapping(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    @PutMapping("/{gwId}/{reqIdCode}")
    @PreAuthorize("hasAuthority('APP_MAPPING:W')")
    public ResponseEntity<ApiResponse<Void>> updateMapping(
            @PathVariable String gwId,
            @PathVariable String reqIdCode,
            @Valid @RequestBody AppMappingUpsertRequest request) {

        appMappingService.updateMapping(gwId, reqIdCode, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAppMappings(
            @RequestParam(required = false) String gwId,
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String reqIdCode,
            @RequestParam(required = false) String trxKeyword,
            @RequestParam(required = false) String bizAppId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        byte[] excelBytes = appMappingService.exportAppMappings(
                gwId, orgId, reqIdCode, trxKeyword, bizAppId, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("AppMapping", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @DeleteMapping("/{gwId}/{reqIdCode}")
    @PreAuthorize("hasAuthority('APP_MAPPING:W')")
    public ResponseEntity<ApiResponse<Void>> deleteMapping(@PathVariable String gwId, @PathVariable String reqIdCode) {

        appMappingService.deleteMapping(gwId, reqIdCode);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

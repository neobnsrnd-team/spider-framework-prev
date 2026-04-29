package com.example.spider_admin.domain.listenertrx.controller;

import com.example.spider_admin.domain.listenertrx.dto.ListenerConnectorMappingBatchRequest;
import com.example.spider_admin.domain.listenertrx.dto.ListenerConnectorMappingResponse;
import com.example.spider_admin.domain.listenertrx.dto.ListenerConnectorMappingUpsertRequest;
import com.example.spider_admin.domain.listenertrx.service.ListenerConnectorMappingService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
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
@RequestMapping("/api/interface-mnt/listener-connector-mappings")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('LISTENER_CONNECTOR_MAPPING:R')")
public class ListenerConnectorMappingController {

    private final ListenerConnectorMappingService mappingService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ListenerConnectorMappingResponse>>> searchMappings(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String listenerGwId,
            @RequestParam(required = false) String listenerSystemId,
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String connectorGwId,
            @RequestParam(required = false) String connectorSystemId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<ListenerConnectorMappingResponse> response = mappingService.searchMappings(
                pageRequest, listenerGwId, listenerSystemId, identifier, connectorGwId, connectorSystemId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{listenerGwId}/{listenerSystemId}/{identifier}")
    public ResponseEntity<ApiResponse<ListenerConnectorMappingResponse>> getMappingDetail(
            @PathVariable String listenerGwId, @PathVariable String listenerSystemId, @PathVariable String identifier) {

        ListenerConnectorMappingResponse response =
                mappingService.getMappingByPk(listenerGwId, listenerSystemId, identifier);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LISTENER_CONNECTOR_MAPPING:W')")
    public ResponseEntity<ApiResponse<Void>> createMapping(
            @Valid @RequestBody ListenerConnectorMappingUpsertRequest request) {

        mappingService.createMapping(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    @PutMapping("/{listenerGwId}/{listenerSystemId}/{identifier}")
    @PreAuthorize("hasAuthority('LISTENER_CONNECTOR_MAPPING:W')")
    public ResponseEntity<ApiResponse<Void>> updateMapping(
            @PathVariable String listenerGwId,
            @PathVariable String listenerSystemId,
            @PathVariable String identifier,
            @Valid @RequestBody ListenerConnectorMappingUpsertRequest request) {

        mappingService.updateMapping(listenerGwId, listenerSystemId, identifier, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{listenerGwId}/{listenerSystemId}/{identifier}")
    @PreAuthorize("hasAuthority('LISTENER_CONNECTOR_MAPPING:W')")
    public ResponseEntity<ApiResponse<Void>> deleteMapping(
            @PathVariable String listenerGwId, @PathVariable String listenerSystemId, @PathVariable String identifier) {

        mappingService.deleteMapping(listenerGwId, listenerSystemId, identifier);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportListenerConnectorMappings(
            @RequestParam(required = false) String listenerGwId,
            @RequestParam(required = false) String listenerSystemId,
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String connectorGwId,
            @RequestParam(required = false) String connectorSystemId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] excelBytes = mappingService.exportListenerConnectorMappings(
                listenerGwId, listenerSystemId, identifier, connectorGwId, connectorSystemId, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("ListenerConnectorMapping", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('LISTENER_CONNECTOR_MAPPING:W')")
    public ResponseEntity<ApiResponse<Void>> saveMappingBatch(
            @Valid @RequestBody ListenerConnectorMappingBatchRequest request) {

        mappingService.saveMappingBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }
}

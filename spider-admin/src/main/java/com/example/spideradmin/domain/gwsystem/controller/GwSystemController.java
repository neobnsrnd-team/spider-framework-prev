package com.example.spideradmin.domain.gwsystem.controller;

import com.example.spideradmin.domain.gwsystem.dto.SystemBatchRequest;
import com.example.spideradmin.domain.gwsystem.service.GwSystemService;
import com.example.spideradmin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gw-systems")
@RequiredArgsConstructor
public class GwSystemController {

    private final GwSystemService gwSystemService;

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('GATEWAY_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> saveSystemBatch(
            @RequestParam String gatewayId, @Valid @RequestBody SystemBatchRequest request) {
        gwSystemService.saveSystemBatch(gatewayId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

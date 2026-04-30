package com.example.spideradmin.domain.service.controller;

import com.example.spideradmin.domain.service.dto.FwkServiceRelationResponse;
import com.example.spideradmin.domain.service.dto.FwkServiceRelationSaveRequest;
import com.example.spideradmin.domain.service.service.FwkServiceRelationService;
import com.example.spideradmin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 연결 컴포넌트 REST Controller.
 *
 * <p>클래스 레벨: FWK_SERVICE:R (읽기 기본), 저장은 FWK_SERVICE:W 오버라이드.
 */
@Slf4j
@RestController
@RequestMapping("/api/fwk-services")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('FWK_SERVICE:R')")
public class FwkServiceRelationController {

    private final FwkServiceRelationService fwkServiceRelationService;

    /** 서비스 연결 컴포넌트 + 파라미터 목록 조회 */
    @GetMapping("/{serviceId}/relations")
    public ResponseEntity<ApiResponse<List<FwkServiceRelationResponse>>> getRelations(@PathVariable String serviceId) {
        log.info("GET /api/fwk-services/{}/relations", serviceId);
        return ResponseEntity.ok(ApiResponse.success(fwkServiceRelationService.getRelations(serviceId)));
    }

    /** 서비스 연결 컴포넌트 저장 (replace 전략) */
    @PutMapping("/{serviceId}/relations")
    @PreAuthorize("hasAuthority('FWK_SERVICE:W')")
    public ResponseEntity<ApiResponse<List<FwkServiceRelationResponse>>> saveRelations(
            @PathVariable String serviceId, @Valid @RequestBody FwkServiceRelationSaveRequest dto) {
        log.info("PUT /api/fwk-services/{}/relations", serviceId);
        return ResponseEntity.ok(ApiResponse.success(fwkServiceRelationService.saveRelations(serviceId, dto)));
    }
}

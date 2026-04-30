package com.example.spideradmin.domain.emergencynotice.controller;

import com.example.spideradmin.domain.emergencynotice.dto.EmergencyNoticeBulkSaveRequest;
import com.example.spideradmin.domain.emergencynotice.dto.EmergencyNoticeResponse;
import com.example.spideradmin.domain.emergencynotice.service.EmergencyNoticeService;
import com.example.spideradmin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 긴급공지 관리 REST Controller
 */
@RestController
@RequestMapping("/api/emergency-notices")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('EMERGENCY_NOTICE:R')")
public class EmergencyNoticeController {

    private final EmergencyNoticeService emergencyNoticeService;

    /**
     * 긴급공지 목록 및 노출 타입 조회
     * GET /api/emergency-notices
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll() {
        List<EmergencyNoticeResponse> notices = emergencyNoticeService.getAll();
        String displayType = emergencyNoticeService.getDisplayType();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "notices", notices,
                "displayType", displayType)));
    }

    /**
     * 긴급공지 일괄 저장 (언어별 제목·내용 + 노출 타입)
     * PUT /api/emergency-notices
     */
    @PutMapping
    @PreAuthorize("hasAuthority('EMERGENCY_NOTICE:W')")
    public ResponseEntity<ApiResponse<Void>> saveAll(@Valid @RequestBody EmergencyNoticeBulkSaveRequest request) {
        emergencyNoticeService.saveAll(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

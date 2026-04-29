package com.example.admin_demo.domain.emergencynotice.controller;

import com.example.admin_demo.domain.emergencynotice.dto.EmergencyNoticeSettingsRequest;
import com.example.admin_demo.domain.emergencynotice.service.EmergencyNoticeDeployService;
import com.example.admin_demo.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 긴급공지 배포 관리 REST Controller
 *
 * <p>배포 라이프사이클(DRAFT → DEPLOYED → ENDED)을 제어하고,
 * biz-channel에 TCP 커맨드를 통해 배포 상태를 동기화한다.
 */
@RestController
@RequestMapping("/api/emergency-notice-deploys")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('EMERGENCY_NOTICE:R')")
public class EmergencyNoticeDeployController {

    private final EmergencyNoticeDeployService emergencyNoticeDeployService;

    /**
     * 현재 배포 상태 및 이력 조회 (페이징·구분 필터 지원)
     * GET /api/emergency-notice-deploys?reason=배포&page=1&pageSize=10
     *
     * @param reason   구분 필터 (생략 시 전체)
     * @param page     페이지 번호 (기본값 1)
     * @param pageSize 페이지당 건수 (기본값 10)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeployInfo(
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(
                ApiResponse.success(emergencyNoticeDeployService.getDeployInfo(reason, page, pageSize)));
    }

    /**
     * 긴급공지 배포 (DRAFT / ENDED → DEPLOYED)
     * POST /api/emergency-notice-deploys/deploy
     */
    @PostMapping("/deploy")
    @PreAuthorize("hasAuthority('EMERGENCY_NOTICE:W')")
    public ResponseEntity<ApiResponse<Void>> deploy() {
        emergencyNoticeDeployService.deploy();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 긴급공지 배포 종료 (DEPLOYED → ENDED)
     * POST /api/emergency-notice-deploys/end
     */
    @PostMapping("/end")
    @PreAuthorize("hasAuthority('EMERGENCY_NOTICE:W')")
    public ResponseEntity<ApiResponse<Void>> endDeploy() {
        emergencyNoticeDeployService.endDeploy();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 공지 노출 설정 변경 (닫기 버튼·오늘 하루 보지 않기)
     * PATCH /api/emergency-notice-deploys/settings
     *
     * <p>배포 중이면 변경 즉시 biz-channel에 재동기화된다.
     */
    @PatchMapping("/settings")
    @PreAuthorize("hasAuthority('EMERGENCY_NOTICE:W')")
    public ResponseEntity<ApiResponse<Void>> updateSettings(
            @Valid @RequestBody EmergencyNoticeSettingsRequest request) {
        emergencyNoticeDeployService.updateSettings(request.getCloseableYn(), request.getHideTodayYn());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

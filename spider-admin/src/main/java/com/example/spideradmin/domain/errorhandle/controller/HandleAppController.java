package com.example.spideradmin.domain.errorhandle.controller;

import com.example.spideradmin.domain.errorhandle.dto.HandleAppResponse;
import com.example.spideradmin.domain.errorhandle.service.HandleAppService;
import com.example.spideradmin.global.dto.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Handle App management
 * 핸들러 APP 관리 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/handle-apps")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ERROR_CODE:R')")
public class HandleAppController {

    private final HandleAppService handleAppService;

    /**
     * 전체 핸들러 APP 목록 조회
     * GET /api/handle-apps
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<HandleAppResponse>>> getAllHandleApps() {
        log.info("GET /api/handle-apps - Fetching all handle apps");
        List<HandleAppResponse> handleApps = handleAppService.getAllHandleApps();
        return ResponseEntity.ok(ApiResponse.success(handleApps));
    }

    /**
     * 핸들러 APP 상세 조회
     * GET /api/handle-apps/{handleAppId}
     */
    @GetMapping("/{handleAppId}")
    public ResponseEntity<ApiResponse<HandleAppResponse>> getHandleApp(@PathVariable String handleAppId) {
        log.info("GET /api/handle-apps/{} - Fetching handle app", handleAppId);
        HandleAppResponse handleApp = handleAppService.getHandleApp(handleAppId);
        return ResponseEntity.ok(ApiResponse.success(handleApp));
    }
}

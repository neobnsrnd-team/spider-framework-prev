package com.example.spideradmin.domain.errorhandle.controller;

import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppResponse;
import com.example.spideradmin.domain.errorhandle.service.ErrorHandleAppService;
import com.example.spideradmin.global.dto.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 오류코드-처리APP 매핑 REST Controller
 */
@RestController
@RequestMapping("/api/error-handle-apps")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ERROR_CODE:R')")
public class ErrorHandleAppController {

    private final ErrorHandleAppService errorHandleAppService;

    /**
     * 오류코드별 처리APP 매핑 목록 조회 (핸들러명 포함)
     * GET /api/error-handle-apps?errorCode=xxx
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ErrorHandleAppResponse>>> getByErrorCode(@RequestParam String errorCode) {
        List<ErrorHandleAppResponse> result = errorHandleAppService.getByErrorCodeWithHandleAppName(errorCode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

package com.example.spideradmin.domain.errorcode.controller;

import com.example.spideradmin.domain.errorcode.dto.ErrorDescCreateRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorDescResponse;
import com.example.spideradmin.domain.errorcode.dto.ErrorDescUpdateRequest;
import com.example.spideradmin.domain.errorcode.service.ErrorDescService;
import com.example.spideradmin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 오류코드 다국어 설명 REST Controller
 */
@RestController
@RequestMapping("/api/error-descs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ERROR_CODE:R')")
public class ErrorDescController {

    private final ErrorDescService errorDescService;

    /**
     * 오류코드별 다국어 설명 목록 조회
     * GET /api/error-descs?errorCode=xxx
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ErrorDescResponse>>> getByErrorCode(@RequestParam String errorCode) {
        List<ErrorDescResponse> result = errorDescService.getByErrorCode(errorCode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 오류코드 다국어 설명 상세 조회
     * GET /api/error-descs/{errorCode}/{localeCode}
     */
    @GetMapping("/{errorCode}/{localeCode}")
    public ResponseEntity<ApiResponse<ErrorDescResponse>> getById(
            @PathVariable String errorCode, @PathVariable String localeCode) {
        ErrorDescResponse result = errorDescService.getById(errorCode, localeCode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 오류코드 다국어 설명 등록
     * POST /api/error-descs
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ERROR_CODE:W')")
    public ResponseEntity<ApiResponse<ErrorDescResponse>> create(
            @Valid @RequestBody ErrorDescCreateRequest requestDTO) {
        ErrorDescResponse created = errorDescService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("오류코드 설명이 등록되었습니다", created));
    }

    /**
     * 오류코드 다국어 설명 수정
     * PUT /api/error-descs/{errorCode}/{localeCode}
     */
    @PutMapping("/{errorCode}/{localeCode}")
    @PreAuthorize("hasAuthority('ERROR_CODE:W')")
    public ResponseEntity<ApiResponse<ErrorDescResponse>> update(
            @PathVariable String errorCode,
            @PathVariable String localeCode,
            @Valid @RequestBody ErrorDescUpdateRequest requestDTO) {
        ErrorDescResponse updated = errorDescService.update(errorCode, localeCode, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("오류코드 설명이 수정되었습니다", updated));
    }

    /**
     * 오류코드 다국어 설명 삭제
     * DELETE /api/error-descs/{errorCode}/{localeCode}
     */
    @DeleteMapping("/{errorCode}/{localeCode}")
    @PreAuthorize("hasAuthority('ERROR_CODE:W')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String errorCode, @PathVariable String localeCode) {
        errorDescService.delete(errorCode, localeCode);
        return ResponseEntity.ok(ApiResponse.success("오류코드 설명이 삭제되었습니다", null));
    }

    /**
     * 오류코드의 모든 다국어 설명 삭제
     * DELETE /api/error-descs?errorCode=xxx
     */
    @DeleteMapping
    @PreAuthorize("hasAuthority('ERROR_CODE:W')")
    public ResponseEntity<ApiResponse<Void>> deleteByErrorCode(@RequestParam String errorCode) {
        errorDescService.deleteByErrorCode(errorCode);
        return ResponseEntity.ok(ApiResponse.success("오류코드의 모든 설명이 삭제되었습니다", null));
    }
}

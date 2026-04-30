package com.example.spideradmin.domain.validation.controller;

import com.example.spideradmin.domain.validation.dto.ValidationCreateRequest;
import com.example.spideradmin.domain.validation.dto.ValidationResponse;
import com.example.spideradmin.domain.validation.dto.ValidationSearchRequest;
import com.example.spideradmin.domain.validation.dto.ValidationUpdateRequest;
import com.example.spideradmin.domain.validation.service.ValidationService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Validation 컴포넌트 관리 REST Controller
 *
 * @PreAuthorize 적용: Validation 관리 메뉴 권한 검사
 * - 클래스 레벨: 읽기 권한(READ) 기본 적용
 */
@Slf4j
@RestController
@RequestMapping("/api/validations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VALIDATION:R')")
public class ValidationController {

    private final ValidationService validationService;

    /**
     * Validation 페이징 검색 조회
     * GET /api/validations/page
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ValidationResponse>>> getValidationsWithPagination(
            @ModelAttribute ValidationSearchRequest searchDTO) {
        log.info(
                "GET /api/validations/page - page: {}, size: {}, validationId: {}, validationDesc: {}",
                searchDTO.getPage(),
                searchDTO.getSize(),
                searchDTO.getValidationId(),
                searchDTO.getValidationDesc());
        PageResponse<ValidationResponse> response = validationService.getValidationsWithSearch(searchDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Validation 단건 조회
     * GET /api/validations/{validationId}
     */
    @GetMapping("/{validationId}")
    public ResponseEntity<ApiResponse<ValidationResponse>> getById(@PathVariable String validationId) {
        log.info("GET /api/validations/{}", validationId);
        return ResponseEntity.ok(ApiResponse.success(validationService.getById(validationId)));
    }

    /**
     * Validation 엑셀 내보내기
     * GET /api/validations/export
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportValidations(
            @RequestParam(required = false) String validationId,
            @RequestParam(required = false) String validationDesc,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        byte[] excelBytes = validationService.exportValidations(validationId, validationDesc, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("Validation", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * Validation 등록
     * POST /api/validations
     */
    @PostMapping
    @PreAuthorize("hasAuthority('VALIDATION:W')")
    public ResponseEntity<ApiResponse<ValidationResponse>> create(@Valid @RequestBody ValidationCreateRequest dto) {
        log.info("POST /api/validations - validationId: {}", dto.getValidationId());
        ValidationResponse response = validationService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Validation 수정
     * PUT /api/validations/{validationId}
     */
    @PutMapping("/{validationId}")
    @PreAuthorize("hasAuthority('VALIDATION:W')")
    public ResponseEntity<ApiResponse<ValidationResponse>> update(
            @PathVariable String validationId, @Valid @RequestBody ValidationUpdateRequest dto) {
        log.info("PUT /api/validations/{}", validationId);
        return ResponseEntity.ok(ApiResponse.success(validationService.update(validationId, dto)));
    }

    /**
     * Validation 삭제
     * DELETE /api/validations/{validationId}
     */
    @DeleteMapping("/{validationId}")
    @PreAuthorize("hasAuthority('VALIDATION:W')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String validationId) {
        log.info("DELETE /api/validations/{}", validationId);
        validationService.delete(validationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

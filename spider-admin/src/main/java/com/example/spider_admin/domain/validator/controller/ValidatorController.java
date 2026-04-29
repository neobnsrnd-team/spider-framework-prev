package com.example.spider_admin.domain.validator.controller;

import com.example.spider_admin.domain.validator.dto.ValidatorCreateRequest;
import com.example.spider_admin.domain.validator.dto.ValidatorResponse;
import com.example.spider_admin.domain.validator.dto.ValidatorSearchRequest;
import com.example.spider_admin.domain.validator.dto.ValidatorUpdateRequest;
import com.example.spider_admin.domain.validator.service.ValidatorService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
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
 * 거래 Validator 관리 REST Controller
 *
 * @PreAuthorize 적용: Validator 관리 메뉴 권한 검사
 * - 클래스 레벨: 읽기 권한(READ) 기본 적용
 */
@Slf4j
@RestController
@RequestMapping("/api/validators")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VALIDATOR:R')")
public class ValidatorController {

    private final ValidatorService validatorService;

    /**
     * Validator 페이징 검색 조회
     * GET /api/validators/page?page=1&size=20&validatorId=...&validatorName=...&bizDomain=...&useYn=...
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ValidatorResponse>>> getValidatorsWithPagination(
            @ModelAttribute ValidatorSearchRequest searchDTO) {

        log.info(
                "GET /api/validators/page - page: {}, size: {}, validatorId: {}, validatorName: {}, bizDomain: {}, useYn: {}",
                searchDTO.getPage(),
                searchDTO.getSize(),
                searchDTO.getValidatorId(),
                searchDTO.getValidatorName(),
                searchDTO.getBizDomain(),
                searchDTO.getUseYn());

        PageResponse<ValidatorResponse> response = validatorService.getValidatorsWithSearch(searchDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Validator 단건 조회
     * GET /api/validators/{validatorId}
     */
    @GetMapping("/{validatorId}")
    public ResponseEntity<ApiResponse<ValidatorResponse>> getById(@PathVariable String validatorId) {
        log.info("GET /api/validators/{}", validatorId);
        return ResponseEntity.ok(ApiResponse.success(validatorService.getById(validatorId)));
    }

    /**
     * Validator 엑셀 내보내기
     * GET /api/validators/export
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportValidators(
            @RequestParam(required = false) String validatorId,
            @RequestParam(required = false) String validatorName,
            @RequestParam(required = false) String bizDomain,
            @RequestParam(required = false) String useYn,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        byte[] excelBytes =
                validatorService.exportValidators(validatorId, validatorName, bizDomain, useYn, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("Validator", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * Validator 등록
     * POST /api/validators
     */
    @PostMapping
    @PreAuthorize("hasAuthority('VALIDATOR:W')")
    public ResponseEntity<ApiResponse<ValidatorResponse>> create(@Valid @RequestBody ValidatorCreateRequest dto) {
        log.info("POST /api/validators - validatorId: {}", dto.getValidatorId());
        ValidatorResponse response = validatorService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Validator 수정
     * PUT /api/validators/{validatorId}
     */
    @PutMapping("/{validatorId}")
    @PreAuthorize("hasAuthority('VALIDATOR:W')")
    public ResponseEntity<ApiResponse<ValidatorResponse>> update(
            @PathVariable String validatorId, @Valid @RequestBody ValidatorUpdateRequest dto) {
        log.info("PUT /api/validators/{}", validatorId);
        return ResponseEntity.ok(ApiResponse.success(validatorService.update(validatorId, dto)));
    }

    /**
     * Validator 삭제
     * DELETE /api/validators/{validatorId}
     */
    @DeleteMapping("/{validatorId}")
    @PreAuthorize("hasAuthority('VALIDATOR:W')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String validatorId) {
        log.info("DELETE /api/validators/{}", validatorId);
        validatorService.delete(validatorId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

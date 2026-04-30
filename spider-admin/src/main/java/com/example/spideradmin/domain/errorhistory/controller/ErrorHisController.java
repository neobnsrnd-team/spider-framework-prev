package com.example.spideradmin.domain.errorhistory.controller;

import com.example.spideradmin.domain.errorhistory.dto.ErrorHisResponse;
import com.example.spideradmin.domain.errorhistory.dto.ErrorHisSearchRequest;
import com.example.spideradmin.domain.errorhistory.service.ErrorHisService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Error History management
 * 오류 발생 이력 관리 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/error-histories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ERROR_HIS:R')")
public class ErrorHisController {

    private final ErrorHisService errorHisService;

    /**
     * 오류 발생 이력 목록 조회 (페이징 + 검색)
     * GET /api/error-histories/page?page=1&size=20
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ErrorHisResponse>>> getErrorHistories(
            @ModelAttribute ErrorHisSearchRequest searchDTO) {

        log.info(
                "GET /api/error-histories/page - page={}, size={}, errorCode={}",
                searchDTO.getPage(),
                searchDTO.getSize(),
                searchDTO.getErrorCode());

        PageResponse<ErrorHisResponse> response = errorHisService.getErrorHistories(searchDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 오류 발생 이력 상세 조회
     * GET /api/error-histories/{errorCode}/{errorSerNo}
     */
    @GetMapping("/{errorCode}/{errorSerNo}")
    public ResponseEntity<ApiResponse<ErrorHisResponse>> getErrorHis(
            @PathVariable String errorCode, @PathVariable String errorSerNo) {
        log.info("GET /api/error-histories/{}/{} - Fetching error history detail", errorCode, errorSerNo);
        ErrorHisResponse errorHis = errorHisService.getErrorHis(errorCode, errorSerNo);
        return ResponseEntity.ok(ApiResponse.success(errorHis));
    }

    /**
     * 오류 발생 이력 엑셀 내보내기
     * GET /api/error-histories/export
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportErrorHistories(@ModelAttribute ErrorHisSearchRequest searchDTO) {
        log.info("GET /api/error-histories/export");
        byte[] excelBytes = errorHisService.exportErrorHistories(searchDTO);
        String fileName = ExcelExportUtil.generateFileName("ErrorHistory", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}

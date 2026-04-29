package com.example.spider_admin.domain.transdata.controller;

import com.example.spider_admin.domain.transdata.dto.TransDataGenerationRequest;
import com.example.spider_admin.domain.transdata.dto.TransDataSourceResponse;
import com.example.spider_admin.domain.transdata.dto.TransDataTimesResponse;
import com.example.spider_admin.domain.transdata.service.TransDataGenerationService;
import com.example.spider_admin.domain.transdata.service.TransDataSqlDownloadService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.util.SecurityUtil;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 이행 데이터 생성 REST API
 */
@RestController
@RequestMapping("/api/trans/generation")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TRANS_DATA_POPUP:R')")
public class TransDataGenerationController {

    private final TransDataGenerationService transDataGenerationService;
    private final TransDataSqlDownloadService transDataSqlDownloadService;

    /**
     * 탭별 소스 데이터 조회
     */
    @GetMapping("/source")
    public ResponseEntity<ApiResponse<List<TransDataSourceResponse>>> getSourceList(
            @RequestParam String tab,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String orgId) {

        List<TransDataSourceResponse> result =
                transDataGenerationService.getSourceList(tab, searchField, searchValue, orgId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 이행 실행 (DB 저장 - 기존)
     */
    @PostMapping("/execute")
    @PreAuthorize("hasAuthority('TRANS_DATA_POPUP:W')")
    public ResponseEntity<ApiResponse<TransDataTimesResponse>> executeTransfer(
            @Valid @RequestBody TransDataGenerationRequest dto) {

        TransDataTimesResponse result = transDataGenerationService.executeTransfer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    /**
     * 이행 SQL 파일 ZIP 다운로드
     */
    @PostMapping("/download")
    @PreAuthorize("hasAuthority('TRANS_DATA_POPUP:W')")
    public ResponseEntity<byte[]> downloadSqlZip(@Valid @RequestBody TransDataGenerationRequest dto)
            throws IOException {

        String userId = SecurityUtil.getCurrentUserIdOrSystem();
        String timestamp = transDataSqlDownloadService.generateTimestamp();
        byte[] zipBytes = transDataSqlDownloadService.generateSqlZip(dto, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + timestamp + ".zip\"");

        return ResponseEntity.ok().headers(headers).body(zipBytes);
    }
}

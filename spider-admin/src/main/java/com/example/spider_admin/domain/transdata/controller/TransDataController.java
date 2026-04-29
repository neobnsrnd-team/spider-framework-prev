package com.example.spider_admin.domain.transdata.controller;

import com.example.spider_admin.domain.transdata.dto.TransDataDetailResponse;
import com.example.spider_admin.domain.transdata.dto.TransDataHisFailResponse;
import com.example.spider_admin.domain.transdata.dto.TransDataHisResponse;
import com.example.spider_admin.domain.transdata.dto.TransDataHisSearchRequest;
import com.example.spider_admin.domain.transdata.dto.TransDataTimesResponse;
import com.example.spider_admin.domain.transdata.dto.TransDataTimesSearchRequest;
import com.example.spider_admin.domain.transdata.service.TransDataService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 이행 데이터 조회 REST API
 * READ-ONLY 기능만 제공
 */
@RestController
@RequestMapping("/api/trans")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TRANS_DATA_EXEC:R')")
public class TransDataController {

    private final TransDataService transDataService;

    /**
     * 이행 실행 이력 페이지네이션 검색
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<TransDataTimesResponse>>> getTransDataTimesWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String tranResult) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        TransDataTimesSearchRequest searchDTO = TransDataTimesSearchRequest.builder()
                .userId(userId)
                .tranResult(tranResult)
                .build();

        return ResponseEntity.ok(ApiResponse.success(transDataService.searchTransDataTimes(pageRequest, searchDTO)));
    }

    /**
     * 이행 실행 이력 엑셀 내보내기
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTransDataTimes(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String tranResult) {

        TransDataTimesSearchRequest searchDTO = TransDataTimesSearchRequest.builder()
                .userId(userId)
                .tranResult(tranResult)
                .build();

        byte[] excelBytes = transDataService.exportTransDataTimes(searchDTO, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("TransData", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * 이행 상세 이력 조회
     */
    @GetMapping("/{tranSeq}/details")
    public ResponseEntity<ApiResponse<TransDataDetailResponse>> getTransDataDetail(@PathVariable Long tranSeq) {
        return ResponseEntity.ok(ApiResponse.success(transDataService.getTransDataDetail(tranSeq)));
    }

    /**
     * 이행 상세 이력 페이지네이션 검색
     */
    @GetMapping("/{tranSeq}/details/page")
    public ResponseEntity<ApiResponse<PageResponse<TransDataHisResponse>>> getTransDataHisWithPagination(
            @PathVariable Long tranSeq,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String tranResult,
            @RequestParam(required = false) String tranType) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        TransDataHisSearchRequest searchDTO = TransDataHisSearchRequest.builder()
                .tranResult(tranResult)
                .tranType(tranType)
                .build();

        return ResponseEntity.ok(
                ApiResponse.success(transDataService.searchTransDataHis(tranSeq, pageRequest, searchDTO)));
    }

    /**
     * 이행 상세 이력 실패 상세 조회
     */
    @GetMapping("/{tranSeq}/details/fail")
    public ResponseEntity<ApiResponse<TransDataHisFailResponse>> getTransDataHisFailDetail(
            @PathVariable Long tranSeq, @RequestParam String tranId, @RequestParam String tranType) {
        return ResponseEntity.ok(
                ApiResponse.success(transDataService.getTransDataHisFailDetail(tranSeq, tranId, tranType)));
    }
}

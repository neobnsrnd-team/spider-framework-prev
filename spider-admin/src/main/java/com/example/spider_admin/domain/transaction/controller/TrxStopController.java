package com.example.spider_admin.domain.transaction.controller;

import com.example.spider_admin.domain.transaction.dto.OperModeBatchRequest;
import com.example.spider_admin.domain.transaction.dto.TrxStopBatchRequest;
import com.example.spider_admin.domain.transaction.dto.TrxStopListResponse;
import com.example.spider_admin.domain.transaction.dto.TrxStopSearchRequest;
import com.example.spider_admin.domain.transaction.service.TrxStopService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 거래중지 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/trx-stop")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TRX_STOP:R')")
public class TrxStopController {

    private final TrxStopService trxStopService;

    /**
     * 거래중지 목록 페이징 검색
     * GET /api/trx-stop/page
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<TrxStopListResponse>>> searchTrxStop(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String operModeTypeFilter,
            @RequestParam(required = false) String trxStopYnFilter,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        TrxStopSearchRequest searchDTO = TrxStopSearchRequest.builder()
                .searchField(searchField)
                .searchValue(searchValue)
                .operModeTypeFilter(operModeTypeFilter)
                .trxStopYnFilter(trxStopYnFilter)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<TrxStopListResponse> result = trxStopService.searchTrxStopList(pageRequest, searchDTO);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 거래 중지 엑셀 내보내기
     * GET /api/trx-stop/export
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTrxStop(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String operModeTypeFilter,
            @RequestParam(required = false) String trxStopYnFilter) {

        byte[] excelBytes = trxStopService.exportTrxStop(
                searchField, searchValue, operModeTypeFilter, trxStopYnFilter, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("TrxStop", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * 거래 일괄 중지/시작
     * PUT /api/trx-stop/batch
     */
    @PutMapping("/batch")
    @PreAuthorize("hasAuthority('TRX_STOP:W')")
    public ResponseEntity<ApiResponse<Void>> batchUpdateTrxStop(@Valid @RequestBody TrxStopBatchRequest requestDTO) {
        trxStopService.batchUpdateTrxStop(requestDTO);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 운영모드 일괄 변경
     * PUT /api/trx-stop/batch-oper-mode
     */
    @PutMapping("/batch-oper-mode")
    @PreAuthorize("hasAuthority('TRX_STOP:W')")
    public ResponseEntity<ApiResponse<Void>> batchUpdateOperMode(@RequestBody OperModeBatchRequest requestDTO) {
        trxStopService.batchUpdateOperMode(requestDTO);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

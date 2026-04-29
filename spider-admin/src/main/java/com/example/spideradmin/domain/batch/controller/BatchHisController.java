package com.example.spideradmin.domain.batch.controller;

import com.example.spideradmin.domain.batch.dto.BatchHisResponse;
import com.example.spideradmin.domain.batch.dto.BatchHisSearchRequest;
import com.example.spideradmin.domain.batch.service.BatchHisService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 배치 수행 내역 조회 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/batch/history")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BATCH_HISTORY:R')")
public class BatchHisController {

    private final BatchHisService batchHisService;

    /**
     * 배치 수행 이력 목록 조회 (검색 + 페이지네이션)
     * GET /api/batch/history/page?page=1&size=20&batchAppId=test&instanceId=PT11&resRtCode=SUCCESS&batchDate=20260114
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<BatchHisResponse>>> getBatchHistoryWithPagination(
            @ModelAttribute BatchHisSearchRequest searchDTO) {

        log.info(
                "GET /api/batch/history/page - page: {}, size: {}, batchAppId: {}, instanceId: {}, resRtCode: {}, batchDate: {}",
                searchDTO.getPage(),
                searchDTO.getSize(),
                searchDTO.getBatchAppId(),
                searchDTO.getInstanceId(),
                searchDTO.getResRtCode(),
                searchDTO.getBatchDate());

        PageResponse<BatchHisResponse> response =
                batchHisService.searchBatchHistory(searchDTO.toPageRequest(), searchDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 배치 수행 이력 엑셀 내보내기
     * GET /api/batch/history/export?batchAppId=test&instanceId=PT11&resRtCode=1&batchDate=20260114
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportBatchHistory(@ModelAttribute BatchHisSearchRequest searchDTO) {
        byte[] excelBytes = batchHisService.exportBatchHistory(searchDTO);
        String fileName = ExcelExportUtil.generateFileName("BatchHistory", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}

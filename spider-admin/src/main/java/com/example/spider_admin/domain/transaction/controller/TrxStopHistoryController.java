package com.example.spider_admin.domain.transaction.controller;

import com.example.spider_admin.domain.transaction.dto.TrxStopHistorySearchRequest;
import com.example.spider_admin.domain.transaction.dto.TrxStopHistoryWithTrxNameResponse;
import com.example.spider_admin.domain.transaction.service.TrxStopHistoryService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 거래중지이력 조회 인터페이스를 제공합니다.
 * <p>이 컨트롤러는 TRX_STOP_HISTORY 권한 체계에 바인딩되어 있으며,
 * 거래 및 서비스의 중지/재개 이력을 시간순으로 추적합니다.
 * <p>모든 응답은 {@link ApiResponse} 규격으로 통일하여 반환합니다.
 * <p>거래중지이력은 {@code TrxService.updateTrx()} 호출 시 거래중지여부가 변경될 때 자동으로 생성되며,
 * 이 컨트롤러는 조회 전용 API를 제공합니다.
 *
 * @see TrxStopHistoryService
 * @see PreAuthorize
 */
@Slf4j
@RestController
@RequestMapping("/api/trx-stop-histories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TRX_STOP_HISTORY:R')")
public class TrxStopHistoryController {

    private final TrxStopHistoryService trxStopHistoryService;

    /**
     * 검색 조건에 따른 거래중지이력 페이징 검색을 수행합니다.
     * <p>구분유형(거래/서비스), 거래ID, 일시 범위를 기준으로 필터링하며,
     * 결과는 거래중지시간 역순으로 정렬됩니다.
     *
     * @param searchDTO   검색 조건 (구분유형, 거래ID, 시작/종료일시)
     * @param pageRequest 페이징 정보 (page, size)
     * @return 페이징 처리된 거래중지이력 목록 (거래명 포함)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TrxStopHistoryWithTrxNameResponse>>> searchHistories(
            @ModelAttribute TrxStopHistorySearchRequest searchDTO, @ModelAttribute PageRequest pageRequest) {
        log.info(
                "Searching TrxStopHistory: gubunType={}, trxId={}, dateRange=[{}, {}], page={}, size={}",
                searchDTO.getGubunType(),
                searchDTO.getTrxId(),
                searchDTO.getStartDtime(),
                searchDTO.getEndDtime(),
                pageRequest.getPage(),
                pageRequest.getSize());

        PageResponse<TrxStopHistoryWithTrxNameResponse> response =
                trxStopHistoryService.searchHistories(searchDTO, pageRequest);

        log.debug("Found {} histories (total: {})", response.getContent().size(), response.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 거래ID의 전체 중지이력을 조회합니다.
     * <p>해당 거래의 모든 중지/재개 이력을 시간순으로 반환하며 페이징 처리되지 않습니다.
     *
     * @param trxId 거래 고유 식별자
     * @return 해당 거래의 중지이력 목록 (거래명 포함)
     */
    @GetMapping("/trx/{trxId}")
    public ResponseEntity<ApiResponse<List<TrxStopHistoryWithTrxNameResponse>>> getByTrxId(@PathVariable String trxId) {
        log.info("Getting TrxStopHistory by trxId: {}", trxId);

        List<TrxStopHistoryWithTrxNameResponse> histories = trxStopHistoryService.findByTrxId(trxId);

        log.debug("Found {} histories for trxId: {}", histories.size(), trxId);

        return ResponseEntity.ok(ApiResponse.success(histories));
    }

    /**
     * 거래중지이력을 엑셀 파일로 내보냅니다.
     *
     * @param searchDTO 검색 조건 (구분유형, 거래ID, 시작/종료일시)
     * @return xlsx 파일 바이트 배열
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@ModelAttribute TrxStopHistorySearchRequest searchDTO) {
        log.info("GET /api/trx-stop-histories/export");

        byte[] excelBytes = trxStopHistoryService.exportExcel(searchDTO);
        String fileName = ExcelExportUtil.generateFileName("TrxStopHistory", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }
}

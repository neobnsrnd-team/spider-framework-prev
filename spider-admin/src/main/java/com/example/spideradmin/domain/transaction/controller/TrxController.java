package com.example.spideradmin.domain.transaction.controller;

import com.example.spideradmin.domain.transaction.dto.TrxCreateRequest;
import com.example.spideradmin.domain.transaction.dto.TrxCreateResponse;
import com.example.spideradmin.domain.transaction.dto.TrxListResponse;
import com.example.spideradmin.domain.transaction.dto.TrxResponse;
import com.example.spideradmin.domain.transaction.dto.TrxSearchRequest;
import com.example.spideradmin.domain.transaction.dto.TrxSimpleResponse;
import com.example.spideradmin.domain.transaction.dto.TrxUpdateRequest;
import com.example.spideradmin.domain.transaction.service.TrxService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * TRX(거래) 리소스에 대한 CRUD 인터페이스를 제공합니다.
 * 모든 응답은 {@link ApiResponse} 규격으로 통일하여 반환합니다.
 *
 * @see TrxService
 */
@Slf4j
@RestController
@RequestMapping("/api/trx")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TRX:R')")
public class TrxController {

    private final TrxService trxService;

    /**
     * 전체 거래 목록 조회 (select box용)
     * GET /api/trx/list
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<TrxSimpleResponse>>> getAllTrxList() {
        log.info("GET /api/trx/list - Fetching all TRX list for select box");
        List<TrxSimpleResponse> trxList = trxService.getAllTrxList();
        return ResponseEntity.ok(ApiResponse.success(trxList));
    }

    /**
     * 특정 식별자(ID)를 기반으로 상세 TRX 정보를 조회합니다.
     *
     * @param trxId TRX 고유 식별자
     */
    @GetMapping("/{trxId}")
    public ResponseEntity<ApiResponse<TrxResponse>> getTrxById(@PathVariable String trxId) {
        return ResponseEntity.ok(ApiResponse.success(trxService.getTrxById(trxId)));
    }

    /**
     * 새로운 TRX를 생성합니다.
     * 생성이 완료되면 201 Created 응답과 함께 생성된 데이터 정보를 반환합니다.
     *
     * @param requestDTO 생성할 TRX 정보 (유효성 검증 필수)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('TRX:W')")
    public ResponseEntity<ApiResponse<TrxCreateResponse>> createTrx(@Valid @RequestBody TrxCreateRequest requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(trxService.createTrx(requestDTO)));
    }

    /**
     * TRX 정보를 수정합니다.
     *
     * @param trxId      수정할 TRX의 식별자
     * @param requestDTO 수정할 TRX 정보 (유효성 검증 필수)
     */
    @PutMapping("/{trxId}")
    @PreAuthorize("hasAuthority('TRX:W')")
    public ResponseEntity<ApiResponse<TrxResponse>> updateTrx(
            @PathVariable String trxId, @Valid @RequestBody TrxUpdateRequest requestDTO) {
        return ResponseEntity.ok(ApiResponse.success(trxService.updateTrx(trxId, requestDTO)));
    }

    /**
     * TRX를 시스템에서 삭제 처리합니다.
     *
     * @param trxId 삭제할 TRX의 식별자
     */
    @DeleteMapping("/{trxId}")
    @PreAuthorize("hasAuthority('TRX:W')")
    public ResponseEntity<ApiResponse<Void>> deleteTrx(@PathVariable String trxId) {
        trxService.deleteTrx(trxId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 거래 목록 페이징 검색
     * GET /api/trx/page
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<TrxListResponse>>> searchTrx(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String orgIdFilter,
            @RequestParam(required = false) String trxStopYnFilter,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        TrxSearchRequest searchDTO = TrxSearchRequest.builder()
                .searchField(searchField)
                .searchValue(searchValue)
                .orgIdFilter(orgIdFilter)
                .trxStopYnFilter(trxStopYnFilter)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<TrxListResponse> result = trxService.searchTrxWithPagination(pageRequest, searchDTO);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 거래 목록 엑셀 내보내기
     * GET /api/trx/export
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String orgIdFilter,
            @RequestParam(required = false) String trxStopYnFilter) {

        byte[] excelBytes = trxService.exportTransactions(
                searchField, searchValue, trxStopYnFilter, orgIdFilter, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("Transaction", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * 업무분류 옵션 목록 조회
     * GET /api/trx/options/biz-groups
     */
    @GetMapping("/options/biz-groups")
    public ResponseEntity<ApiResponse<List<String>>> getBizGroupOptions() {
        return ResponseEntity.ok(ApiResponse.success(trxService.getBizGroupOptions()));
    }
}

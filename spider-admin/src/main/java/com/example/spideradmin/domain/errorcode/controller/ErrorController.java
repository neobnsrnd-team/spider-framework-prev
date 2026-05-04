package com.example.spideradmin.domain.errorcode.controller;

import com.example.spideradmin.domain.errorcode.dto.ErrorCreateRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorDetailResponse;
import com.example.spideradmin.domain.errorcode.dto.ErrorResponse;
import com.example.spideradmin.domain.errorcode.dto.ErrorSearchRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorUpdateRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorWithHandleAppsResponse;
import com.example.spideradmin.domain.errorcode.service.ErrorService;
import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppRequest;
import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppResponse;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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
 * REST Controller for Error Code management
 * 오류코드 관리 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/errors")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ERROR_CODE:R')")
public class ErrorController {

    private final ErrorService errorService;

    /**
     * 오류코드 목록 조회 (페이징 + 검색)
     * GET /api/errors/page?page=1&size=20&searchField=errorCode&searchValue=AIE&trxId=xxx&handleAppId=xxx
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ErrorWithHandleAppsResponse>>> getErrors(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String trxId,
            @RequestParam(required = false) String handleAppId) {

        log.info(
                "GET /api/errors/page - page={}, size={}, searchField={}, searchValue={}, trxId={}, handleAppId={}",
                page,
                size,
                searchField,
                searchValue,
                trxId,
                handleAppId);

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        ErrorSearchRequest searchDTO = ErrorSearchRequest.builder()
                .searchField(searchField)
                .searchValue(searchValue)
                .trxId(trxId)
                .handleAppId(handleAppId)
                .build();

        PageResponse<ErrorWithHandleAppsResponse> response = errorService.getErrors(pageRequest, searchDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 오류코드 엑셀 내보내기
     * GET /api/errors/export
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportErrors(
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String trxId,
            @RequestParam(required = false) String handleAppId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        byte[] excelBytes =
                errorService.exportErrors(searchField, searchValue, trxId, handleAppId, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("ErrorCode", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * 오류코드 상세 조회
     * GET /api/errors/{errorCode}
     */
    @GetMapping("/{errorCode}")
    public ResponseEntity<ApiResponse<ErrorDetailResponse>> getError(@PathVariable String errorCode) {
        log.info("GET /api/errors/{} - Fetching error detail", errorCode);
        ErrorDetailResponse error = errorService.getErrorDetail(errorCode);
        return ResponseEntity.ok(ApiResponse.success(error));
    }

    /**
     * 오류코드 등록
     * POST /api/errors
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ERROR_CODE:W')")
    public ResponseEntity<ApiResponse<ErrorResponse>> createError(@Valid @RequestBody ErrorCreateRequest dto) {
        log.info("POST /api/errors - Creating error: {}", dto.getErrorCode());
        ErrorResponse createdError = errorService.createError(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("오류코드가 등록되었습니다", createdError));
    }

    /**
     * 오류코드 수정
     * PUT /api/errors/{errorCode}
     */
    @PutMapping("/{errorCode}")
    @PreAuthorize("hasAuthority('ERROR_CODE:W')")
    public ResponseEntity<ApiResponse<ErrorResponse>> updateError(
            @PathVariable String errorCode, @Valid @RequestBody ErrorUpdateRequest dto) {
        log.info("PUT /api/errors/{} - Updating error", errorCode);
        ErrorResponse updatedError = errorService.updateError(errorCode, dto);
        return ResponseEntity.ok(ApiResponse.success("오류코드가 수정되었습니다", updatedError));
    }

    /**
     * 오류코드 삭제
     * DELETE /api/errors/{errorCode}
     */
    @DeleteMapping("/{errorCode}")
    public ResponseEntity<ApiResponse<Void>> deleteError(@PathVariable String errorCode) {
        log.info("DELETE /api/errors/{} - Deleting error", errorCode);
        errorService.deleteError(errorCode);
        return ResponseEntity.ok(ApiResponse.success("오류코드가 삭제되었습니다", null));
    }

    /**
     * 오류코드별 핸들러 목록 조회
     * GET /api/errors/{errorCode}/handle-apps
     */
    @GetMapping("/{errorCode}/handle-apps")
    public ResponseEntity<ApiResponse<List<ErrorHandleAppResponse>>> getErrorHandleApps(
            @PathVariable String errorCode) {
        log.info("GET /api/errors/{}/handle-apps - Fetching error handle apps", errorCode);
        List<ErrorHandleAppResponse> handleApps = errorService.getErrorHandleApps(errorCode);
        return ResponseEntity.ok(ApiResponse.success(handleApps));
    }

    /**
     * 오류코드별 핸들러 저장 (전체 교체)
     * PUT /api/errors/{errorCode}/handle-apps
     */
    @PutMapping("/{errorCode}/handle-apps")
    @PreAuthorize("hasAuthority('ERROR_CODE:W')")
    public ResponseEntity<ApiResponse<Void>> saveErrorHandleApps(
            @PathVariable String errorCode, @RequestBody List<ErrorHandleAppRequest> handleApps) {
        log.info("PUT /api/errors/{}/handle-apps - Saving {} handle apps", errorCode, handleApps.size());
        errorService.saveErrorHandleApps(errorCode, handleApps);
        return ResponseEntity.ok(ApiResponse.success("핸들러가 저장되었습니다", null));
    }
}

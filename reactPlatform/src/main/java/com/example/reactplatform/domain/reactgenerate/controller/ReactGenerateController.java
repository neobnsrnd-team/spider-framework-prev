package com.example.reactplatform.domain.reactgenerate.controller;

import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateApprovalResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateRequest;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateSearchRequest;
import com.example.reactplatform.domain.reactgenerate.dto.ReactRegenerateRequest;
import com.example.reactplatform.domain.reactgenerate.dto.RenderErrorRequest;
import com.example.reactplatform.domain.reactgenerate.service.ReactGenerateService;
import com.example.reactplatform.global.dto.ApiResponse;
import com.example.reactplatform.global.util.ExcelExportUtil;
import com.example.reactplatform.global.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/react-generate")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REACT_GENERATE:R')")
public class ReactGenerateController {

    private final ReactGenerateService reactGenerateService;

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('REACT_GENERATE:W')")
    public ResponseEntity<ApiResponse<ReactGenerateResponse>> generate(
            @Valid @RequestBody ReactGenerateRequest request) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(reactGenerateService.generate(request, currentUserId)));
    }

    /**
     * 기존 생성 이력을 기반으로 변경 요청사항을 반영하여 코드를 재생성한다.
     *
     * @param id      재생성 기준이 되는 원본 코드 ID (refCodeId)
     * @param request 변경 요청사항
     * @return 재생성된 코드와 메타 정보 (refCodeId, rootCodeId 포함)
     */
    @PostMapping("/{id}/regenerate")
    @PreAuthorize("hasAuthority('REACT_GENERATE:W')")
    public ResponseEntity<ApiResponse<ReactGenerateResponse>> regenerate(
            @PathVariable String id, @Valid @RequestBody ReactRegenerateRequest request) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(reactGenerateService.regenerate(id, request, currentUserId)));
    }

    @PostMapping("/{id}/request-approval")
    @PreAuthorize("hasAuthority('REACT_GENERATE:W')")
    public ResponseEntity<ApiResponse<ReactGenerateApprovalResponse>> requestApproval(@PathVariable String id) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(reactGenerateService.requestApproval(id, currentUserId)));
    }

    /**
     * 검색 조건에 맞는 React 코드 생성 이력 목록을 페이지네이션 형태로 조회한다.
     *
     * @param req 검색 조건 (status, createUserId, fromDate, toDate, page, size)
     * @return list, totalCount, page, size
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory(ReactGenerateSearchRequest req) {
        return ResponseEntity.ok(ApiResponse.success(reactGenerateService.getHistory(req)));
    }

    /**
     * 검색 조건에 맞는 이력 전체를 엑셀 파일로 내보낸다.
     *
     * @param req 검색 조건 (status, createUserId, fromDate, toDate)
     * @return xlsx 파일
     */
    @GetMapping("/history/export")
    public ResponseEntity<byte[]> exportHistory(ReactGenerateSearchRequest req) {
        byte[] excelBytes = reactGenerateService.exportHistory(req);
        String fileName = ExcelExportUtil.generateFileName("ReactGenerateHistory", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * CODE_ID로 React 코드 생성 이력 상세를 조회한다.
     *
     * @param id 조회할 CODE_ID
     * @return 코드·미리보기 등 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReactGenerateResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(reactGenerateService.getById(id)));
    }

    /**
     * Preview App(iframe)에서 발생한 렌더링 오류를 수신하여 오류 이력에 기록한다.
     *
     * <p>렌더링 오류는 브라우저 side에서 소멸하므로 클라이언트가 명시적으로 전송해야 한다.
     * fire-and-forget 방식으로 호출되며, 항상 200을 반환한다.
     */
    @PostMapping("/render-error")
    @PreAuthorize("hasAuthority('REACT_GENERATE:R')")
    public ResponseEntity<Void> logRenderError(
            @RequestBody RenderErrorRequest request, HttpServletRequest httpRequest) {
        reactGenerateService.logRenderError(
                request.getCodeId(),
                request.getErrorMessage(),
                SecurityUtil.getCurrentUserIdOrAnonymous(),
                httpRequest.getRequestURI());
        return ResponseEntity.ok().build();
    }
}

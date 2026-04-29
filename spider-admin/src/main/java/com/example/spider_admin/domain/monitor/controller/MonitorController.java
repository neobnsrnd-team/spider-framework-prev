package com.example.spider_admin.domain.monitor.controller;

import com.example.spider_admin.domain.monitor.dto.MonitorCreateRequest;
import com.example.spider_admin.domain.monitor.dto.MonitorResponse;
import com.example.spider_admin.domain.monitor.dto.MonitorUpdateRequest;
import com.example.spider_admin.domain.monitor.service.MonitorService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
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
 * <h3>Monitor REST Controller</h3>
 * <p>모니터 현황판 관리를 위한 AJAX 엔드포인트를 제공합니다.</p>
 *
 * <h4>예외 처리:</h4>
 * <ul>
 *     <li>GlobalExceptionHandler에 위임하여 일관된 에러 응답 제공</li>
 * </ul>
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *     <li>GET /api/monitors - 전체 목록 조회</li>
 *     <li>GET /api/monitors/{monitorId} - 단건 조회</li>
 *     <li>POST /api/monitors - 생성</li>
 *     <li>PUT /api/monitors/{monitorId} - 수정</li>
 *     <li>DELETE /api/monitors/{monitorId} - 삭제</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/monitors")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MONITOR:R')")
public class MonitorController {

    private final MonitorService monitorService;

    /**
     * 전체 모니터 목록 조회
     * <p>GET /api/monitors</p>
     *
     * @param sortBy        정렬 기준 필드
     * @param sortDirection 정렬 방향 (ASC, DESC)
     * @return 모니터 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MonitorResponse>>> getAllMonitors(
            @RequestParam(required = false) String sortBy, @RequestParam(required = false) String sortDirection) {
        log.info("GET /api/monitors - Fetching all monitors (sortBy={}, sortDirection={})", sortBy, sortDirection);
        List<MonitorResponse> monitors = monitorService.getAllMonitors(sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(monitors));
    }

    /**
     * 페이징 처리된 모니터 목록 조회
     * <p>GET /api/monitors/page</p>
     *
     * @param page          페이지 번호 (1-based index)
     * @param size          페이지 당 항목 수
     * @param sortBy        정렬 기준 필드
     * @param sortDirection 정렬 방향 (ASC, DESC)
     * @param searchField   검색 필드 (monitorId, monitorName)
     * @param searchValue   검색 값
     * @return 페이징 처리된 모니터 목록
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<MonitorResponse>>> getMonitorsWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue) {
        log.debug("GET /api/monitors/page - page={}, size={}", page, size);

        // searchField 화이트리스트 검증 (SQL Injection 방지)
        if (searchField != null
                && !searchField.isEmpty()
                && !"monitorId".equals(searchField)
                && !"monitorName".equals(searchField)) {
            log.warn("Invalid searchField: {}", searchField);
            return ResponseEntity.badRequest().body(ApiResponse.error("유효하지 않은 검색 필드입니다.", 400));
        }

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1) // 1-based → 0-based 변환
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<MonitorResponse> response =
                monitorService.getMonitorsWithPagination(pageRequest, searchField, searchValue);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 모니터 ID로 단건 조회
     * <p>GET /api/monitors/{monitorId}</p>
     *
     * @param monitorId 모니터 ID
     * @return 모니터 DTO
     */
    @GetMapping("/{monitorId}")
    public ResponseEntity<ApiResponse<MonitorResponse>> getMonitorById(@PathVariable String monitorId) {
        log.info("GET /api/monitors/{} - Fetching monitor by ID", monitorId);
        MonitorResponse monitor = monitorService.getMonitorById(monitorId);
        return ResponseEntity.ok(ApiResponse.success(monitor));
    }

    /**
     * 새로운 모니터 생성
     * <p>POST /api/monitors</p>
     *
     * @param dto 모니터 생성 요청 DTO
     * @return 생성된 모니터 DTO
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MONITOR:W')")
    public ResponseEntity<ApiResponse<MonitorResponse>> createMonitor(@Valid @RequestBody MonitorCreateRequest dto) {

        log.info("POST /api/monitors - Creating new monitor: {}", dto.getMonitorId());
        MonitorResponse createdMonitor = monitorService.createMonitor(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("모니터가 생성되었습니다", createdMonitor));
    }

    /**
     * 기존 모니터 수정
     * <p>PUT /api/monitors/{monitorId}</p>
     *
     * @param monitorId 모니터 ID
     * @param dto 모니터 수정 요청 DTO
     * @return 수정된 모니터 DTO
     */
    @PutMapping("/{monitorId}")
    @PreAuthorize("hasAuthority('MONITOR:W')")
    public ResponseEntity<ApiResponse<MonitorResponse>> updateMonitor(
            @PathVariable String monitorId, @Valid @RequestBody MonitorUpdateRequest dto) {

        log.info("PUT /api/monitors/{} - Updating monitor", monitorId);
        MonitorResponse updatedMonitor = monitorService.updateMonitor(monitorId, dto);
        return ResponseEntity.ok(ApiResponse.success("모니터가 수정되었습니다", updatedMonitor));
    }

    /**
     * 모니터 삭제
     * <p>DELETE /api/monitors/{monitorId}</p>
     *
     * @param monitorId 모니터 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{monitorId}")
    @PreAuthorize("hasAuthority('MONITOR:W')")
    public ResponseEntity<ApiResponse<Void>> deleteMonitor(@PathVariable String monitorId) {
        log.info("DELETE /api/monitors/{} - Deleting monitor", monitorId);
        monitorService.deleteMonitor(monitorId);
        return ResponseEntity.ok(ApiResponse.success("모니터가 삭제되었습니다", null));
    }

    /**
     * 모니터 목록 엑셀 다운로드
     * <p>GET /api/monitors/export</p>
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMonitors(
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        // searchField 화이트리스트 검증 (SQL Injection 방지)
        if (searchField != null
                && !searchField.isEmpty()
                && !"monitorId".equals(searchField)
                && !"monitorName".equals(searchField)) {
            log.warn("Invalid searchField: {}", searchField);
            return ResponseEntity.badRequest().build();
        }
        byte[] excelBytes = monitorService.exportMonitors(searchField, searchValue, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("Monitor", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * 전체 모니터 수 조회
     * <p>GET /api/monitors/count/total</p>
     *
     * @return 전체 모니터 개수
     */
    @GetMapping("/count/total")
    public ResponseEntity<ApiResponse<Long>> getTotalCount() {
        log.info("GET /api/monitors/count/total - Fetching total count");
        long count = monitorService.getTotalCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * 사용 중인 모니터 수 조회
     * <p>GET /api/monitors/count/active</p>
     *
     * @return 사용 중인 모니터 개수
     */
    @GetMapping("/count/active")
    public ResponseEntity<ApiResponse<Long>> getActiveCount() {
        log.info("GET /api/monitors/count/active - Fetching active count");
        long count = monitorService.getActiveCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}

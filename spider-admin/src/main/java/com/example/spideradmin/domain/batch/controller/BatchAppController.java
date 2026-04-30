package com.example.spideradmin.domain.batch.controller;

import com.example.spideradmin.domain.batch.dto.BatchAppCreateRequest;
import com.example.spideradmin.domain.batch.dto.BatchAppDetailResponse;
import com.example.spideradmin.domain.batch.dto.BatchAppResponse;
import com.example.spideradmin.domain.batch.dto.BatchAppSearchRequest;
import com.example.spideradmin.domain.batch.dto.BatchAppUpdateRequest;
import com.example.spideradmin.domain.batch.dto.ScheduleCronUpdateRequest;
import com.example.spideradmin.domain.batch.dto.ScheduleTriggerRequest;
import com.example.spideradmin.domain.batch.service.BatchAppService;
import com.example.spideradmin.domain.batch.service.BatchExecService;
import com.example.spideradmin.global.dto.ApiResponse;
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
 * 배치 App 관리 REST Controller
 *
 * @PreAuthorize 적용: 배치 App 관리 메뉴(BATCH_APP) 권한 검사
 * - 클래스 레벨: 읽기 권한(BATCH_APP:R) 기본 적용
 * - 생성/수정/삭제 API: 쓰기 권한(BATCH_APP:W) 메서드 레벨 오버라이드
 */
@Slf4j
@RestController
@RequestMapping("/api/batch/apps")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BATCH_APP:R')")
public class BatchAppController {

    private final BatchAppService batchAppService;
    private final BatchExecService batchExecService;

    // ==================== 조회 API ====================

    /**
     * 전체 배치 앱 목록 조회 (드롭다운용)
     * GET /api/batch/apps/list
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<BatchAppResponse>>> getAllBatchApps() {
        log.info("GET /api/batch/apps/list - Fetching all batch apps");
        List<BatchAppResponse> batchApps = batchAppService.getAllBatchApps();
        return ResponseEntity.ok(ApiResponse.success(batchApps));
    }

    /**
     * 배치 앱 페이징 검색 조회
     * GET /api/batch/apps/page?page=1&size=10&searchField=batchAppName&searchValue=test
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<BatchAppResponse>>> getBatchAppsWithPagination(
            @ModelAttribute BatchAppSearchRequest searchDTO) {

        log.info(
                "GET /api/batch/apps/page - page: {}, size: {}, searchField: {}, searchValue: {}",
                searchDTO.getPage(),
                searchDTO.getSize(),
                searchDTO.getSearchField(),
                searchDTO.getSearchValue());

        PageResponse<BatchAppResponse> response =
                batchAppService.getBatchAppsWithSearch(searchDTO.toPageRequest(), searchDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 배치 앱 상세 조회 (WAS 인스턴스 할당 정보 포함)
     * GET /api/batch/apps/{batchAppId}
     */
    @GetMapping("/{batchAppId}")
    public ResponseEntity<ApiResponse<BatchAppDetailResponse>> getBatchAppById(@PathVariable String batchAppId) {
        log.info("GET /api/batch/apps/{} - Fetching batch app by ID", batchAppId);
        BatchAppDetailResponse batchApp = batchAppService.getBatchAppById(batchAppId);
        return ResponseEntity.ok(ApiResponse.success(batchApp));
    }

    /**
     * 배치 앱 ID 중복 체크
     * GET /api/batch/apps/check/id?batchAppId=test
     */
    @GetMapping("/check/id")
    public ResponseEntity<ApiResponse<Boolean>> checkBatchAppIdExists(@RequestParam String batchAppId) {
        log.info("GET /api/batch/apps/check/id - Checking batchAppId: {}", batchAppId);
        boolean exists = batchAppService.existsByBatchAppId(batchAppId);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportBatchApps(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String instanceIdFilter,
            @RequestParam(required = false) String batchCycleFilter) {
        byte[] excelBytes = batchAppService.exportBatchApps(
                searchField, searchValue, instanceIdFilter, batchCycleFilter, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("BatchApp", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    // ==================== 생성/수정/삭제 API ====================

    /**
     * 배치 앱 생성
     * POST /api/batch/apps
     */
    @PostMapping
    @PreAuthorize("hasAuthority('BATCH_APP:W')")
    public ResponseEntity<ApiResponse<BatchAppResponse>> createBatchApp(
            @Valid @RequestBody BatchAppCreateRequest requestDTO) {

        log.info("POST /api/batch/apps - Creating new batch app: {}", requestDTO.getBatchAppId());
        BatchAppResponse createdBatchApp = batchAppService.createBatchApp(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("배치 앱이 생성되었습니다", createdBatchApp));
    }

    /**
     * 배치 앱 수정
     * PUT /api/batch/apps/{batchAppId}
     */
    @PutMapping("/{batchAppId}")
    @PreAuthorize("hasAuthority('BATCH_APP:W')")
    public ResponseEntity<ApiResponse<BatchAppResponse>> updateBatchApp(
            @PathVariable String batchAppId, @Valid @RequestBody BatchAppUpdateRequest requestDTO) {

        log.info("PUT /api/batch/apps/{} - Updating batch app", batchAppId);
        BatchAppResponse updatedBatchApp = batchAppService.updateBatchApp(batchAppId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("배치 앱이 수정되었습니다", updatedBatchApp));
    }

    /**
     * 배치 앱 삭제
     * DELETE /api/batch/apps/{batchAppId}
     */
    @DeleteMapping("/{batchAppId}")
    @PreAuthorize("hasAuthority('BATCH_APP:W')")
    public ResponseEntity<ApiResponse<Void>> deleteBatchApp(@PathVariable String batchAppId) {
        log.info("DELETE /api/batch/apps/{} - Deleting batch app", batchAppId);
        batchAppService.deleteBatchApp(batchAppId);
        return ResponseEntity.ok(ApiResponse.success("배치 앱이 삭제되었습니다", null));
    }

    // ==================== WAS Instance 할당 API ====================

    /**
     * WAS 인스턴스 추가
     * POST /api/batch/apps/{batchAppId}/was/instance
     */
    @PostMapping("/{batchAppId}/was/instance")
    @PreAuthorize("hasAuthority('BATCH_APP:W')")
    public ResponseEntity<ApiResponse<Void>> addInstanceToBatchApp(
            @PathVariable String batchAppId, @RequestParam String instanceId) {

        log.info("POST /api/batch/apps/{}/was/instance - Adding instance: {}", batchAppId, instanceId);
        batchAppService.addInstanceToBatchApp(batchAppId, instanceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("WAS 인스턴스가 추가되었습니다", null));
    }

    /**
     * WAS 인스턴스 할당 수정
     * PUT /api/batch/apps/{batchAppId}/was/instance/{instanceId}
     */
    @PutMapping("/{batchAppId}/was/instance/{instanceId}")
    @PreAuthorize("hasAuthority('BATCH_APP:W')")
    public ResponseEntity<ApiResponse<Void>> updateInstanceAssignment(
            @PathVariable String batchAppId, @PathVariable String instanceId, @RequestParam String useYn) {

        log.info("PUT /api/batch/apps/{}/was/instance/{} - Updating assignment", batchAppId, instanceId);
        batchAppService.updateInstanceAssignment(batchAppId, instanceId, useYn);
        return ResponseEntity.ok(ApiResponse.success("WAS 인스턴스 설정이 수정되었습니다", null));
    }

    /**
     * WAS 인스턴스 제거
     * DELETE /api/batch/apps/{batchAppId}/was/instance/{instanceId}
     */
    @DeleteMapping("/{batchAppId}/was/instance/{instanceId}")
    @PreAuthorize("hasAuthority('BATCH_APP:W')")
    public ResponseEntity<ApiResponse<Void>> removeInstanceFromBatchApp(
            @PathVariable String batchAppId, @PathVariable String instanceId) {

        log.info("DELETE /api/batch/apps/{}/was/instance/{} - Removing instance", batchAppId, instanceId);
        batchAppService.removeInstanceFromBatchApp(batchAppId, instanceId);
        return ResponseEntity.ok(ApiResponse.success("WAS 인스턴스가 삭제되었습니다", null));
    }

    // ==================== 스케줄 관리 API ====================

    /**
     * 스케줄 즉시 실행.
     * 지정 WAS 인스턴스의 Quartz Job을 즉시 트리거한다.
     * POST /api/batch/apps/{batchAppId}/schedule/trigger
     */
    @PostMapping("/{batchAppId}/schedule/trigger")
    @PreAuthorize("hasAuthority('BATCH_APP:W')")
    public ResponseEntity<ApiResponse<Void>> triggerSchedule(
            @PathVariable String batchAppId, @Valid @RequestBody ScheduleTriggerRequest request) {

        log.info("POST /api/batch/apps/{}/schedule/trigger - instanceId={}", batchAppId, request.getInstanceId());
        batchExecService.triggerSchedule(batchAppId, request.getInstanceId(), request.getBatchDate());
        return ResponseEntity.ok(ApiResponse.success("스케줄 즉시 실행이 요청되었습니다", null));
    }

    /**
     * 스케줄 Cron 표현식 변경.
     * DB(FWK_BATCH_APP.CRON_TEXT)를 업데이트하고 모든 배정 WAS 인스턴스에 스케줄 재등록 커맨드를 전송한다.
     * PUT /api/batch/apps/{batchAppId}/schedule/cron
     */
    @PutMapping("/{batchAppId}/schedule/cron")
    @PreAuthorize("hasAuthority('BATCH_APP:W')")
    public ResponseEntity<ApiResponse<Void>> updateScheduleCron(
            @PathVariable String batchAppId, @Valid @RequestBody ScheduleCronUpdateRequest request) {

        log.info("PUT /api/batch/apps/{}/schedule/cron - cron={}", batchAppId, request.getCronText());
        // 1. DB 업데이트
        batchAppService.updateCronText(batchAppId, request.getCronText());
        // 2. 모든 배정 인스턴스에 Quartz 재스케줄 TCP 전송
        batchExecService.updateScheduleCron(batchAppId, request.getCronText());
        return ResponseEntity.ok(ApiResponse.success("Cron 스케줄이 변경되었습니다", null));
    }
}

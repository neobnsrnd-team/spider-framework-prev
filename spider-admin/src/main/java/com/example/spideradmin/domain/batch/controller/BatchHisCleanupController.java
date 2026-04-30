package com.example.spideradmin.domain.batch.controller;

import com.example.spideradmin.domain.batch.dto.BatchHisCleanupResponse;
import com.example.spideradmin.domain.batch.service.BatchHisCleanupService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.exception.InvalidInputException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 배치 이력 정리 관리 API
 * <p>
 * 배치 실행 이력 데이터의 수동 정리 기능을 제공합니다.
 * </p>
 */
@Tag(name = "배치 이력 정리", description = "배치 이력 정리 관리 API")
@RestController
@RequestMapping("/api/batch/history/cleanup")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BATCH_HISTORY:R')")
public class BatchHisCleanupController {

    private static final int MIN_RETENTION_DAYS = 1;
    private static final int MAX_RETENTION_DAYS = 365;

    private final BatchHisCleanupService batchHisCleanupService;

    /**
     * 삭제 대상 건수 조회 (미리보기)
     *
     * @param retentionDays 보관 기간 (일, 기본값 90)
     * @return 삭제 대상 건수
     */
    @Operation(summary = "삭제 대상 건수 조회", description = "보관 기간 경과 데이터 건수를 조회합니다.")
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<Integer>> previewCleanup(
            @Parameter(description = "보관 기간 (일, 1~365)", example = "90") @RequestParam(defaultValue = "90")
                    int retentionDays) {

        validateRetentionDays(retentionDays);
        int count = batchHisCleanupService.countDeletionTarget(retentionDays);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * 수동 정리 실행
     *
     * @param retentionDays 보관 기간 (일, 기본값 90)
     * @return 정리 결과 (삭제 건수, 소요 시간 등)
     */
    @Operation(summary = "수동 정리 실행", description = "보관 기간 경과 데이터를 수동으로 삭제합니다.")
    @PostMapping("/execute")
    @PreAuthorize("hasAuthority('BATCH_HISTORY:W')")
    public ResponseEntity<ApiResponse<BatchHisCleanupResponse>> executeCleanup(
            @Parameter(description = "보관 기간 (일, 1~365)", example = "90") @RequestParam(defaultValue = "90")
                    int retentionDays) {

        validateRetentionDays(retentionDays);
        BatchHisCleanupResponse result = batchHisCleanupService.cleanup(retentionDays);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * TEST-ONLY: 특정 배치 앱/인스턴스(선택: 배치일자) 이력만 삭제
     */
    @Operation(
            summary = "Delete test target history",
            description = "TEST-ONLY endpoint that deletes only the target batch history records")
    @DeleteMapping("/test-target")
    @PreAuthorize("hasAuthority('BATCH_HISTORY:W')")
    public ResponseEntity<ApiResponse<Integer>> deleteTestTargetHistory(
            @Parameter(description = "Batch app id", example = "PM_BEXHIS") @RequestParam String batchAppId,
            @Parameter(description = "Instance id", example = "BH01") @RequestParam String instanceId,
            @Parameter(description = "Batch date (YYYYMMDD, optional)", example = "20260210")
                    @RequestParam(required = false)
                    String batchDate) {

        int deletedCount = batchHisCleanupService.deleteTestTargetHistory(batchAppId, instanceId, batchDate);
        return ResponseEntity.ok(ApiResponse.success("TEST-ONLY: target batch history deleted", deletedCount));
    }

    /**
     * 보관 기간 유효성 검증
     */
    private void validateRetentionDays(int retentionDays) {
        if (retentionDays < MIN_RETENTION_DAYS || retentionDays > MAX_RETENTION_DAYS) {
            throw new InvalidInputException(
                    String.format("보관 기간은 %d~%d일 사이여야 합니다.", MIN_RETENTION_DAYS, MAX_RETENTION_DAYS));
        }
    }
}

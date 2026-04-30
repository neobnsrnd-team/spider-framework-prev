package com.example.spideradmin.domain.batch.controller;

import com.example.spideradmin.domain.batch.dto.BatchExecRequest;
import com.example.spideradmin.domain.batch.dto.BatchHisResponse;
import com.example.spideradmin.domain.batch.enums.BatchResRtCode;
import com.example.spideradmin.domain.batch.service.BatchExecService;
import com.example.spideradmin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Batch Execution
 * Provides AJAX endpoints for manual batch execution
 *
 * @PreAuthorize 적용: 배치 App 관리 메뉴(BATCH_APP) 권한 검사
 */
@Slf4j
@RestController
@RequestMapping("/api/batch/exec")
@RequiredArgsConstructor
public class BatchExecController {

    private final BatchExecService batchExecService;

    /**
     * 배치 수동 실행
     * POST /api/batch/exec
     */
    @PostMapping
    @PreAuthorize("hasAuthority('BATCH_APP:W')")
    public ResponseEntity<ApiResponse<List<BatchHisResponse>>> executeManualBatch(
            @Valid @RequestBody BatchExecRequest requestDTO) {

        log.info(
                "POST /api/batch/exec - Manual batch execution request: batchAppId={}, instanceIds={}, batchDate={}",
                requestDTO.getBatchAppId(),
                requestDTO.getInstanceIds(),
                requestDTO.getBatchDate());

        List<BatchHisResponse> results = batchExecService.executeManualBatch(requestDTO);

        long successCount = 0;
        long abnormalCount = 0;
        for (BatchHisResponse r : results) {
            if (BatchResRtCode.SUCCESS.getCode().equals(r.getResRtCode())) successCount++;
            else if (BatchResRtCode.ABNORMAL_TERMINATION.getCode().equals(r.getResRtCode())) abnormalCount++;
        }

        if (abnormalCount == 0) {
            // 전체 성공
            String message = String.format("배치 실행이 완료되었습니다. (총 %d건 성공)", successCount);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(message, results));
        }

        // 전체 실패 또는 부분 실패 — 빌더 통합
        String message = (successCount == 0)
                ? String.format("배치 실행이 실패하였습니다. (총 %d건 실패)", abnormalCount)
                : String.format("일부 배치 실행에 실패하였습니다. (성공: %d건 / 실패: %d건)", successCount, abnormalCount);

        return ResponseEntity.ok(ApiResponse.<List<BatchHisResponse>>builder()
                .success(false)
                .message(message)
                .data(results)
                .code(200)
                .build());
    }

    /**
     * 배치 실행 이력 조회 (단건)
     * GET /api/batch/exec/{batchAppId}/{instanceId}/{batchDate}/{batchExecuteSeq}
     *
     * @throws com.example.spideradmin.domain.batch.exception.BatchHisNotFoundException
     *         배치 실행 이력을 찾을 수 없을 때 발생 (HTTP 404)
     */
    @GetMapping("/{batchAppId}/{instanceId}/{batchDate}/{batchExecuteSeq}")
    @PreAuthorize("hasAuthority('BATCH_APP:R')")
    public ResponseEntity<ApiResponse<BatchHisResponse>> getBatchHis(
            @PathVariable String batchAppId,
            @PathVariable String instanceId,
            @PathVariable String batchDate,
            @PathVariable Integer batchExecuteSeq) {

        log.info(
                "GET /api/batch/exec/{}/{}/{}/{} - Fetching batch execution history",
                batchAppId,
                instanceId,
                batchDate,
                batchExecuteSeq);

        BatchHisResponse result = batchExecService.getBatchHis(batchAppId, instanceId, batchDate, batchExecuteSeq);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

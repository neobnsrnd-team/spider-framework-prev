package com.example.spideradmin.domain.batch.controller;

import com.example.spideradmin.domain.batch.dto.BatchRunningResponse;
import com.example.spideradmin.domain.batch.dto.BatchStopRequest;
import com.example.spideradmin.domain.batch.service.BatchRunningService;
import com.example.spideradmin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 실행 중 배치 모니터링 REST API 컨트롤러.
 *
 * <p>Admin이 여러 batch-was 인스턴스에 병렬 HTTP 요청을 보내 실행 중 배치 목록을 집계하고,
 * 특정 배치의 강제 종료를 해당 WAS 인스턴스로 프록시하는 API를 제공한다.
 *
 * <p>뷰 라우팅은 {@code PageController}에서 {@code /batches/running} 경로로 처리한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchRunningApiController {

    private final BatchRunningService batchRunningService;

    /**
     * 전체 WAS 인스턴스에서 실행 중인 배치 목록 집계 조회.
     * GET /api/batch/running
     *
     * <p>등록된 모든 WAS 인스턴스에 병렬 HTTP 요청을 보내 실행 중 배치 목록을 집계한다.
     * 통신 불가 인스턴스는 connected=false 항목으로 포함되며 나머지 결과는 정상 반환된다.
     *
     * @return 전체 WAS 인스턴스의 실행 중 배치 목록 (instanceId 오름차순 정렬)
     */
    @GetMapping("/running")
    @PreAuthorize("hasAuthority('BATCH_RUNNING:R')")
    public ApiResponse<List<BatchRunningResponse>> getRunningBatches() {
        log.info("GET /api/batch/running - 실행 중 배치 목록 집계 조회");
        List<BatchRunningResponse> result = batchRunningService.getRunningBatches();
        return ApiResponse.success(result);
    }

    /**
     * 특정 WAS 인스턴스의 실행 중 배치 강제 종료 프록시.
     * POST /api/batch/stop
     *
     * <p>요청에 지정된 WAS 인스턴스에 강제 종료 요청을 프록시한다.
     * instanceId로 대상 WAS를 식별하고, jobExecutionId로 종료할 배치 실행을 특정한다.
     *
     * @param request 강제 종료 대상 인스턴스 ID와 JobExecution ID
     * @return batch-was의 강제 종료 응답
     */
    @PostMapping("/stop")
    @PreAuthorize("hasAuthority('BATCH_RUNNING:W')")
    public ApiResponse<?> stopBatch(@RequestBody @Valid BatchStopRequest request) {
        log.info(
                "POST /api/batch/stop - 배치 강제 종료 요청: instanceId={}, jobExecutionId={}",
                request.getInstanceId(),
                request.getJobExecutionId());
        String result = batchRunningService.stopBatch(request);
        return ApiResponse.success("배치 강제 종료 요청이 전송되었습니다.", result);
    }
}

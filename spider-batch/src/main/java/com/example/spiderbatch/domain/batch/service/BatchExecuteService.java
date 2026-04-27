package com.example.spiderbatch.domain.batch.service;

import com.example.spiderbatch.constant.BatchConstants;
import com.example.spiderbatch.domain.batch.dto.BatchExecuteRequest;
import com.example.spiderbatch.domain.batch.dto.BatchExecuteResponse;
import com.example.spiderbatch.domain.batch.mapper.BatchAppMapper;
import com.example.spiderbatch.domain.batch.mapper.BatchHisMapper;
import com.example.spiderbatch.global.log.BatchAuditLogger;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 배치 실행 Service.
 *
 * <p>Admin으로부터 실행 요청을 수신하여 Spring Batch Job을 동기 실행한다.
 * FWK_BATCH_HIS INSERT(STARTED) → Job 실행 → UPDATE(결과) 흐름을 전담한다.</p>
 *
 * <p>Admin의 BatchExecService TODO 주석에서 이관된 책임이다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchExecuteService {

    /** application.yml의 batch.was.instance-id 값 (FWK_WAS_INSTANCE.INSTANCE_ID 와 일치) */
    @Value("${batch.was.instance-id}")
    private String instanceId;

    private final JobRegistry jobRegistry;
    private final JobLauncher jobLauncher;
    private final BatchAppMapper batchAppMapper;
    private final BatchHisMapper batchHisMapper;
    /** Micrometer MeterRegistry: batch.job.duration / batch.job.status / batch.step.write.count 기록 */
    private final MeterRegistry meterRegistry;
    private final BatchAuditLogger auditLogger;

    /**
     * 배치 Job을 동기 실행하고 FWK_BATCH_HIS에 이력을 기록한다.
     *
     * <p>실행 흐름:
     * <ol>
     *   <li>FWK_BATCH_APP에서 BATCH_APP_FILE_NAME(= Job Bean 이름) 조회</li>
     *   <li>다음 실행 회차(BATCH_EXECUTE_SEQ) 계산</li>
     *   <li>FWK_BATCH_HIS INSERT (RES_RT_CODE='0', STARTED)</li>
     *   <li>JobRegistry에서 Job 조회 → JobLauncher.run()</li>
     *   <li>완료 → UPDATE (RES_RT_CODE='1', SUCCESS)</li>
     *   <li>실패 → UPDATE (RES_RT_CODE='9', ABNORMAL_TERMINATION + ERROR_REASON)</li>
     * </ol>
     * </p>
     *
     * @param request   Admin에서 전달된 실행 요청
     * @param requestIp 요청 클라이언트 IP (감사 로그용)
     * @return 실행 결과 (seq, 결과 코드, 처리 건수 등)
     */
    public BatchExecuteResponse execute(BatchExecuteRequest request, String requestIp) {
        String userId = resolveUserId(request.getUserId());

        // 실행 요청 감사 로그 — 누가 어느 배치를 어디서 요청했는지 기록
        auditLogger.logRequest(request.getBatchAppId(), userId, requestIp);

        // 1. FWK_BATCH_APP에서 Job Bean 이름 조회
        String batchAppFileName = batchAppMapper.selectBatchAppFileName(request.getBatchAppId());
        if (batchAppFileName == null || batchAppFileName.isBlank()) {
            throw new IllegalArgumentException("등록되지 않은 배치입니다: batchAppId=" + request.getBatchAppId());
        }

        // 2. 다음 실행 회차 계산
        int nextSeq = batchHisMapper.selectNextExecuteSeq(
                request.getBatchAppId(), instanceId, request.getBatchDate());

        // 3. FWK_BATCH_HIS INSERT (STARTED)
        String logDtime = LocalDateTime.now().format(BatchConstants.LOG_DATE_TIME_FORMATTER);
        batchHisMapper.insertBatchHis(
                request.getBatchAppId(), instanceId, request.getBatchDate(),
                nextSeq, logDtime, userId);

        log.info("배치 실행 시작: batchAppId={}, jobName={}, seq={}, batchDate={}",
                request.getBatchAppId(), batchAppFileName, nextSeq, request.getBatchDate());

        // Job 실행 시간 측정 시작 (Prometheus Timer 기록에 사용)
        long startNanos = System.nanoTime();

        // 4. Job 조회 및 실행
        JobExecution jobExecution;
        try {
            Job job = jobRegistry.getJob(batchAppFileName);
            JobParameters params = buildJobParameters(request, nextSeq);
            jobExecution = jobLauncher.run(job, params);
        } catch (NoSuchJobException e) {
            // JobRegistry에 등록되지 않은 Job 이름
            String errorReason = "Job을 찾을 수 없습니다: " + batchAppFileName;
            log.error("배치 실행 실패 - {}: batchAppId={}", errorReason, request.getBatchAppId());
            BatchExecuteResponse response =
                    updateAsError(request.getBatchAppId(), request.getBatchDate(), nextSeq, userId, errorReason);
            // Job 조회 실패도 비정상 종료로 기록 — 처리 건수 없음
            recordMetrics(request.getBatchAppId(), response.getResRtCode(), startNanos, 0L, 0L);
            auditLogger.logFailure(request.getBatchAppId(), nextSeq, errorReason);
            return response;
        } catch (Exception e) {
            String errorReason = "Job 실행 오류: " + e.getMessage();
            log.error("배치 실행 중 예외 발생: batchAppId={}", request.getBatchAppId(), e);
            BatchExecuteResponse response =
                    updateAsError(request.getBatchAppId(), request.getBatchDate(), nextSeq, userId, errorReason);
            // 예외 발생 시도 비정상 종료로 기록 — 처리 건수 없음
            recordMetrics(request.getBatchAppId(), response.getResRtCode(), startNanos, 0L, 0L);
            auditLogger.logFailure(request.getBatchAppId(), nextSeq, errorReason);
            return response;
        }

        // 5. 실행 결과에 따라 FWK_BATCH_HIS UPDATE
        BatchExecuteResponse response = updateResult(request, nextSeq, userId, jobExecution);

        // 6. Job 완료 후 StepExecution 기준으로 처리 건수 집계 후 메트릭 기록
        long wc = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();
        long sc = jobExecution.getStepExecutions().stream()
                .mapToLong(s -> s.getReadSkipCount() + s.getProcessSkipCount() + s.getWriteSkipCount())
                .sum();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        recordMetrics(request.getBatchAppId(), response.getResRtCode(), startNanos, wc, sc);

        // 성공/실패 여부에 따라 감사 로그 분기
        if (BatchConstants.RES_RT_SUCCESS.equals(response.getResRtCode())) {
            auditLogger.logSuccess(request.getBatchAppId(), nextSeq, durationMs, wc);
        } else {
            // ABNORMAL: updateResult가 비정상 종료로 판단한 경우
            auditLogger.logFailure(request.getBatchAppId(), nextSeq, response.getErrorReason());
        }

        return response;
    }

    /**
     * JobParameters 구성.
     * run.id에 타임스탬프를 포함해 동일 파라미터로 재실행 가능하게 한다.
     */
    private JobParameters buildJobParameters(BatchExecuteRequest request, int seq) {
        JobParametersBuilder builder = new JobParametersBuilder()
                .addString("batchAppId", request.getBatchAppId())
                .addString("batchDate", request.getBatchDate())
                .addString("userId", resolveUserId(request.getUserId()))
                .addLong("batchExecuteSeq", (long) seq)
                // 동일 파라미터 조합으로 재실행 허용 (Spring Batch의 중복 실행 방지 우회)
                .addLong("run.id", System.currentTimeMillis());

        if (request.getParameters() != null && !request.getParameters().isBlank()) {
            builder.addString("parameters", request.getParameters());
        }
        return builder.toJobParameters();
    }

    /**
     * Job 실행 완료 후 결과에 따라 FWK_BATCH_HIS UPDATE.
     */
    private BatchExecuteResponse updateResult(
            BatchExecuteRequest request, int seq, String userId, JobExecution jobExecution) {

        String batchEndDtime = LocalDateTime.now().format(BatchConstants.END_DATE_TIME_FORMATTER);

        // readCount: 실제 읽은 건수(전체 처리 대상)
        // writeCount: DB/외부 시스템에 실제 쓴 건수(성공)
        // skipCount: read + process + write skip 합산 — processSkipCount 만 집계하면 누락 발생
        long readCount = jobExecution.getStepExecutions().stream()
                .mapToLong(s -> s.getReadCount())
                .sum();
        long writeCount = jobExecution.getStepExecutions().stream()
                .mapToLong(s -> s.getWriteCount())
                .sum();
        long skipCount = jobExecution.getStepExecutions().stream()
                .mapToLong(s -> s.getReadSkipCount() + s.getProcessSkipCount() + s.getWriteSkipCount())
                .sum();

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("배치 실행 완료: batchAppId={}, seq={}, read={}, write={}, skip={}",
                    request.getBatchAppId(), seq, readCount, writeCount, skipCount);

            // EXECUTE_COUNT=읽은건수, SUCCESS_COUNT=쓴건수, FAIL_COUNT=스킵건수
            int updated = batchHisMapper.updateBatchHisResult(
                    request.getBatchAppId(), instanceId, request.getBatchDate(), seq,
                    BatchConstants.RES_RT_SUCCESS, batchEndDtime,
                    null, readCount, writeCount, skipCount, userId);
            if (updated == 0) {
                log.error("[CRITICAL] FWK_BATCH_HIS UPDATE 0건 — 이력 고착 위험: batchAppId={}, seq={}",
                        request.getBatchAppId(), seq);
            }

            return BatchExecuteResponse.builder()
                    .batchAppId(request.getBatchAppId())
                    .batchExecuteSeq(seq)
                    .resRtCode(BatchConstants.RES_RT_SUCCESS)
                    .batchEndDtime(batchEndDtime)
                    .executeCount(readCount)
                    .build();

        } else {
            // FAILED, STOPPED, UNKNOWN 등 비정상 종료
            String errorReason = collectErrorReason(jobExecution);
            log.warn("배치 비정상 종료: batchAppId={}, seq={}, status={}, read={}, write={}, skip={}, reason={}",
                    request.getBatchAppId(), seq, jobExecution.getStatus(),
                    readCount, writeCount, skipCount, errorReason);

            int updated = batchHisMapper.updateBatchHisResult(
                    request.getBatchAppId(), instanceId, request.getBatchDate(), seq,
                    BatchConstants.RES_RT_ABNORMAL, batchEndDtime,
                    errorReason, readCount, writeCount, skipCount, userId);
            if (updated == 0) {
                log.error("[CRITICAL] FWK_BATCH_HIS UPDATE 0건 — 이력 고착 위험: batchAppId={}, seq={}",
                        request.getBatchAppId(), seq);
            }

            return BatchExecuteResponse.builder()
                    .batchAppId(request.getBatchAppId())
                    .batchExecuteSeq(seq)
                    .resRtCode(BatchConstants.RES_RT_ABNORMAL)
                    .batchEndDtime(batchEndDtime)
                    .errorReason(errorReason)
                    .executeCount(readCount)
                    .build();
        }
    }

    /**
     * Job 실행 전 오류(Job 미발견, 실행 예외) 시 FWK_BATCH_HIS를 ABNORMAL_TERMINATION으로 UPDATE.
     */
    private BatchExecuteResponse updateAsError(
            String batchAppId, String batchDate, int seq, String userId, String errorReason) {

        String batchEndDtime = LocalDateTime.now().format(BatchConstants.END_DATE_TIME_FORMATTER);
        int updated = batchHisMapper.updateBatchHisResult(
                batchAppId, instanceId, batchDate, seq,
                BatchConstants.RES_RT_ABNORMAL, batchEndDtime,
                errorReason, 0L, 0L, 0L, userId);
        if (updated == 0) {
            log.error("[CRITICAL] FWK_BATCH_HIS UPDATE 0건 — 이력 고착 위험: batchAppId={}, seq={}",
                    batchAppId, seq);
        }

        return BatchExecuteResponse.builder()
                .batchAppId(batchAppId)
                .batchExecuteSeq(seq)
                .resRtCode(BatchConstants.RES_RT_ABNORMAL)
                .batchEndDtime(batchEndDtime)
                .errorReason(errorReason)
                .build();
    }

    /**
     * JobExecution의 실패 예외 메시지를 수집해 단일 문자열로 반환.
     * FWK_BATCH_HIS.ERROR_REASON 컬럼 최대 길이(4000)를 고려해 자른다.
     */
    private String collectErrorReason(JobExecution jobExecution) {
        List<Throwable> exceptions = jobExecution.getAllFailureExceptions();
        if (exceptions.isEmpty()) {
            return "BatchStatus=" + jobExecution.getStatus();
        }
        String reason = exceptions.stream()
                .map(Throwable::getMessage)
                .filter(msg -> msg != null)
                .findFirst()
                .orElse("알 수 없는 오류");
        // Oracle VARCHAR2(4000)는 바이트 기준 — 한글(UTF-8 3바이트/자) 1300자 = 3,900바이트
        // 3900자 기준으로 잘라내면 순수 한글 시 11,700바이트로 초과 → SQLGrammarException 발생
        return reason.length() > 1300 ? reason.substring(0, 1300) + "..." : reason;
    }

    /** userId가 null이거나 비어 있으면 SYSTEM으로 대체 */
    private String resolveUserId(String userId) {
        return (userId != null && !userId.isBlank()) ? userId : BatchConstants.SYSTEM_USER_ID;
    }

    /**
     * Prometheus 메트릭을 기록한다.
     *
     * <ul>
     *   <li>{@code batch.job.duration} (Timer): Job 실행 시간 (초 단위, Prometheus에서 _seconds/_count/_sum 자동 생성)</li>
     *   <li>{@code batch.job.status} (Counter): 실행 결과(SUCCESS / ABNORMAL) 누적 카운트</li>
     *   <li>{@code batch.step.write.count} (Counter): 실제 DB 쓰기 처리 건수 누적</li>
     *   <li>{@code batch.step.skip.count} (Counter): read + process + write 스킵 건수 누적</li>
     * </ul>
     *
     * @param batchAppId 배치 앱 ID (태그로 사용)
     * @param resRtCode  결과 코드 ({@link BatchConstants#RES_RT_SUCCESS} or {@link BatchConstants#RES_RT_ABNORMAL})
     * @param startNanos {@link System#nanoTime()} 기준 Job 시작 시각
     * @param writeCount StepExecution 기준 실제 쓰기 건수 합산
     * @param skipCount  StepExecution 기준 전체 스킵 건수 합산
     */
    private void recordMetrics(String batchAppId, String resRtCode, long startNanos,
                               long writeCount, long skipCount) {
        // Job 실행 시간 — Prometheus Timer는 _seconds_count, _seconds_sum, _seconds_bucket 생성
        Timer.builder("batch.job.duration")
                .tag("batchAppId", batchAppId)
                .register(meterRegistry)
                .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);

        // 실행 결과 카운터 — SUCCESS / ABNORMAL 으로 분기
        String statusTag = BatchConstants.RES_RT_SUCCESS.equals(resRtCode) ? "SUCCESS" : "ABNORMAL";
        Counter.builder("batch.job.status")
                .tag("batchAppId", batchAppId)
                .tag("status", statusTag)
                .register(meterRegistry)
                .increment();

        // 처리 건수 — 0건이면 카운터 등록 생략 (불필요한 빈 시계열 방지)
        if (writeCount > 0) {
            Counter.builder("batch.step.write.count")
                    .tag("batchAppId", batchAppId)
                    .register(meterRegistry)
                    .increment(writeCount);
        }

        // 스킵 건수 — 0건이면 카운터 등록 생략
        if (skipCount > 0) {
            Counter.builder("batch.step.skip.count")
                    .tag("batchAppId", batchAppId)
                    .register(meterRegistry)
                    .increment(skipCount);
        }
    }
}

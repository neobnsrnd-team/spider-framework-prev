package com.example.admin_demo.domain.batch.service;

import com.example.admin_demo.domain.batch.dto.BatchAppResponse;
import com.example.admin_demo.domain.batch.dto.BatchExecRequest;
import com.example.admin_demo.domain.batch.dto.BatchHisResponse;
import com.example.admin_demo.domain.batch.enums.BatchResRtCode;
import com.example.admin_demo.domain.batch.mapper.BatchAppMapper;
import com.example.admin_demo.domain.batch.mapper.BatchHisMapper;
import com.example.admin_demo.global.exception.InvalidInputException;
import com.example.admin_demo.global.exception.NotFoundException;
import com.example.admin_demo.global.util.AuditUtil;
import com.example.admin_demo.infra.tcp.adapter.BatchManagementAdapter;
import com.example.spidercommon.infra.tcp.model.ManagementContext;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치 수동 실행 Service.
 *
 * <p>WAS 인스턴스에 TCP 통신(ObjectStream)으로 배치 실행을 위임한다.
 * FWK_BATCH_HIS의 INSERT/UPDATE는 WAS에서 직접 처리한다.</p>
 *
 * <p>기존 HTTP 전송 방식은 주석으로 보존되어 있다 (롤백 대비).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchExecService {

    private final BatchAppMapper batchAppMapper;
    private final BatchHisMapper batchHisMapper;

    /** Admin ↔ batch-was 간 TCP 통신 어댑터 (ObjectStream 방식) */
    private final BatchManagementAdapter batchManagementAdapter;

    // 기존 HTTP 전송 방식(레거시) — TCP 전환 이후 미사용
    // private final RestTemplate restTemplate;
    // private static final String BATCH_EXEC_ENDPOINT = "/api/batch/execute";

    /** TCP 커맨드 상수 — batch-was와 약속된 식별자 */
    private static final String CMD_BATCH_EXEC = "BATCH_EXEC";

    @Transactional
    public List<BatchHisResponse> executeManualBatch(BatchExecRequest requestDTO) {
        // 중복 실행 방지: 이미 실행중(STARTED)인 배치가 있으면 차단
        int executingCount = batchHisMapper.countExecutingByBatchAppId(requestDTO.getBatchAppId());
        if (executingCount > 0) {
            throw new InvalidInputException(
                    "배치(" + requestDTO.getBatchAppId() + ")가 이미 실행중입니다. (" + executingCount + "건) 완료 후 재실행하세요.");
        }

        // 선행 배치 의존성 검증
        validatePreBatchDependency(requestDTO.getBatchAppId());

        String userId = AuditUtil.currentUserId();
        List<BatchHisResponse> results = new ArrayList<>();

        // 각 WAS 인스턴스에 배치 실행 요청 (FWK_BATCH_HIS INSERT/UPDATE는 WAS가 직접 수행)
        for (String instanceId : requestDTO.getInstanceIds()) {
            boolean sent = sendBatchExecRequest(instanceId, requestDTO, userId);

            // WAS 응답 결과를 그대로 반영 (실제 이력은 WAS가 기록)
            results.add(BatchHisResponse.builder()
                    .batchAppId(requestDTO.getBatchAppId())
                    .instanceId(instanceId)
                    .batchDate(requestDTO.getBatchDate())
                    .lastUpdateUserId(userId)
                    .resRtCode(sent ? BatchResRtCode.SUCCESS.getCode() : BatchResRtCode.ABNORMAL_TERMINATION.getCode())
                    .build());
        }

        return results;
    }

    /**
     * WAS 인스턴스에 배치 실행 요청을 TCP(ObjectStream)로 전송한다.
     * FWK_BATCH_HIS INSERT(STARTED) → Job 실행 → UPDATE(결과)는 WAS가 전담한다.
     *
     * @return true: 배치가 성공적으로 완료됨, false: 실패 또는 전송 불가
     */
    private boolean sendBatchExecRequest(String instanceId, BatchExecRequest requestDTO, String userId) {

        // WAS 인스턴스 조회는 BatchManagementAdapter가 전담하므로 Service에서 중복 조회하지 않는다.

        // TCP 전송을 위한 ManagementContext 구성
        ManagementContext ctx = ManagementContext.builder()
                .command(CMD_BATCH_EXEC)
                .instanceId(instanceId)
                .batchAppId(requestDTO.getBatchAppId())
                .batchDate(requestDTO.getBatchDate())
                .userId(userId)
                .parameters(
                        (requestDTO.getParameters() != null
                                        && !requestDTO.getParameters().isBlank())
                                ? requestDTO.getParameters()
                                : null)
                .build();

        log.info("배치 실행 TCP 요청: instanceId={}, batchAppId={}", instanceId, requestDTO.getBatchAppId());

        // BatchManagementAdapter가 내부적으로 TcpClient.sendObject(ObjectStream)를 호출한다.
        // 실패 시 resultCode="ERROR" ManagementContext를 반환한다 (예외를 던지지 않음).
        ManagementContext response = batchManagementAdapter.doProcess(CMD_BATCH_EXEC, ctx);

        if (response == null) {
            log.warn("배치 실행 응답 없음: instanceId={}", instanceId);
            return false;
        }

        if ("ERROR".equals(response.getResultCode())
                || (response.getErrorMessage() != null
                        && !response.getErrorMessage().isBlank())) {
            log.warn(
                    "배치 실행 실패: instanceId={}, resultCode={}, error={}",
                    instanceId,
                    response.getResultCode(),
                    response.getErrorMessage());
            return false;
        }

        // BatchResRtCode.SUCCESS("1") 인 경우에만 성공 처리
        boolean success = BatchResRtCode.SUCCESS.getCode().equals(response.getResultCode());
        if (success) {
            log.info("배치 실행 완료: instanceId={}, resultCode={}", instanceId, response.getResultCode());
        } else {
            log.warn("배치 비정상 종료: instanceId={}, resultCode={}", instanceId, response.getResultCode());
        }
        return success;

        /* --------------------------------------------------------------------
         * [LEGACY] HTTP 전송 방식 (TCP 전환 이전) — 다양한 통신 방식을 구현했음을 보여주기 위해 주석으로 보존
         *
         * String port = instance.getPort();
         * if (port == null || port.isBlank()) {
         *     log.warn("배치 실행 요청 실패: PORT 정보 없음. instanceId={}", instanceId);
         *     return false;
         * }
         * String url = String.format("http://%s:%s%s", ip, port, BATCH_EXEC_ENDPOINT);
         * try {
         *     HttpHeaders headers = new HttpHeaders();
         *     headers.setContentType(MediaType.APPLICATION_JSON);
         *
         *     Map<String, String> body = new HashMap<>();
         *     body.put("batchAppId", requestDTO.getBatchAppId());
         *     body.put("batchDate", requestDTO.getBatchDate());
         *     body.put("userId", userId);
         *     if (requestDTO.getParameters() != null && !requestDTO.getParameters().isBlank()) {
         *         body.put("parameters", requestDTO.getParameters());
         *     }
         *
         *     HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(body, headers);
         *     log.info("배치 실행 요청: instanceId={}, url={}, batchAppId={}", instanceId, url, requestDTO.getBatchAppId());
         *     ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);
         *
         *     if (response.getStatusCode().is2xxSuccessful()) {
         *         log.info("배치 실행 완료: instanceId={}", instanceId);
         *         return true;
         *     } else {
         *         log.warn("배치 실행 실패 응답: instanceId={}, status={}", instanceId, response.getStatusCode());
         *         return false;
         *     }
         * } catch (RestClientException e) {
         *     log.warn("배치 실행 실패: instanceId={}, url={}, error={}", instanceId, url, e.getMessage());
         *     return false;
         * }
         * -------------------------------------------------------------------- */
    }

    /**
     * 선행 배치 의존성 검증.
     * 선행 배치가 설정되어 있으면 최근 실행 상태가 SUCCESS인지 확인한다.
     */
    private void validatePreBatchDependency(String batchAppId) {
        BatchAppResponse batchApp = batchAppMapper.selectResponseById(batchAppId);
        if (batchApp == null) {
            throw new NotFoundException("batchAppId: " + batchAppId);
        }

        String preBatchAppId = batchApp.getPreBatchAppId();
        if (preBatchAppId == null || preBatchAppId.isBlank()) {
            return;
        }

        if (batchAppMapper.countByBatchAppId(preBatchAppId) == 0) {
            throw new InvalidInputException("선행 배치가 존재하지 않습니다: " + preBatchAppId);
        }

        String latestStatus = batchHisMapper.selectLatestStatusByBatchAppId(preBatchAppId);

        if (latestStatus == null) {
            throw new InvalidInputException("선행 배치(" + preBatchAppId + ")의 실행 이력이 없습니다. 선행 배치를 먼저 실행하세요.");
        }

        if (!BatchResRtCode.SUCCESS.getCode().equals(latestStatus)) {
            BatchResRtCode status = BatchResRtCode.fromCode(latestStatus);
            String statusName = (status != null) ? status.getDescription() : latestStatus;
            throw new InvalidInputException(
                    "선행 배치(" + preBatchAppId + ")의 최근 실행 상태가 '" + statusName + "'입니다. 선행 배치가 정상 종료된 후 실행하세요.");
        }
    }

    public BatchHisResponse getBatchHis(
            String batchAppId, String instanceId, String batchDate, Integer batchExecuteSeq) {
        BatchHisResponse result =
                batchHisMapper.findByIdWithDetails(batchAppId, instanceId, batchDate, batchExecuteSeq);

        if (result == null) {
            throw new NotFoundException(String.format(
                    "batchAppId: %s, instanceId: %s, batchDate: %s, seq: %d",
                    batchAppId, instanceId, batchDate, batchExecuteSeq));
        }

        return result;
    }
}

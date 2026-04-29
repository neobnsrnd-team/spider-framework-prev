package com.example.spider_admin.domain.emergencynotice.service;

import com.example.spider_admin.domain.emergencynotice.dto.EmergencyNoticeDeployStatusResponse;
import com.example.spider_admin.domain.emergencynotice.dto.EmergencyNoticeHistoryResponse;
import com.example.spider_admin.domain.emergencynotice.dto.EmergencyNoticeResponse;
import com.example.spider_admin.domain.emergencynotice.mapper.EmergencyNoticeDeployMapper;
import com.example.spider_admin.domain.emergencynotice.mapper.EmergencyNoticeMapper;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import com.example.spider_admin.infra.tcp.adapter.BizChannelAdapter;
import com.example.spider_admin.infra.tcp.model.JsonCommandRequest;
import com.example.spider_admin.infra.tcp.model.JsonCommandResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 긴급공지 배포 관리 서비스
 *
 * <p>배포 라이프사이클(DRAFT → DEPLOYED → ENDED)을 관리하고,
 * biz-channel에 TCP 통신을 통해 배포 상태를 동기화한다.
 *
 * <p>흐름:
 * <ol>
 *   <li>쓰기 트랜잭션 진입 시 DEPLOY_STATUS 행을 SELECT FOR UPDATE로 잠금 (TOCTOU 방지)</li>
 *   <li>Admin DB 업데이트 (FWK_PROPERTY DEPLOY_STATUS 변경) + 이력 스냅샷 삽입</li>
 *   <li>트랜잭션 커밋 후 biz-channel TCP 호출 (비치명적 — 실패 시 재기동 후 복구됨)</li>
 * </ol>
 *
 * <p>기존 HTTP REST 전송 방식은 주석으로 보존되어 있다 (롤백 대비).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmergencyNoticeDeployService {

    /** 배포 상태 상수 */
    private static final String STATUS_DEPLOYED = "DEPLOYED";

    private static final String STATUS_ENDED = "ENDED";

    /** TCP 커맨드 상수 — biz-channel과 약속된 식별자 */
    private static final String CMD_NOTICE_SYNC = "NOTICE_SYNC";

    private static final String CMD_NOTICE_END = "NOTICE_END";

    // 기존 HTTP REST 전송 경로(레거시) — TCP 전환 이후 미사용
    // private static final String SYNC_PATH = "/api/notices/sync";
    // private static final String END_PATH = "/api/notices/end";
    // private static final String ADMIN_SECRET_HEADER = "X-Admin-Secret";

    private final EmergencyNoticeDeployMapper emergencyNoticeDeployMapper;
    private final EmergencyNoticeMapper emergencyNoticeMapper;

    /** Admin ↔ biz-channel 간 TCP 통신 어댑터 (JSON 프로토콜) */
    private final BizChannelAdapter bizChannelAdapter;

    // 기존 HTTP 호출 설정(레거시) — TCP 전환 이후 미사용
    // private final RestTemplate restTemplate;
    // @Value("${biz-channel.url:http://localhost:3001}")
    // private String bizChannelUrl;
    // @Value("${biz-channel.admin-secret:admin-secret}")
    // private String adminSecret;

    /**
     * 현재 배포 상태, 노출 설정, 이력(페이징·필터 적용)을 함께 반환한다.
     *
     * @param reason   구분 필터 (null·빈문자열이면 전체 조회)
     * @param page     페이지 번호 (1부터)
     * @param pageSize 페이지당 건수
     */
    public Map<String, Object> getDeployInfo(String reason, int page, int pageSize) {
        EmergencyNoticeDeployStatusResponse status = selectDeployStatusOrThrow();
        int offset = (page - 1) * pageSize;
        List<EmergencyNoticeHistoryResponse> history =
                emergencyNoticeDeployMapper.selectHistory(reason, offset, pageSize);
        int historyTotal = emergencyNoticeDeployMapper.selectHistoryCount(reason);
        Map<String, Object> result = new HashMap<>();
        result.put("deployStatus", status.getDeployStatus());
        result.put("startDtime", status.getStartDtime());
        result.put("endDtime", status.getEndDtime());
        result.put("closeableYn", status.getCloseableYn() != null ? status.getCloseableYn() : "Y");
        result.put("hideTodayYn", status.getHideTodayYn() != null ? status.getHideTodayYn() : "Y");
        result.put("history", history);
        result.put("historyTotal", historyTotal);
        result.put("historyPage", page);
        result.put("historyPageSize", pageSize);
        return result;
    }

    /**
     * 긴급공지를 배포한다.
     *
     * <ol>
     *   <li>DEPLOY_STATUS 행을 SELECT FOR UPDATE로 잠금 — 동시 배포 요청 직렬화</li>
     *   <li>DEPLOY_STATUS='DEPLOYED', START_DTIME=now, END_DTIME=NULL 행 업데이트</li>
     *   <li>배포 이력 스냅샷 삽입</li>
     *   <li>커밋 완료 후 biz-channel에 TCP NOTICE_SYNC 커맨드 전송</li>
     * </ol>
     */
    @Transactional
    public void deploy() {
        EmergencyNoticeDeployStatusResponse status = selectDeployStatusForUpdateOrThrow();
        if (STATUS_DEPLOYED.equals(status.getDeployStatus())) {
            throw new InvalidInputException("이미 배포 중입니다. 배포 종료 후 다시 시도해주세요.");
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        emergencyNoticeDeployMapper.updateDeployStart(now, now, userId);
        emergencyNoticeDeployMapper.insertHistorySnapshot("배포", now, userId);

        log.info("긴급공지 배포 완료: userId={}, startDtime={}", userId, now);

        // 트랜잭션 내에서 페이로드를 조회한 뒤, 커밋 완료 후에 TCP 호출
        // → DB 커밋 전에 biz-channel이 호출되는 순서 역전 문제 방지
        Map<String, Object> payload = buildSyncPayload();
        registerAfterCommit(() -> doSyncToBizChannel(payload));
    }

    /**
     * 긴급공지 배포를 종료한다.
     *
     * <ol>
     *   <li>DEPLOY_STATUS 행을 SELECT FOR UPDATE로 잠금</li>
     *   <li>DEPLOY_STATUS='ENDED', END_DTIME=now 행 업데이트</li>
     *   <li>배포 종료 이력 스냅샷 삽입</li>
     *   <li>커밋 완료 후 biz-channel에 TCP NOTICE_END 커맨드 전송</li>
     * </ol>
     */
    @Transactional
    public void endDeploy() {
        EmergencyNoticeDeployStatusResponse status = selectDeployStatusForUpdateOrThrow();
        if (!STATUS_DEPLOYED.equals(status.getDeployStatus())) {
            throw new InvalidInputException("배포 중인 공지가 없습니다. 먼저 배포를 진행해주세요.");
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        emergencyNoticeDeployMapper.updateDeployEnd(now, now, userId);
        emergencyNoticeDeployMapper.insertHistorySnapshot("배포 종료", now, userId);

        log.info("긴급공지 배포 종료 완료: userId={}, endDtime={}", userId, now);

        // 커밋 완료 후 종료 신호 전송
        registerAfterCommit(this::doEndBizChannelNotice);
    }

    /**
     * 공지 노출 설정(닫기 버튼·체크박스)을 저장한다.
     *
     * <p>배포 중(DEPLOYED) 상태라면 변경 즉시 biz-channel에 재동기화하여
     * 실시간으로 반영한다. (critical 장애 시 즉각 차단 가능)
     *
     * @param closeableYn  닫기 버튼 노출 여부 (Y/N)
     * @param hideTodayYn 오늘 하루 보지 않기 체크박스 노출 여부 (Y/N)
     */
    @Transactional
    public void updateSettings(String closeableYn, String hideTodayYn) {
        // FOR UPDATE로 잠금 확보 + 현재 배포 여부 확인 (쓰기 작업 전 상태 스냅샷)
        EmergencyNoticeDeployStatusResponse status = selectDeployStatusForUpdateOrThrow();

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        emergencyNoticeDeployMapper.updateCloseableYn(closeableYn, now, userId);
        emergencyNoticeDeployMapper.updateHideTodayYn(hideTodayYn, now, userId);
        emergencyNoticeDeployMapper.insertHistorySnapshot("설정 변경", now, userId);

        log.info("긴급공지 설정 변경: closeableYn={}, hideTodayYn={}, userId={}", closeableYn, hideTodayYn, userId);

        // 배포 중이면 변경된 설정을 커밋 후 biz-channel에 즉시 반영
        // buildSyncPayload()는 업데이트 후 새 값을 읽으므로 변경 사항이 반영됨
        if (STATUS_DEPLOYED.equals(status.getDeployStatus())) {
            Map<String, Object> payload = buildSyncPayload();
            registerAfterCommit(() -> doSyncToBizChannel(payload));
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /**
     * DEPLOY_STATUS 행의 배포 상태를 조회하고 없으면 NotFoundException을 던진다.
     * 읽기 전용 트랜잭션({@code getDeployInfo})에서 사용한다.
     */
    private EmergencyNoticeDeployStatusResponse selectDeployStatusOrThrow() {
        EmergencyNoticeDeployStatusResponse status = emergencyNoticeDeployMapper.selectDeployStatus();
        if (status == null) {
            throw new NotFoundException(
                    "긴급공지 초기 데이터가 없습니다. 03_insert_initial_data.sql 실행 후 04_alter_tables.sql을 실행해주세요.");
        }
        return status;
    }

    /**
     * DEPLOY_STATUS 행을 SELECT FOR UPDATE로 잠근 뒤 배포 상태를 반환한다.
     * 동시 요청이 같은 상태를 읽고 중복 배포·종료하는 TOCTOU 경쟁을 방지한다.
     * 반드시 쓰기 트랜잭션(@Transactional) 내에서 호출해야 한다.
     */
    private EmergencyNoticeDeployStatusResponse selectDeployStatusForUpdateOrThrow() {
        EmergencyNoticeDeployStatusResponse status = emergencyNoticeDeployMapper.selectDeployStatusForUpdate();
        if (status == null) {
            throw new NotFoundException(
                    "긴급공지 초기 데이터가 없습니다. 03_insert_initial_data.sql 실행 후 04_alter_tables.sql을 실행해주세요.");
        }
        return status;
    }

    /**
     * 현재 트랜잭션이 커밋된 후 실행할 작업을 등록한다.
     * DB 커밋이 완료된 시점에 외부 시스템(biz-channel)을 호출하여
     * "커밋 전 호출 → biz-channel이 구 상태를 전달받는" 순서 역전을 방지한다.
     */
    private void registerAfterCommit(Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    /**
     * 현재 공지 내용·노출 설정을 DB에서 조회하여 biz-channel 요청 페이로드를 구성한다.
     * 트랜잭션 내에서 호출하므로 해당 트랜잭션의 최신 변경 사항(설정 업데이트 등)이 반영된다.
     */
    private Map<String, Object> buildSyncPayload() {
        List<EmergencyNoticeResponse> notices = emergencyNoticeMapper.selectAll();
        String displayType = emergencyNoticeMapper.selectDisplayType();
        EmergencyNoticeDeployStatusResponse deployStatus = emergencyNoticeDeployMapper.selectDeployStatus();

        List<Map<String, String>> noticePayload = notices.stream()
                .map(n -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("lang", n.getPropertyId());
                    item.put("title", n.getTitle() != null ? n.getTitle() : "");
                    item.put("content", n.getContent() != null ? n.getContent() : "");
                    return item;
                })
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("notices", noticePayload);
        body.put("displayType", displayType != null ? displayType : "N");
        body.put(
                "closeableYn",
                deployStatus != null && deployStatus.getCloseableYn() != null ? deployStatus.getCloseableYn() : "Y");
        body.put(
                "hideTodayYn",
                deployStatus != null && deployStatus.getHideTodayYn() != null ? deployStatus.getHideTodayYn() : "Y");
        return body;
    }

    /**
     * 구성된 페이로드를 biz-channel에 TCP(JSON 프로토콜)로 전송한다.
     * 커밋 후 {@link #registerAfterCommit}에서 호출된다.
     * biz-channel 비가용 시 경고 로그만 출력하고 진행한다
     * (재기동 시 biz-channel의 restoreNoticeState()로 복구).
     */
    private void doSyncToBizChannel(Map<String, Object> body) {
        try {
            JsonCommandRequest request = JsonCommandRequest.builder()
                    .command(CMD_NOTICE_SYNC)
                    .requestId(UUID.randomUUID().toString())
                    .payload(body)
                    .build();

            // BizChannelAdapter가 내부적으로 TcpClient.sendJson(4바이트 prefix + UTF-8 JSON)을 호출한다.
            // 예외를 던지지 않고 실패 시 success=false인 JsonCommandResponse를 반환한다.
            JsonCommandResponse response = bizChannelAdapter.doProcess(CMD_NOTICE_SYNC, request);

            if (response != null && response.isSuccess()) {
                log.info("biz-channel 긴급공지 TCP 동기화 완료: message={}", response.getMessage());
            } else {
                // 비치명적: Admin DB는 이미 커밋됨 — biz-channel 재기동 시 restoreNoticeState()로 자동 복구
                log.warn(
                        "biz-channel TCP 동기화 실패 (비치명적, 재기동 시 자동 복구): error={}",
                        response != null ? response.getError() : "응답 없음");
            }
        } catch (Exception e) {
            log.warn("biz-channel TCP 동기화 실패 (비치명적, 재기동 시 자동 복구): {}", e.getMessage());
        }

        /* --------------------------------------------------------------------
         * [LEGACY] HTTP REST 전송 방식 (TCP 전환 이전) — 롤백 대비 주석으로 보존
         *
         * try {
         *     HttpHeaders headers = new HttpHeaders();
         *     headers.setContentType(MediaType.APPLICATION_JSON);
         *     headers.set(ADMIN_SECRET_HEADER, adminSecret);
         *
         *     restTemplate.postForObject(bizChannelUrl + SYNC_PATH, new HttpEntity<>(body, headers), Void.class);
         *
         *     log.info("biz-channel 긴급공지 동기화 완료: url={}", bizChannelUrl + SYNC_PATH);
         * } catch (Exception e) {
         *     log.warn("biz-channel 동기화 실패 (비치명적, 재기동 시 자동 복구): {}", e.getMessage());
         * }
         * -------------------------------------------------------------------- */
    }

    /**
     * biz-channel에 TCP로 배포 종료 신호를 전송한다.
     * 커밋 후 {@link #registerAfterCommit}에서 호출된다.
     * 실패 시 경고 로그만 출력하고 진행한다.
     */
    private void doEndBizChannelNotice() {
        try {
            JsonCommandRequest request = JsonCommandRequest.builder()
                    .command(CMD_NOTICE_END)
                    .requestId(UUID.randomUUID().toString())
                    // NOTICE_END는 별도 페이로드가 없으므로 빈 Map 전달 (null 방어)
                    .payload(new HashMap<>())
                    .build();

            JsonCommandResponse response = bizChannelAdapter.doProcess(CMD_NOTICE_END, request);

            if (response != null && response.isSuccess()) {
                log.info("biz-channel 긴급공지 TCP 종료 완료: message={}", response.getMessage());
            } else {
                log.warn(
                        "biz-channel TCP 종료 신호 전송 실패 (비치명적): error={}",
                        response != null ? response.getError() : "응답 없음");
            }
        } catch (Exception e) {
            log.warn("biz-channel TCP 종료 신호 전송 실패 (비치명적): {}", e.getMessage());
        }

        /* --------------------------------------------------------------------
         * [LEGACY] HTTP REST 전송 방식 (TCP 전환 이전) — 롤백 대비 주석으로 보존
         *
         * try {
         *     HttpHeaders headers = new HttpHeaders();
         *     headers.setContentType(MediaType.APPLICATION_JSON);
         *     headers.set(ADMIN_SECRET_HEADER, adminSecret);
         *
         *     restTemplate.postForObject(bizChannelUrl + END_PATH, new HttpEntity<>(null, headers), Void.class);
         *
         *     log.info("biz-channel 긴급공지 종료 완료: url={}", bizChannelUrl + END_PATH);
         * } catch (Exception e) {
         *     log.warn("biz-channel 종료 신호 전송 실패 (비치명적): {}", e.getMessage());
         * }
         * -------------------------------------------------------------------- */
    }
}

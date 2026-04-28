package com.example.spiderlink.domain.messageinstance;

import com.example.spiderlink.domain.messageinstance.dto.MessageInstanceInsertRequest;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 전문 이력 비동기 기록 큐.
 *
 * <p>TCP·HTTP 처리 스레드가 DB INSERT를 기다리지 않도록 {@link MessageInstanceInsertRequest}를
 * 큐에 적재하고, 별도 컨슈머 스레드가 {@code FWK_MESSAGE_INSTANCE}에 INSERT한다.</p>
 *
 * <p>큐 용량({@value CAPACITY}건) 초과 시 경고 로그만 출력하고 드랍한다 — 비즈니스 영향 없음.</p>
 *
 * <p>Spring Context 종료 시 {@link SmartLifecycle#stop()}이 호출되어 큐에 남은 항목을 모두
 * 처리한 뒤 컨슈머 스레드를 종료한다.</p>
 */
@Slf4j
public class MessageLogQueue implements SmartLifecycle {

    /** 참고소스(spiderLink_Admin) AsyncQueueManager 권장값 */
    static final int CAPACITY = 256;

    private static final String INSERT_SQL =
            "INSERT INTO FWK_MESSAGE_INSTANCE (" +
            "  MESSAGE_SNO, TRX_ID, ORG_ID, IO_TYPE, REQ_RES_TYPE, MESSAGE_ID," +
            "  TRX_TRACKING_NO, USER_ID, LOG_DTIME, LAST_LOG_DTIME, LAST_RT_CODE," +
            "  INSTANCE_ID, RETRY_TRX_YN, MESSAGE_DATA, TRX_DTIME, CHANNEL_TYPE, URI, SUCCESS_YN" +
            ") VALUES (" +
            "  FWK_MESSAGE_INSTANCE_SEQ.NEXTVAL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?" +
            ")";

    private final LinkedBlockingQueue<MessageInstanceInsertRequest> queue =
            new LinkedBlockingQueue<>(CAPACITY);
    private final JdbcTemplate jdbcTemplate;

    private Thread consumerThread;
    private volatile boolean running = false;

    public MessageLogQueue(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 전문 이력 INSERT 요청을 큐에 적재한다.
     *
     * <p>비블로킹 — 큐가 가득 찬 경우 경고 로그만 출력하고 즉시 반환한다.</p>
     */
    public void put(MessageInstanceInsertRequest req) {
        if (!queue.offer(req)) {
            log.warn("[MessageLogQueue] 큐 용량 초과 — 전문 이력 드랍 (messageId={})", req.getMessageId());
        }
    }

    /** 컨슈머 스레드 시작 — Spring Context 초기화 완료 후 자동 호출 */
    @Override
    public void start() {
        running = true;
        consumerThread = new Thread(this::consume, "msg-log-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("[MessageLogQueue] 전문 이력 비동기 큐 시작 (capacity={})", CAPACITY);
    }

    /**
     * 컨슈머 스레드 종료 — Spring Context 종료 시 자동 호출.
     *
     * <p>큐에 남은 항목을 모두 처리한 뒤 종료한다.</p>
     */
    @Override
    public void stop() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        drainRemaining();
        log.info("[MessageLogQueue] 전문 이력 비동기 큐 종료");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void consume() {
        while (running) {
            try {
                // 100ms 대기 후 재확인 — running 플래그 변경 감지 가능
                MessageInstanceInsertRequest req = queue.poll(100, TimeUnit.MILLISECONDS);
                if (req != null) {
                    insertOne(req);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("[MessageLogQueue] 전문 이력 처리 중 오류: {}", e.getMessage());
            }
        }
    }

    /** Context 종료 시 큐에 남은 항목을 동기로 모두 처리 */
    private void drainRemaining() {
        int drained = 0;
        MessageInstanceInsertRequest req;
        while ((req = queue.poll()) != null) {
            insertOne(req);
            drained++;
        }
        if (drained > 0) {
            log.info("[MessageLogQueue] 종료 시 잔여 {}건 처리 완료", drained);
        }
    }

    private void insertOne(MessageInstanceInsertRequest r) {
        try {
            jdbcTemplate.update(INSERT_SQL,
                    r.getTrxId(), r.getOrgId(), r.getIoType(), r.getReqResType(), r.getMessageId(),
                    r.getTrxTrackingNo(), r.getUserId(), r.getLogDtime(), r.getLastLogDtime(),
                    r.getLastRtCode(), r.getInstanceId(), r.getRetryTrxYn(),
                    r.getMessageData(), r.getTrxDtime(), r.getChannelType(), r.getUri(),
                    r.getSuccessYn()
            );
        } catch (Exception e) {
            log.warn("[MessageLogQueue] DB INSERT 실패 — messageId={}: {}", r.getMessageId(), e.getMessage());
        }
    }
}

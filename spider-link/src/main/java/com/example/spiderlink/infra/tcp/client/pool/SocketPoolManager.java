package com.example.spiderlink.infra.tcp.client.pool;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

/**
 * 전체 소켓 풀 레지스트리.
 *
 * <p>(host:port) 키로 {@link SocketPool}을 지연 생성하고 관리한다.
 * Spring Context 종료 시 {@link SmartLifecycle#stop()}이 자동 호출되어
 * 모든 유휴 소켓을 닫는다.</p>
 *
 * <p>{@link com.example.spiderlink.config.SpiderLinkAutoConfiguration}에 의해
 * 자동으로 빈으로 등록된다.</p>
 */
@Slf4j
public class SocketPoolManager implements SmartLifecycle {

    /** (host:port) → SocketPool 맵 */
    private final ConcurrentHashMap<String, SocketPool> pools = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    /**
     * (host:port) 키에 해당하는 소켓 풀을 반환하고, 없으면 생성한다.
     *
     * @param host 대상 호스트
     * @param port 대상 포트
     * @return 해당 엔드포인트의 SocketPool
     */
    public SocketPool getOrCreate(String host, int port) {
        String key = host + ":" + port;
        return pools.computeIfAbsent(key, k -> {
            log.info("[SocketPoolManager] 소켓 풀 신규 생성: {}", key);
            return new SocketPool(host, port);
        });
    }

    /**
     * 풀에서 소켓을 빌린다.
     *
     * @param host 대상 호스트
     * @param port 대상 포트
     * @return 빌린 PooledSocket
     * @throws IOException 연결 실패 또는 풀 고갈 타임아웃
     */
    public PooledSocket borrow(String host, int port) throws IOException {
        // stop() 이후 신규 소켓 생성 차단 — Context 종료 중 연결 시도 방지
        if (!running) {
            throw new IOException("[SocketPoolManager] 매니저가 중지 상태 — 소켓 대여 불가");
        }
        return getOrCreate(host, port).borrow();
    }

    /**
     * 소켓을 풀에 반납한다.
     *
     * <p>stop() 이후 pools 맵이 비워진 상태에서 지연된 응답이 반납될 수 있으므로,
     * pools.get()으로 기존 풀이 있을 때만 반납하고 없으면 소켓을 즉시 폐기한다.</p>
     *
     * @param host    대상 호스트
     * @param port    대상 포트
     * @param pooled  반납할 소켓
     * @param success 통신 성공 여부 — false면 소켓 폐기
     */
    public void release(String host, int port, PooledSocket pooled, boolean success) {
        SocketPool pool = pools.get(host + ":" + port);
        if (pool != null) {
            pool.release(pooled, success);
        } else {
            // shutdown 완료 후 반납 — 풀이 이미 제거됐으므로 소켓 직접 폐기
            pooled.invalidate();
        }
    }

    /**
     * Admin 모니터링용 — 특정 엔드포인트 풀 상태 문자열 반환.
     *
     * @param host 대상 호스트
     * @param port 대상 포트
     * @return "active=N, idle=M, total=K/MAX_ACTIVE" 형식 문자열
     */
    public String getInfo(String host, int port) {
        String key = host + ":" + port;
        SocketPool pool = pools.get(key);
        return pool != null ? pool.getInfo() : key + " — 풀 없음";
    }

    /**
     * Admin 모니터링용 — 등록된 모든 풀의 상태를 구조화된 맵으로 반환한다.
     *
     * <p>키: "host:port", 값: active / idle / total / maxActive 필드를 담은 맵.</p>
     *
     * @return 엔드포인트별 풀 상태 맵 (등록된 풀이 없으면 빈 맵)
     */
    public java.util.Map<String, java.util.Map<String, Object>> getAllPoolInfo() {
        java.util.Map<String, java.util.Map<String, Object>> result = new java.util.LinkedHashMap<>();
        pools.forEach((key, pool) -> result.put(key, pool.getInfoMap()));
        return result;
    }

    /** 등록된 모든 풀의 상태를 로그에 출력한다 */
    public void logPoolState() {
        if (pools.isEmpty()) {
            log.info("[SocketPoolManager] 등록된 소켓 풀 없음");
            return;
        }
        pools.values().forEach(pool -> log.info("[SocketPoolManager] {}", pool.getInfo()));
    }

    /** Spring Context 초기화 완료 후 자동 호출 */
    @Override
    public void start() {
        running = true;
        log.info("[SocketPoolManager] 소켓 풀 매니저 시작");
    }

    /** Spring Context 종료 시 자동 호출 — 모든 유휴 소켓 닫기 */
    @Override
    public void stop() {
        running = false;
        pools.values().forEach(SocketPool::closeAll);
        pools.clear();
        log.info("[SocketPoolManager] 소켓 풀 매니저 종료 — 모든 풀 닫힘");
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}

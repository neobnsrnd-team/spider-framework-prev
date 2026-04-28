package com.example.spiderlink.infra.tcp.client.pool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import lombok.extern.slf4j.Slf4j;

/**
 * 단일 (host:port) 대상 소켓 커넥션 풀.
 *
 * <p>참고소스(spiderLink_Admin) {@code SocketPool} 설계 기준값을 적용한다.</p>
 *
 * <ul>
 *   <li>최대 소켓 수: {@value MAX_ACTIVE}개</li>
 *   <li>풀 고갈 시 최대 대기: {@value BORROW_WAIT_MS}ms</li>
 *   <li>미사용 소켓 자동 폐기: {@value MAX_IDLE_MS}ms (210초)</li>
 * </ul>
 *
 * <p>유효기간 만료 소켓은 {@link #borrow()} 시점에 탐지·폐기하므로 별도 스케줄러가 필요 없다.
 * 모든 공유 상태는 {@code synchronized} 블록으로 보호하며, 소켓 생성 IO는 락 밖에서 수행한다.</p>
 */
@Slf4j
public class SocketPool {

    /** 최대 소켓 수 (active + idle 합계) — 참고소스 기준값 */
    static final int MAX_ACTIVE = 5;
    /** 풀 고갈 시 최대 대기 시간 — 참고소스 기준값 2초 */
    static final long BORROW_WAIT_MS = 2_000;
    /** 미사용 소켓 자동 폐기 기준 — 참고소스 기준값 210초 */
    static final long MAX_IDLE_MS = 210_000;
    /** 연결 타임아웃 — 참고소스 기준값 2초 */
    public static final int CONNECT_TIMEOUT_MS = 2_000;
    /** 읽기 타임아웃 */
    public static final int READ_TIMEOUT_MS = 60_000;

    private final String host;
    private final int port;

    private final ArrayDeque<PooledSocket> idle = new ArrayDeque<>();
    /** active + idle 합계 — MAX_ACTIVE 상한 적용 */
    private int totalCount = 0;
    private final Object lock = new Object();

    public SocketPool(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 풀에서 소켓을 빌린다.
     *
     * <p>유휴 소켓이 있으면 즉시 반환하고, 없으면 신규 생성을 시도한다.
     * 총 소켓 수가 {@link #MAX_ACTIVE}에 도달한 경우 최대 {@link #BORROW_WAIT_MS}ms 대기 후
     * 타임아웃 예외를 던진다.</p>
     *
     * @throws IOException 연결 실패 또는 풀 고갈 타임아웃
     */
    public PooledSocket borrow() throws IOException {
        long deadline = System.currentTimeMillis() + BORROW_WAIT_MS;
        boolean mustCreate = false;

        synchronized (lock) {
            while (true) {
                // 유효기간 만료된 유휴 소켓 제거
                while (!idle.isEmpty() && !idle.peekFirst().isValid(MAX_IDLE_MS)) {
                    idle.pollFirst().invalidate();
                    totalCount--;
                    log.debug("[SocketPool] 유휴 소켓 만료 폐기 ({}:{})", host, port);
                }

                // 유효한 유휴 소켓 즉시 반환
                if (!idle.isEmpty()) {
                    PooledSocket pooled = idle.pollFirst();
                    pooled.touch();
                    return pooled;
                }

                // 신규 생성 가능 → 카운터 선점 후 락 밖에서 IO 수행
                if (totalCount < MAX_ACTIVE) {
                    totalCount++;
                    mustCreate = true;
                    break;
                }

                // 풀 고갈 — 반납 신호 대기
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new IOException(String.format(
                            "[SocketPool] 소켓 풀 고갈 — 대기 시간 초과 (%s:%d, total=%d/%d)",
                            host, port, totalCount, MAX_ACTIVE));
                }
                try {
                    lock.wait(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("[SocketPool] borrow 대기 중 인터럽트", e);
                }
            }
        }

        // 소켓 생성 — IO는 락 밖에서 수행
        if (mustCreate) {
            try {
                Socket socket = new Socket();
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                socket.setSoTimeout(READ_TIMEOUT_MS);
                log.debug("[SocketPool] 신규 소켓 생성 ({}:{}, total={})", host, port, totalCount);
                return new PooledSocket(socket);
            } catch (IOException e) {
                // 생성 실패 — 선점한 카운터 반환
                synchronized (lock) {
                    totalCount--;
                    lock.notifyAll();
                }
                throw e;
            }
        }

        throw new IOException("[SocketPool] 내부 상태 오류 — 도달 불가 코드");
    }

    /**
     * 소켓을 풀에 반납한다.
     *
     * @param pooled  반납할 소켓
     * @param success 통신 성공 여부 — false면 소켓 폐기
     */
    public void release(PooledSocket pooled, boolean success) {
        if (!success || !pooled.isValid(MAX_IDLE_MS)) {
            pooled.invalidate();
            synchronized (lock) {
                totalCount--;
                lock.notifyAll();
            }
            log.debug("[SocketPool] 오류 소켓 폐기 ({}:{}, total={})", host, port, totalCount);
        } else {
            pooled.touch();
            synchronized (lock) {
                idle.addLast(pooled);
                lock.notifyAll();
            }
        }
    }

    /** 풀 종료 시 모든 유휴 소켓을 닫는다 */
    public void closeAll() {
        synchronized (lock) {
            while (!idle.isEmpty()) {
                idle.pollFirst().invalidate();
                totalCount--;
            }
            lock.notifyAll();
        }
    }

    /** Admin 모니터링용 상태 문자열 */
    public String getInfo() {
        synchronized (lock) {
            int idleCount = idle.size();
            int activeCount = totalCount - idleCount;
            return String.format("[%s:%d] active=%d, idle=%d, total=%d/%d",
                    host, port, activeCount, idleCount, totalCount, MAX_ACTIVE);
        }
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
}

package com.example.spiderbatch.lock;

import com.example.spiderbatch.spi.DistributedLockService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * Redis 분산 락 서비스.
 *
 * <p>Redisson {@link RLock}을 사용해 멀티 인스턴스 환경에서
 * 동일 배치의 중복 실행을 방지한다.</p>
 *
 * <p>락 키: {@code batch:lock:{batchAppId}}<br>
 * leaseTime을 지정하지 않아 Redisson Watchdog이 활성화된다.
 * Watchdog은 배치 실행 중 락을 30초마다 자동 갱신하며, 완료/실패 시 {@link #unlock}에서 명시적으로 해제한다.
 * 프로세스가 비정상 종료되면 Watchdog이 멈추고 기본 TTL(30초) 후 락이 자동 해제된다.</p>
 *
 * <p>이 빈은 {@code redisson-spring-boot-starter}가 클래스패스에 있을 때
 * {@link com.example.spiderbatch.config.RedisLockConfig}에 의해 자동 등록된다.</p>
 *
 * <p>TODO: 운영 환경에서는 RedissonClient를 Sentinel 또는 Cluster 모드로 구성해야 한다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RedisDistributedLockService implements DistributedLockService {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "batch:lock:";

    /**
     * 배치 분산 락 획득 시도.
     * leaseTime 없이 tryLock — Watchdog이 활성화되어 배치 실행 중 락이 자동 갱신된다.
     * 대기 없이 즉시 시도(waitTime=0)하여 이미 락이 있으면 false 반환.
     *
     * @param batchAppId 배치 APP ID
     * @return 락 획득 성공 시 true, 이미 다른 인스턴스가 실행 중이면 false
     */
    @Override
    public boolean tryLock(String batchAppId) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + batchAppId);
        try {
            // leaseTime 생략 → Watchdog 활성화 (배치 실행 시간에 상관없이 락 유지)
            boolean acquired = lock.tryLock(0, TimeUnit.SECONDS);
            if (acquired) {
                log.debug("[분산 락] 획득: batchAppId={}", batchAppId);
            } else {
                log.info("[분산 락] 이미 실행 중 — 스킵: batchAppId={}", batchAppId);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[분산 락] 획득 인터럽트: batchAppId={}", batchAppId);
            return false;
        }
    }

    /**
     * 배치 분산 락 해제.
     * 현재 스레드가 보유한 락만 해제한다. 락이 없거나 다른 스레드 소유이면 무시.
     *
     * @param batchAppId 배치 APP ID
     */
    @Override
    public void unlock(String batchAppId) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + batchAppId);
        // isHeldByCurrentThread: 현재 스레드가 락을 보유한 경우에만 해제
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("[분산 락] 해제: batchAppId={}", batchAppId);
        }
    }
}

package com.example.spiderbatch.lock;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * Redis 분산 락 서비스.
 *
 * <p>Redisson {@link RLock}을 사용해 멀티 인스턴스 환경에서
 * 동일 배치의 중복 실행을 방지한다.</p>
 *
 * <p>락 키: {@code batch:lock:{batchAppId}}<br>
 * 락 만료 시간: {@value LOCK_EXPIRE_SECONDS}초 — 배치 최대 실행 시간(1시간)을 기준으로 설정.
 * 프로세스가 비정상 종료되더라도 만료 시간 후 자동 해제된다.</p>
 *
 * <p>TODO: 운영 환경에서는 RedissonClient를 Sentinel 또는 Cluster 모드로 구성해야 한다.
 * application.yml의 {@code spring.data.redis} 설정을
 * {@code spring.redis.sentinel} 또는 {@code spring.redis.cluster} 블록으로 전환할 것.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDistributedLockService {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "batch:lock:";
    /** 락 만료 시간 (초) — 배치 최대 실행 시간 + 여유 시간 */
    private static final long LOCK_EXPIRE_SECONDS = 3600;

    /**
     * 배치 분산 락 획득 시도.
     * 대기 없이 즉시 시도(tryLock waitTime=0)하여 이미 락이 있으면 false 반환.
     *
     * @param batchAppId 배치 APP ID
     * @return 락 획득 성공 시 true, 이미 다른 인스턴스가 실행 중이면 false
     */
    public boolean tryLock(String batchAppId) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + batchAppId);
        try {
            boolean acquired = lock.tryLock(0, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
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
    public void unlock(String batchAppId) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + batchAppId);
        // isHeldByCurrentThread: 현재 스레드가 락을 보유한 경우에만 해제
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("[분산 락] 해제: batchAppId={}", batchAppId);
        }
    }
}

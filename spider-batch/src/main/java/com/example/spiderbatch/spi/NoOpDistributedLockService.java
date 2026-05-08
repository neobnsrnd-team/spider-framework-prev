package com.example.spiderbatch.spi;

import lombok.extern.slf4j.Slf4j;

/**
 * @file NoOpDistributedLockService.java
 * @description 분산 락 미사용 기본 구현체.
 *
 * <p>Redis(Redisson)가 클래스패스에 없을 때 {@link com.example.spiderbatch.config.SpiderBatchAutoConfiguration}에 의해
 * 자동 등록된다. 단일 인스턴스 환경이나 중복 실행 방지가 불필요한 경우에 사용한다.</p>
 *
 * <p>{@link #tryLock}은 항상 {@code true}를 반환하여 모든 배치 실행 요청을 허용한다.
 * 멀티 인스턴스 환경에서 중복 실행 방지가 필요하다면 {@link com.example.spiderbatch.lock.RedisDistributedLockService}를
 * 활성화하거나 커스텀 구현체를 Bean으로 등록해야 한다.</p>
 */
@Slf4j
public class NoOpDistributedLockService implements DistributedLockService {

    @Override
    public boolean tryLock(String batchAppId) {
        log.debug("[NoOpLock] 락 획득(No-Op): batchAppId={}", batchAppId);
        return true;
    }

    @Override
    public void unlock(String batchAppId) {
        log.debug("[NoOpLock] 락 해제(No-Op): batchAppId={}", batchAppId);
    }
}

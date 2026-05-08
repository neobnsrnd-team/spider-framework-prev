package com.example.spiderbatch.spi;

/**
 * @file DistributedLockService.java
 * @description 분산 락 서비스 SPI 인터페이스.
 *
 * <p>멀티 인스턴스 환경에서 동일 배치의 중복 실행을 방지하는 분산 락 추상화.
 * Redis(Redisson) 구현체는 {@code redisson-spring-boot-starter}가 클래스패스에 있을 때
 * {@link com.example.spiderbatch.config.RedisLockConfig}에 의해 자동 등록된다.
 * Redis 미사용 환경에서는 항상 락을 허용하는 {@link NoOpDistributedLockService}가 기본으로 등록된다.</p>
 *
 * <p>내장 프로젝트에서 다른 락 구현(ZooKeeper 등)을 사용하려면 이 인터페이스를 구현한 Bean을 등록한다.</p>
 */
public interface DistributedLockService {

    /**
     * 배치 분산 락 획득 시도.
     * 대기 없이 즉시 시도하며 이미 다른 인스턴스가 실행 중이면 {@code false} 반환.
     *
     * @param batchAppId 배치 APP ID (락 키 식별자)
     * @return 락 획득 성공 시 {@code true}, 이미 실행 중이면 {@code false}
     */
    boolean tryLock(String batchAppId);

    /**
     * 배치 분산 락 해제.
     * 현재 스레드가 보유한 락만 해제한다. 미보유 상태면 무시.
     *
     * @param batchAppId 배치 APP ID (락 키 식별자)
     */
    void unlock(String batchAppId);
}

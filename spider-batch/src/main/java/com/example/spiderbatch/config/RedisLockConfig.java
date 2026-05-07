package com.example.spiderbatch.config;

import com.example.spiderbatch.lock.RedisDistributedLockService;
import com.example.spiderbatch.spi.DistributedLockService;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 분산 락 자동 설정.
 *
 * <p>{@code redisson-spring-boot-starter}가 클래스패스에 있을 때만 활성화된다.
 * 이미 {@link DistributedLockService} Bean이 등록되어 있으면 이 설정을 건너뛴다(커스텀 구현 우선).</p>
 *
 * <p>Redisson 미포함 환경에서는 이 설정 전체가 스킵되며,
 * {@link com.example.spiderbatch.config.SpiderBatchAutoConfiguration}의
 * {@link com.example.spiderbatch.spi.NoOpDistributedLockService}가 폴백으로 등록된다.</p>
 */
@Configuration
@ConditionalOnClass(RedissonClient.class)
public class RedisLockConfig {

    /**
     * {@link RedissonClient} 빈을 사용하는 Redis 분산 락 서비스.
     * Redisson이 클래스패스에 있고 {@link DistributedLockService} 빈이 없을 때만 등록.
     */
    @Bean
    @ConditionalOnMissingBean(DistributedLockService.class)
    public DistributedLockService distributedLockService(RedissonClient redissonClient) {
        return new RedisDistributedLockService(redissonClient);
    }
}

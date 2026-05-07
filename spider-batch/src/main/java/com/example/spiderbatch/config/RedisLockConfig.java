package com.example.spiderbatch.config;

import com.example.spiderbatch.lock.RedisDistributedLockService;
import com.example.spiderbatch.spi.DistributedLockService;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
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
 * <p>{@link RedissonClient} 빈을 직접 생성하며 {@code lazyInitialization=true}를 적용한다.
 * 이로써 WAS 기동 시 Redis 연결을 즉시 시도하지 않고, 첫 락 획득 요청 시점으로 연결을 미룬다.
 * {@code RedissonAutoConfigurationV2}의 Eager 연결을 우회하기 위해 직접 빈을 생성한다.</p>
 *
 * <p>Redisson 미포함 환경에서는 이 설정 전체가 스킵되며,
 * {@link com.example.spiderbatch.config.SpiderBatchAutoConfiguration}의
 * {@link com.example.spiderbatch.spi.NoOpDistributedLockService}가 폴백으로 등록된다.</p>
 */
@Configuration
@ConditionalOnClass({RedissonClient.class, Redisson.class})
public class RedisLockConfig {

    /** Redis 호스트 (spring.data.redis.host 또는 REDIS_HOST 환경변수) */
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    /** Redis 포트 (spring.data.redis.port 또는 REDIS_PORT 환경변수) */
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * LazyInitialization=true 설정으로 RedissonClient 빈 생성.
     *
     * <p>lazyInitialization=true: WAS 기동 시 Redis 연결을 시도하지 않고,
     * 첫 락 획득 요청 시점으로 연결을 미룬다.
     * YAML config 블록의 lazyInitialization은 RedissonAutoConfigurationV2의 Eager 초기화를
     * 막지 못하므로, Java API로 직접 Config를 구성한다.</p>
     *
     * <p>이미 {@link RedissonClient} 빈이 있으면 스킵 — 커스텀 Redisson 설정 우선.</p>
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 첫 실제 Redis 명령 시점까지 연결 지연 — Redis 미기동 환경에서도 WAS 정상 기동
        config.setLazyInitialization(true);
        config.useSingleServer()
              .setAddress("redis://" + redisHost + ":" + redisPort);
        return Redisson.create(config);
    }

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

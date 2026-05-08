package com.example.spiderbatch.spi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @file NoOpDistributedLockServiceTest.java
 * @description {@link NoOpDistributedLockService} 단위 테스트.
 */
class NoOpDistributedLockServiceTest {

    private final NoOpDistributedLockService lockService = new NoOpDistributedLockService();

    @Test
    void tryLock_항상_true_반환() {
        assertThat(lockService.tryLock("batchA")).isTrue();
        assertThat(lockService.tryLock("batchB")).isTrue();
        assertThat(lockService.tryLock("batchA")).isTrue(); // 중복 획득도 허용
    }

    @Test
    void unlock_예외_없이_완료() {
        // NoOp이므로 예외 없이 정상 종료해야 함
        lockService.unlock("batchA");
        lockService.unlock("nonExistentBatch");
    }

    @Test
    void 연속_tryLock_unlock_정상_동작() {
        assertThat(lockService.tryLock("batchC")).isTrue();
        lockService.unlock("batchC");
        assertThat(lockService.tryLock("batchC")).isTrue(); // 해제 후 재획득
    }
}

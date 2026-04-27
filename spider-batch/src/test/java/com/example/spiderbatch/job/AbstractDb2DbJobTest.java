package com.example.spiderbatch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@DisplayName("AbstractDb2DbJob 템플릿 메서드 테스트")
class AbstractDb2DbJobTest {

    /** 테스트용 최소 구현체 */
    private final AbstractDb2DbJob<Object> job = new AbstractDb2DbJob<>() {
        @Override
        protected String getJobName() { return "testJob"; }
    };

    @Test
    @DisplayName("getPartitionStepName 기본값 — getJobName() + 'PartitionStep'")
    void getPartitionStepName_defaultConcatsJobName() {
        assertThat(job.getPartitionStepName()).isEqualTo("testJobPartitionStep");
    }

    @Test
    @DisplayName("getPartitionStepName 재정의 — 소비자가 원하는 이름으로 변경 가능")
    void getPartitionStepName_overridable() {
        AbstractDb2DbJob<Object> customJob = new AbstractDb2DbJob<>() {
            @Override
            protected String getJobName() { return "db2db"; }

            @Override
            protected String getPartitionStepName() { return "db2DbPartitionStep"; }
        };

        assertThat(customJob.getPartitionStepName()).isEqualTo("db2DbPartitionStep");
    }

    @Test
    @DisplayName("getGridSize 기본값 — 4")
    void getGridSize_defaultIs4() {
        assertThat(job.getGridSize()).isEqualTo(4);
    }

    @Test
    @DisplayName("getPageSize 기본값 — 5")
    void getPageSize_defaultIs5() {
        assertThat(job.getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("getSkipLimit 기본값 — 10")
    void getSkipLimit_defaultIs10() {
        assertThat(job.getSkipLimit()).isEqualTo(10);
    }

    @Test
    @DisplayName("buildTaskExecutor — ThreadPoolTaskExecutor 반환, initialize() 미호출(Spring @Bean이 afterPropertiesSet 담당)")
    void buildTaskExecutor_returnsExecutorWithoutInitializing() {
        ThreadPoolTaskExecutor executor = job.buildTaskExecutor();

        assertThat(executor).isNotNull();
        // Spring @Bean으로 등록 시 afterPropertiesSet()이 initialize()를 호출하므로
        // buildTaskExecutor()는 속성 설정만 한다. initialize()를 직접 호출해도 오류가 없어야 한다.
        assertThatCode(executor::initialize).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("buildTaskExecutor — 스레드 이름 접두사가 jobName으로 설정된다")
    void buildTaskExecutor_threadNamePrefixContainsJobName() {
        ThreadPoolTaskExecutor executor = job.buildTaskExecutor();

        assertThat(executor.getThreadNamePrefix()).isEqualTo("testJob-partition-");
    }
}

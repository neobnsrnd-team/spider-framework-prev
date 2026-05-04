package com.example.spiderbatch.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AbstractDb2ForeignJob 템플릿 메서드 테스트")
class AbstractDb2ForeignJobTest {

    /** 테스트용 최소 구현체 */
    private final AbstractDb2ForeignJob<Object> job = new AbstractDb2ForeignJob<>() {
        @Override
        protected String getJobName() { return "testForeignJob"; }

        @Override
        protected Class<? extends Throwable> getSkippableException() {
            return IllegalArgumentException.class;
        }
    };

    @Test
    @DisplayName("getPageSize 기본값 — 5")
    void getPageSize_defaultIs5() {
        assertThat(job.getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("getSkipLimit 기본값 — 5")
    void getSkipLimit_defaultIs5() {
        assertThat(job.getSkipLimit()).isEqualTo(5);
    }

    @Test
    @DisplayName("getSkippableException — 내장 프로젝트에서 지정한 예외 타입을 반환한다")
    void getSkippableException_returnsConfigured() {
        assertThat(job.getSkippableException()).isEqualTo(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getSkippableException — 다른 예외 타입도 정상 반환한다")
    void getSkippableException_differentExceptionType() {
        AbstractDb2ForeignJob<Object> customJob = new AbstractDb2ForeignJob<>() {
            @Override
            protected String getJobName() { return "customJob"; }

            @Override
            protected Class<? extends Throwable> getSkippableException() {
                return RuntimeException.class;
            }
        };

        assertThat(customJob.getSkippableException()).isEqualTo(RuntimeException.class);
    }
}

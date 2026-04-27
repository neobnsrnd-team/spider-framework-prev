package com.example.spiderbatch.job;

import com.example.spiderbatch.job.common.BatchJobParametersValidator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * DB → 외부 시스템 HTTP 전문 연계 배치 Job의 추상 기반 클래스.
 *
 * <p>단일 Chunk Step 기반 Job의 공통 설정(Job 빌더, Step)을 템플릿 메서드로 제공한다.
 * {@link #getSkippableException()}으로 skip 대상 예외를 지정하고,
 * {@code RuntimeException.class}는 항상 noSkip으로 처리하여 프로그래밍 오류가
 * 의도치 않게 삼켜지는 것을 방지한다.</p>
 *
 * <pre>{@code
 * @Configuration
 * @RequiredArgsConstructor
 * public class Db2ForeignJobConfig extends AbstractDb2ForeignJob<CardUsage> {
 *
 *     @Override
 *     protected String getJobName() { return "db2foreign"; }
 *
 *     @Override
 *     protected Class<? extends Throwable> getSkippableException() {
 *         return ExternalTransferException.class;
 *     }
 *     // ...
 * }
 * }</pre>
 *
 * @param <T> 읽기/쓰기 대상 도메인 타입
 */
public abstract class AbstractDb2ForeignJob<T> {

    /** Job 이름. FWK_BATCH_APP.BATCH_APP_FILE_NAME과 일치해야 한다. */
    protected abstract String getJobName();

    /** 페이지당 읽을 건수. 기본값 5. */
    protected int getPageSize() {
        return 5;
    }

    /** Step에서 허용할 최대 스킵 건수. 기본값 5. */
    protected int getSkipLimit() {
        return 5;
    }

    /**
     * Step에서 skip 처리할 예외 타입을 반환한다.
     * 그 외 {@code RuntimeException}은 noSkip으로 즉시 Step 실패 처리된다.
     */
    protected abstract Class<? extends Throwable> getSkippableException();

    /** {@link BatchJobParametersValidator}를 적용한 Job을 생성한다. */
    protected Job buildJob(JobRepository jobRepository, Step step) {
        return new JobBuilder(getJobName(), jobRepository)
                .validator(new BatchJobParametersValidator())
                .start(step)
                .build();
    }

    /**
     * chunk 기반 단일 Step을 생성한다.
     *
     * <p>{@link #getSkippableException()} 타입만 skip 처리하고
     * 그 외 {@code RuntimeException}은 noSkip으로 즉시 실패한다.</p>
     *
     * @param stepName step 이름
     * @param reader   ItemReader
     * @param writer   ItemWriter
     */
    protected Step buildStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager,
                             String stepName,
                             ItemReader<T> reader,
                             ItemWriter<T> writer) {
        return new StepBuilder(stepName, jobRepository)
                .<T, T>chunk(getPageSize(), transactionManager)
                .reader(reader)
                .writer(writer)
                .faultTolerant()
                .skip(getSkippableException())
                .noSkip(RuntimeException.class)
                .skipLimit(getSkipLimit())
                .allowStartIfComplete(false)
                .build();
    }
}

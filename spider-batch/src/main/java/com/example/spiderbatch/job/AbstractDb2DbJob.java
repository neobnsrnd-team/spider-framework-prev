package com.example.spiderbatch.job;

import com.example.spiderbatch.job.common.BatchJobParametersValidator;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * DB → DB 아카이브 배치 Job의 추상 기반 클래스.
 *
 * <p>파티셔닝 기반 병렬 처리 Job의 공통 설정(Job 빌더, 파티션 Step, 파티션 핸들러,
 * 스레드 풀, 워커 Step)을 템플릿 메서드로 제공한다. 소비자 {@code @Configuration}에서
 * {@code @Bean} 메서드가 이 클래스의 {@code build*} 메서드를 호출하는 방식으로 사용한다.</p>
 *
 * <pre>{@code
 * @Configuration
 * @RequiredArgsConstructor
 * public class Db2DbJobConfig extends AbstractDb2DbJob<CardUsage> {
 *
 *     @Override
 *     protected String getJobName() { return "db2db"; }
 *
 *     @Bean(name = "db2db")
 *     public Job db2DbJob(JobRepository jobRepository, Step db2DbPartitionStep) {
 *         return buildJob(jobRepository, db2DbPartitionStep);
 *     }
 *     // ...
 * }
 * }</pre>
 *
 * @param <T> 읽기/쓰기 대상 도메인 타입
 */
public abstract class AbstractDb2DbJob<T> {

    /** Job 이름. FWK_BATCH_APP.BATCH_APP_FILE_NAME과 일치해야 한다. */
    protected abstract String getJobName();

    /** 페이지당 읽을 건수. 기본값 5. */
    protected int getPageSize() {
        return 5;
    }

    /** 병렬 파티션(스레드) 수. 기본값 4. */
    protected int getGridSize() {
        return 4;
    }

    /** 워커 Step에서 허용할 최대 스킵 건수. 기본값 10. */
    protected int getSkipLimit() {
        return 10;
    }

    /**
     * 파티션 매니저 Step 이름. 기본값은 {@code getJobName() + "PartitionStep"}.
     * 소비자가 이름을 변경하고 싶을 때 재정의한다.
     */
    protected String getPartitionStepName() {
        return getJobName() + "PartitionStep";
    }

    /** {@link BatchJobParametersValidator}를 적용한 Job을 생성한다. */
    protected Job buildJob(JobRepository jobRepository, Step partitionStep) {
        return new JobBuilder(getJobName(), jobRepository)
                .validator(new BatchJobParametersValidator())
                .start(partitionStep)
                .build();
    }

    /**
     * Partitioner와 TaskExecutorPartitionHandler를 사용하는 매니저 Step을 생성한다.
     *
     * @param workerStepName 파티션 키 접두사로 사용할 워커 Step 이름
     * @param partitioner    범위 분할 전략 (ColumnRangePartitioner 등)
     * @param workerStep     병렬 실행될 워커 Step Bean
     * @param taskExecutor   파티션 병렬 실행 스레드 풀 (Spring @Bean으로 관리해야 graceful shutdown 보장)
     */
    protected Step buildPartitionStep(JobRepository jobRepository,
                                      String workerStepName,
                                      Partitioner partitioner,
                                      Step workerStep,
                                      TaskExecutor taskExecutor) {
        return new StepBuilder(getPartitionStepName(), jobRepository)
                .partitioner(workerStepName, partitioner)
                .partitionHandler(buildPartitionHandler(workerStep, taskExecutor))
                .allowStartIfComplete(false)
                .build();
    }

    /**
     * TaskExecutorPartitionHandler를 gridSize 기준으로 생성한다.
     *
     * @param taskExecutor Spring @Bean으로 관리되는 TaskExecutor — Spring이 destroy() 호출 보장
     */
    protected PartitionHandler buildPartitionHandler(Step workerStep, TaskExecutor taskExecutor) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setStep(workerStep);
        handler.setTaskExecutor(taskExecutor);
        handler.setGridSize(getGridSize());
        return handler;
    }

    /**
     * 파티션 병렬 실행용 스레드 풀을 설정한다.
     *
     * <p>소비자 {@code @Configuration}에서 {@code @Bean} 메서드로 감싸 반환해야
     * Spring이 {@code InitializingBean.afterPropertiesSet()}과 {@code DisposableBean.destroy()}를
     * 통해 초기화·graceful shutdown을 관리한다. {@code initialize()}는 직접 호출하지 않는다.</p>
     */
    protected ThreadPoolTaskExecutor buildTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(getGridSize());
        executor.setMaxPoolSize(getGridSize());
        executor.setThreadNamePrefix(getJobName() + "-partition-");
        return executor;
    }

    /**
     * chunk 기반 워커 Step을 생성한다. {@code skip(Exception.class)}를 기본 적용한다.
     *
     * @param workerStepName 워커 Step 이름 (매니저 Step의 partitioner 키와 동일해야 함)
     * @param reader         {@code @StepScope}로 주입된 ItemReader
     * @param writer         ItemWriter
     */
    protected Step buildWorkerStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   String workerStepName,
                                   ItemReader<T> reader,
                                   ItemWriter<T> writer) {
        return new StepBuilder(workerStepName, jobRepository)
                .<T, T>chunk(getPageSize(), transactionManager)
                .reader(reader)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(getSkipLimit())
                .allowStartIfComplete(false)
                .build();
    }
}

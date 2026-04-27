package com.example.spiderbatch.job.file2db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * 파일 아카이브 Tasklet.
 *
 * <p>고정 길이 파일 처리 Step 완료 후 원본 파일을 아카이브 또는 오류 디렉터리로 이동한다.
 * Job 실행 상태(COMPLETED / 그 외)에 따라 이동 대상 디렉터리가 결정된다.</p>
 *
 * <ul>
 *   <li>성공: {@code {archive-dir}/YYYYMMDD/} 하위로 이동</li>
 *   <li>실패: {@code {error-dir}/YYYYMMDD/} 하위로 이동 + 동명의 {@code .reason} 파일 생성 (오류 사유 최대 200자)</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class FileArchiveTasklet implements Tasklet {

    /** 처리 완료 원본 파일 경로 */
    private final String inputFilePath;

    /** 정상 처리 파일 보관 루트 디렉터리 */
    private final String archiveDir;

    /** 오류 파일 보관 루트 디렉터리 */
    private final String errorDir;

    /**
     * Job 흐름(flow)에서 라우팅된 경로를 기반으로 성공 여부를 주입받는다.
     * jobExecution.getStatus()는 Step 실행 중 STARTED로 고정되어 판단 불가하므로,
     * 성공/오류 Step이 분기된 시점에서 이미 결정된 값을 생성자로 전달한다.
     */
    private final boolean isSuccess;

    /** .reason 파일에 기록할 오류 사유 최대 길이 */
    private static final int REASON_MAX_LENGTH = 200;

    /** 날짜 기반 하위 디렉터리 포맷 */
    private static final DateTimeFormatter DIR_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws IOException {
        String dateDir = LocalDate.now().format(DIR_DATE_FORMAT);
        Path sourceFile = Paths.get(inputFilePath);

        if (!Files.exists(sourceFile)) {
            log.warn("[FileArchive] 원본 파일이 존재하지 않음 — skip. path={}", inputFilePath);
            return RepeatStatus.FINISHED;
        }

        if (isSuccess) {
            archiveSuccess(sourceFile, dateDir);
        } else {
            archiveError(sourceFile, dateDir, chunkContext);
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * 성공 아카이브: {@code {archive-dir}/YYYYMMDD/} 하위로 원본 파일 이동.
     */
    private void archiveSuccess(Path sourceFile, String dateDir) throws IOException {
        Path targetDir = Paths.get(archiveDir, dateDir);
        Files.createDirectories(targetDir); // 디렉터리 없으면 생성

        Path targetFile = targetDir.resolve(sourceFile.getFileName());
        Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        log.info("[FileArchive] 성공 아카이브 완료: {} → {}", sourceFile, targetFile);
    }

    /**
     * 실패 아카이브: {@code {error-dir}/YYYYMMDD/} 하위로 원본 파일 이동 후
     * 동명의 {@code .reason} 파일에 오류 사유(최대 200자)를 기록.
     */
    private void archiveError(Path sourceFile, String dateDir, ChunkContext chunkContext) throws IOException {
        Path targetDir = Paths.get(errorDir, dateDir);
        Files.createDirectories(targetDir); // 디렉터리 없으면 생성

        Path targetFile = targetDir.resolve(sourceFile.getFileName());
        Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        log.warn("[FileArchive] 오류 아카이브: {} → {}", sourceFile, targetFile);

        // 오류 사유 수집: Job 내 모든 Step의 실패 예외 메시지를 연결
        String reason = buildFailureReason(chunkContext);

        // .reason 파일: 원본 파일명 + ".reason" 확장자로 동일 오류 디렉터리에 생성
        Path reasonFile = targetDir.resolve(sourceFile.getFileName() + ".reason");
        Files.writeString(reasonFile, reason, StandardCharsets.UTF_8);
        log.warn("[FileArchive] .reason 파일 생성: {}", reasonFile);
    }

    /**
     * Job 실행 컨텍스트에서 실패 원인 문자열을 수집한다.
     * 각 Step의 {@link org.springframework.batch.core.StepExecution#getFailureExceptions()}에서
     * 첫 번째 메시지를 모아 최대 {@value #REASON_MAX_LENGTH}자로 잘라 반환한다.
     */
    private String buildFailureReason(ChunkContext chunkContext) {
        StringJoiner joiner = new StringJoiner("; ");

        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getStepExecutions()
                .forEach(se -> se.getFailureExceptions().stream()
                        .findFirst()
                        .ifPresent(ex -> joiner.add(se.getStepName() + ": " + ex.getMessage())));

        String reason = joiner.toString();

        // 최대 200자 초과 시 잘라냄
        if (reason.length() > REASON_MAX_LENGTH) {
            reason = reason.substring(0, REASON_MAX_LENGTH);
        }

        return reason.isEmpty() ? "Unknown error" : reason;
    }
}

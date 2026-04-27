package com.example.spiderbatch.job.common;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

@DisplayName("BatchJobParametersValidator 테스트")
class BatchJobParametersValidatorTest {

    // -----------------------------------------------------------------------
    // 기본 생성자 (파일 경로 검증 없음)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("필수 파라미터 정상 입력 → 예외 없음")
    void validate_validParams_noException() {
        BatchJobParametersValidator validator = new BatchJobParametersValidator();
        JobParameters params = new JobParametersBuilder()
                .addString("batchAppId", "TEST_JOB")
                .addString("batchDate", "20260424")
                .toJobParameters();

        assertThatCode(() -> validator.validate(params)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("batchAppId 누락 → JobParametersInvalidException")
    void validate_missingBatchAppId_throwsException() {
        BatchJobParametersValidator validator = new BatchJobParametersValidator();
        JobParameters params = new JobParametersBuilder()
                .addString("batchDate", "20260424")
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("batchAppId");
    }

    @Test
    @DisplayName("batchAppId 빈 문자열 → JobParametersInvalidException")
    void validate_blankBatchAppId_throwsException() {
        BatchJobParametersValidator validator = new BatchJobParametersValidator();
        JobParameters params = new JobParametersBuilder()
                .addString("batchAppId", "   ")
                .addString("batchDate", "20260424")
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(JobParametersInvalidException.class);
    }

    @Test
    @DisplayName("batchDate 누락 → JobParametersInvalidException")
    void validate_missingBatchDate_throwsException() {
        BatchJobParametersValidator validator = new BatchJobParametersValidator();
        JobParameters params = new JobParametersBuilder()
                .addString("batchAppId", "TEST_JOB")
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("batchDate");
    }

    @Test
    @DisplayName("batchDate 형식 오류(하이픈 포함) → JobParametersInvalidException")
    void validate_batchDateWithHyphen_throwsException() {
        BatchJobParametersValidator validator = new BatchJobParametersValidator();
        JobParameters params = new JobParametersBuilder()
                .addString("batchAppId", "TEST_JOB")
                .addString("batchDate", "2026-04-24")
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("batchDate");
    }

    @Test
    @DisplayName("batchDate 6자리(YYYYMM) → JobParametersInvalidException")
    void validate_batchDateTooShort_throwsException() {
        BatchJobParametersValidator validator = new BatchJobParametersValidator();
        JobParameters params = new JobParametersBuilder()
                .addString("batchAppId", "TEST_JOB")
                .addString("batchDate", "202604")
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(JobParametersInvalidException.class);
    }

    @Test
    @DisplayName("batchDate 숫자 아닌 문자 포함 → JobParametersInvalidException")
    void validate_batchDateWithLetters_throwsException() {
        BatchJobParametersValidator validator = new BatchJobParametersValidator();
        JobParameters params = new JobParametersBuilder()
                .addString("batchAppId", "TEST_JOB")
                .addString("batchDate", "2026042A")
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(JobParametersInvalidException.class);
    }

    // -----------------------------------------------------------------------
    // requireInputFilePath = true (파일 처리 Job)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("inputFilePath 검증 활성화 + 실제 파일 존재 → 예외 없음")
    void validate_existingFile_noException(@TempDir java.nio.file.Path tempDir) throws Exception {
        File tempFile = tempDir.resolve("input.txt").toFile();
        tempFile.createNewFile();

        BatchJobParametersValidator validator = new BatchJobParametersValidator(true);
        JobParameters params = new JobParametersBuilder()
                .addString("batchAppId", "FIXED_FILE2DB_JOB")
                .addString("batchDate", "20260424")
                .addString("inputFilePath", tempFile.getAbsolutePath())
                .toJobParameters();

        assertThatCode(() -> validator.validate(params)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("inputFilePath 검증 활성화 + inputFilePath 파라미터 누락 → JobParametersInvalidException")
    void validate_missingInputFilePath_throwsException() {
        BatchJobParametersValidator validator = new BatchJobParametersValidator(true);
        JobParameters params = new JobParametersBuilder()
                .addString("batchAppId", "FIXED_FILE2DB_JOB")
                .addString("batchDate", "20260424")
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("inputFilePath");
    }

    @Test
    @DisplayName("inputFilePath 검증 활성화 + 존재하지 않는 파일 경로 → JobParametersInvalidException")
    void validate_nonExistentFile_throwsException() {
        BatchJobParametersValidator validator = new BatchJobParametersValidator(true);
        JobParameters params = new JobParametersBuilder()
                .addString("batchAppId", "FIXED_FILE2DB_JOB")
                .addString("batchDate", "20260424")
                .addString("inputFilePath", "/non/existent/file.txt")
                .toJobParameters();

        assertThatThrownBy(() -> validator.validate(params))
                .isInstanceOf(JobParametersInvalidException.class)
                .hasMessageContaining("inputFilePath");
    }

    @Test
    @DisplayName("inputFilePath 검증 비활성화(기본값) + inputFilePath 없어도 통과")
    void validate_filePathValidationDisabled_ignoresInputFilePath() {
        // 기본 생성자 = requireInputFilePath(false) — CSV/DB Job 등에 사용
        BatchJobParametersValidator validator = new BatchJobParametersValidator();
        JobParameters params = new JobParametersBuilder()
                .addString("batchAppId", "DB2DB_JOB")
                .addString("batchDate", "20260424")
                // inputFilePath 없음
                .toJobParameters();

        assertThatCode(() -> validator.validate(params)).doesNotThrowAnyException();
    }
}

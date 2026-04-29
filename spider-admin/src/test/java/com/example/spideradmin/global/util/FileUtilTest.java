package com.example.spideradmin.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("FileUtil 테스트")
class FileUtilTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        new FileUtil().setUploadBasePath(tempDir.toString());
    }

    // ===== uploadFile =====

    @Nested
    @DisplayName("uploadFile")
    class UploadFile {

        @Test
        @DisplayName("정상 파일을 업로드하면 저장 경로를 반환한다")
        void upload_success() {
            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

            String result = FileUtil.uploadFile(file, "BOARD1", 1L);

            assertThat(result).startsWith("BOARD1/1/").endsWith("_test.txt");
        }

        @Test
        @DisplayName("null 파일은 null을 반환한다")
        void upload_nullFile_returnsNull() {
            assertThat(FileUtil.uploadFile(null, "BOARD1", 1L)).isNull();
        }

        @Test
        @DisplayName("빈 파일은 null을 반환한다")
        void upload_emptyFile_returnsNull() {
            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);

            assertThat(FileUtil.uploadFile(file, "BOARD1", 1L)).isNull();
        }

        @Test
        @DisplayName("파일명에 '..'이 포함되면 InvalidInputException을 던진다")
        void upload_pathTraversal_throwsException() {
            MockMultipartFile file = new MockMultipartFile("file", "../evil.txt", "text/plain", "hack".getBytes());

            assertThatThrownBy(() -> FileUtil.uploadFile(file, "BOARD1", 1L)).isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("boardId로 path traversal 시도하면 InvalidInputException을 던진다")
        void upload_boardIdPathTraversal_throwsException() {
            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hack".getBytes());

            assertThatThrownBy(() -> FileUtil.uploadFile(file, "../../etc", 1L))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("boardId에 슬래시가 포함되면 InvalidInputException을 던진다")
        void upload_boardIdWithSlash_throwsException() {
            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hack".getBytes());

            assertThatThrownBy(() -> FileUtil.uploadFile(file, "BOARD/evil", 1L))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("boardId에 특수문자가 포함되면 InvalidInputException을 던진다")
        void upload_boardIdWithSpecialChars_throwsException() {
            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hack".getBytes());

            assertThatThrownBy(() -> FileUtil.uploadFile(file, "board;rm", 1L))
                    .isInstanceOf(InvalidInputException.class);
            assertThatThrownBy(() -> FileUtil.uploadFile(file, "BOARD ID", 1L))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("boardId가 null이면 InvalidInputException을 던진다")
        void upload_boardIdNull_throwsException() {
            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hack".getBytes());

            assertThatThrownBy(() -> FileUtil.uploadFile(file, null, 1L)).isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("파일명에 슬래시가 포함되면 InvalidInputException을 던진다")
        void upload_fileNameWithSlash_throwsException() {
            MockMultipartFile file = new MockMultipartFile("file", "path/evil.txt", "text/plain", "hack".getBytes());

            assertThatThrownBy(() -> FileUtil.uploadFile(file, "BOARD1", 1L)).isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("파일명에 백슬래시가 포함되면 InvalidInputException을 던진다")
        void upload_fileNameWithBackslash_throwsException() {
            MockMultipartFile file = new MockMultipartFile("file", "path\\evil.txt", "text/plain", "hack".getBytes());

            assertThatThrownBy(() -> FileUtil.uploadFile(file, "BOARD1", 1L)).isInstanceOf(InvalidInputException.class);
        }
    }

    // ===== downloadFile =====

    @Nested
    @DisplayName("downloadFile")
    class DownloadFile {

        @Test
        @DisplayName("존재하는 파일을 다운로드하면 Resource를 반환한다")
        void download_success() throws IOException {
            Path subDir = tempDir.resolve("board/1");
            Files.createDirectories(subDir);
            Files.write(subDir.resolve("file.txt"), "content".getBytes());

            Resource resource = FileUtil.downloadFile("board/1/file.txt");

            assertThat(resource.exists()).isTrue();
        }

        @Test
        @DisplayName("null 경로면 InvalidInputException을 던진다")
        void download_nullPath_throwsException() {
            assertThatThrownBy(() -> FileUtil.downloadFile(null)).isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("빈 경로면 InvalidInputException을 던진다")
        void download_blankPath_throwsException() {
            assertThatThrownBy(() -> FileUtil.downloadFile("  ")).isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("path traversal 시도하면 InvalidInputException을 던진다")
        void download_pathTraversal_throwsException() {
            assertThatThrownBy(() -> FileUtil.downloadFile("../../etc/passwd"))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("존재하지 않는 파일이면 NotFoundException을 던진다")
        void download_notFound_throwsException() {
            assertThatThrownBy(() -> FileUtil.downloadFile("nonexistent.txt")).isInstanceOf(NotFoundException.class);
        }
    }

    // ===== deleteFile =====

    @Nested
    @DisplayName("deleteFile")
    class DeleteFile {

        @Test
        @DisplayName("존재하는 파일을 삭제한다")
        void delete_success() throws IOException {
            Files.write(tempDir.resolve("to-delete.txt"), "data".getBytes());

            FileUtil.deleteFile("to-delete.txt");

            assertThat(Files.exists(tempDir.resolve("to-delete.txt"))).isFalse();
        }

        @Test
        @DisplayName("null 경로는 아무 동작 없이 반환한다")
        void delete_nullPath_noOp() {
            assertThatCode(() -> FileUtil.deleteFile(null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("path traversal 시도하면 InvalidInputException을 던진다")
        void delete_pathTraversal_throwsException() {
            assertThatThrownBy(() -> FileUtil.deleteFile("../../etc/passwd")).isInstanceOf(InvalidInputException.class);
        }
    }

    // ===== exists =====

    @Nested
    @DisplayName("exists")
    class Exists {

        @Test
        @DisplayName("존재하는 파일이면 true를 반환한다")
        void exists_true() throws IOException {
            Files.write(tempDir.resolve("existing.txt"), "data".getBytes());

            assertThat(FileUtil.exists("existing.txt")).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 파일이면 false를 반환한다")
        void exists_false() {
            assertThat(FileUtil.exists("nope.txt")).isFalse();
        }

        @Test
        @DisplayName("null 경로면 false를 반환한다")
        void exists_null_returnsFalse() {
            assertThat(FileUtil.exists(null)).isFalse();
        }

        @Test
        @DisplayName("path traversal 시도하면 false를 반환한다")
        void exists_pathTraversal_returnsFalse() {
            assertThat(FileUtil.exists("../../etc/passwd")).isFalse();
        }
    }

    // ===== extractOriginalFileName =====

    @Nested
    @DisplayName("extractOriginalFileName")
    class ExtractOriginalFileName {

        @Test
        @DisplayName("UUID_원본파일명 형식에서 원본 파일명을 추출한다")
        void extract_success() {
            String result = FileUtil.extractOriginalFileName("board/1/uuid_original.txt");

            assertThat(result).isEqualTo("original.txt");
        }

        @Test
        @DisplayName("null 경로면 null을 반환한다")
        void extract_null_returnsNull() {
            assertThat(FileUtil.extractOriginalFileName(null)).isNull();
        }
    }
}

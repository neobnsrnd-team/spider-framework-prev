package com.example.spider_admin.domain.transdata.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.spider_admin.domain.transdata.dto.TransDataFileSearchRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("TransDataFileService 테스트")
class TransDataFileServiceTest {

    @Test
    @DisplayName("[엑셀] basePath가 존재하지 않으면 빈 엑셀을 생성한다")
    void exportFiles_nonExistentPath_returnsEmptyExcel() {
        TransDataFileService service = new TransDataFileService(null);
        ReflectionTestUtils.setField(service, "basePath", "/non/existent/path/xyz");

        TransDataFileSearchRequest search = new TransDataFileSearchRequest();
        byte[] result = service.exportFiles(search, null, null);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 파일이 있는 디렉토리에서 엑셀을 생성한다")
    void exportFiles_withFiles_returnsExcelBytes(@TempDir Path tempDir) throws IOException {
        // 파일 유형 디렉토리 생성 + 테스트 파일
        Path typeDir = tempDir.resolve("SQL");
        Files.createDirectories(typeDir);
        Files.writeString(typeDir.resolve("test.sql"), "SELECT 1 FROM DUAL");

        TransDataFileService service = new TransDataFileService(null);
        ReflectionTestUtils.setField(service, "basePath", tempDir.toString());

        TransDataFileSearchRequest search = new TransDataFileSearchRequest();
        byte[] result = service.exportFiles(search, "fileName", "ASC");

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 검색 조건으로 필터링된 결과로 엑셀을 생성한다")
    void exportFiles_withFilter_returnsFilteredExcel(@TempDir Path tempDir) throws IOException {
        Path typeDir = tempDir.resolve("SQL");
        Files.createDirectories(typeDir);
        Files.writeString(typeDir.resolve("match.sql"), "SELECT 1");
        Files.writeString(typeDir.resolve("other.sql"), "SELECT 2");

        TransDataFileService service = new TransDataFileService(null);
        ReflectionTestUtils.setField(service, "basePath", tempDir.toString());

        TransDataFileSearchRequest search = new TransDataFileSearchRequest();
        search.setFileName("match");
        byte[] result = service.exportFiles(search, null, null);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }
}

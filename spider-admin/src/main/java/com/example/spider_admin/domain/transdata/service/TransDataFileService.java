package com.example.spider_admin.domain.transdata.service;

import com.example.spider_admin.domain.transdata.dto.TransDataFilePreviewResponse;
import com.example.spider_admin.domain.transdata.dto.TransDataFileResponse;
import com.example.spider_admin.domain.transdata.dto.TransDataFileSearchRequest;
import com.example.spider_admin.domain.transdata.dto.TransDataFileUploadResponse;
import com.example.spider_admin.domain.transdata.dto.TransDataSqlExecuteResponse;
import com.example.spider_admin.domain.transdata.enums.TranType;
import com.example.spider_admin.domain.transdata.util.TransDataSqlParser;
import com.example.spider_admin.domain.transdata.util.TransDataSqlParser.ParsedSection;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.util.ExcelColumnDefinition;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이행 데이터 파일 서비스 구현체
 * - 서버 로컬 파일 시스템에서 파일 목록 조회
 * - DB Mapper 사용하지 않음
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransDataFileService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${trans.data.file.base-path:/temp/trans-data}")
    private String basePath;

    private static final long MAX_PREVIEW_SIZE = 1024L * 1024; // 1MB
    private static final int MAX_FILE_COUNT = 10_000; // 파일 수집 상한
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String COL_FILE_TYPE = "fileType";
    private static final String COL_FILE_NAME = "fileName";
    private static final String COL_FILE_SIZE = "fileSize";
    private static final String COL_CREATE_DATE = "createDate";

    private static class SqlExecutionCounters {
        int success = 0;
        int fail = 0;

        int total() {
            return success + fail;
        }
    }

    public PageResponse<TransDataFileResponse> searchFiles(
            PageRequest pageRequest, TransDataFileSearchRequest searchDTO) {

        Path baseDir = Paths.get(basePath);
        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            log.warn("Base path does not exist or is not a directory: {}", basePath);
            return buildEmptyPage(pageRequest);
        }

        // 1. 전체 파일 목록 수집
        List<TransDataFileResponse> allFiles = collectFiles(baseDir);

        // 2. 검색 조건 필터링
        List<TransDataFileResponse> filtered = applyFilters(allFiles, searchDTO);

        // 3. 정렬
        filtered = applySort(filtered, pageRequest.getSortBy(), pageRequest.getSortDirection());

        // 4. 수동 페이지네이션
        return buildPageResponse(filtered, pageRequest);
    }

    public byte[] exportFiles(TransDataFileSearchRequest searchDTO, String sortBy, String sortDirection) {
        Path baseDir = Paths.get(basePath);
        List<TransDataFileResponse> allFiles =
                (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) ? List.of() : collectFiles(baseDir);

        List<TransDataFileResponse> filtered = applyFilters(allFiles, searchDTO);
        filtered = applySort(filtered, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(filtered.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + filtered.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("이행대상 항목", 15, COL_FILE_TYPE),
                new ExcelColumnDefinition("이행대상 파일명", 40, COL_FILE_NAME),
                new ExcelColumnDefinition("파일 사이즈", 15, COL_FILE_SIZE),
                new ExcelColumnDefinition("이행파일 생성일", 20, COL_CREATE_DATE));

        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> rows = new ArrayList<>(filtered.size());
        for (var item : filtered) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(COL_FILE_TYPE, item.getFileType());
            row.put(COL_FILE_NAME, item.getFileName());
            row.put(COL_FILE_SIZE, item.getFileSize());
            row.put(
                    COL_CREATE_DATE,
                    item.getCreateDate() != null ? item.getCreateDate().format(dtFmt) : "");
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("이행데이터조회", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    public TransDataFilePreviewResponse previewFile(String filePath) {
        Path path = resolveAndValidateFilePath(filePath);

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new InvalidInputException("파일을 찾을 수 없습니다: " + path.getFileName());
        }

        try {
            long fileSize = Files.size(path);
            boolean truncated = fileSize > MAX_PREVIEW_SIZE;

            String content;
            if (truncated) {
                try (var is = Files.newInputStream(path)) {
                    byte[] bytes = is.readNBytes(Math.toIntExact(MAX_PREVIEW_SIZE));
                    content = new String(bytes, StandardCharsets.UTF_8);
                }
            } else {
                content = Files.readString(path, StandardCharsets.UTF_8);
            }

            return TransDataFilePreviewResponse.builder()
                    .fileName(path.getFileName().toString())
                    .content(content)
                    .fileSize(fileSize)
                    .truncated(truncated)
                    .build();
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
            throw new InternalException("파일 읽기에 실패했습니다.", e);
        }
    }

    public List<Map<String, String>> getFileTypes() {
        return Arrays.stream(TranType.values())
                .map(t -> Map.of("code", t.getCode(), "description", t.getDescription()))
                .toList();
    }

    // ======================== Private Methods ========================

    private List<TransDataFileResponse> collectFiles(Path baseDir) {
        List<TransDataFileResponse> files = new ArrayList<>();

        try (DirectoryStream<Path> typeDirs = Files.newDirectoryStream(baseDir)) {
            for (Path typeDir : typeDirs) {
                if (files.size() >= MAX_FILE_COUNT) {
                    log.warn("파일 수집 상한({}) 도달. 일부 파일이 누락될 수 있습니다.", MAX_FILE_COUNT);
                    break;
                }
                if (!Files.isDirectory(typeDir)) {
                    continue;
                }
                collectFilesFromTypeDir(baseDir, typeDir, files);
            }
        } catch (IOException e) {
            log.error("Failed to list type directories from: {}", baseDir, e);
        }

        return files;
    }

    private void collectFilesFromTypeDir(Path baseDir, Path typeDir, List<TransDataFileResponse> files) {
        String fileType = typeDir.getFileName().toString();

        try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(typeDir)) {
            for (Path file : fileStream) {
                if (files.size() >= MAX_FILE_COUNT) break;
                if (!Files.isRegularFile(file)) continue;
                readFileResponse(baseDir, file, fileType).ifPresent(files::add);
            }
        } catch (IOException e) {
            log.warn("Failed to list files in directory: {}", typeDir, e);
        }
    }

    private Optional<TransDataFileResponse> readFileResponse(Path baseDir, Path file, String fileType) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            LocalDateTime createTime =
                    LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());

            String fileName = file.getFileName().toString();
            return Optional.of(TransDataFileResponse.builder()
                    .fileType(resolveFileType(fileType, fileName))
                    .fileName(fileName)
                    .fileSize(attrs.size())
                    .createDate(createTime)
                    .filePath(baseDir.relativize(file).toString().replace('\\', '/'))
                    .build());
        } catch (IOException e) {
            log.warn("Failed to read file attributes: {}", file, e);
            return Optional.empty();
        }
    }

    private List<TransDataFileResponse> applyFilters(
            List<TransDataFileResponse> files, TransDataFileSearchRequest searchDTO) {

        Stream<TransDataFileResponse> stream = files.stream();

        // 파일명 필터 (부분 일치, 대소문자 무시)
        if (searchDTO.getFileName() != null && !searchDTO.getFileName().isBlank()) {
            String keyword = searchDTO.getFileName().toLowerCase();
            stream = stream.filter(f -> f.getFileName().toLowerCase().contains(keyword));
        }

        // 파일 유형 필터
        if (searchDTO.getFileType() != null && !searchDTO.getFileType().isBlank()) {
            stream = stream.filter(f -> searchDTO.getFileType().equals(f.getFileType()));
        }

        // 날짜 범위 필터 (검색: yyyyMMdd, DTO createDate: LocalDateTime)
        if (searchDTO.getDateFrom() != null && !searchDTO.getDateFrom().isBlank()) {
            LocalDate from = LocalDate.parse(searchDTO.getDateFrom(), DATE_FMT);
            stream = stream.filter(f -> !f.getCreateDate().toLocalDate().isBefore(from));
        }
        if (searchDTO.getDateTo() != null && !searchDTO.getDateTo().isBlank()) {
            LocalDate to = LocalDate.parse(searchDTO.getDateTo(), DATE_FMT);
            stream = stream.filter(f -> !f.getCreateDate().toLocalDate().isAfter(to));
        }

        return stream.toList();
    }

    private List<TransDataFileResponse> applySort(
            List<TransDataFileResponse> files, String sortBy, String sortDirection) {

        if (sortBy == null || sortBy.isBlank()) {
            sortBy = COL_CREATE_DATE;
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "DESC";
        }

        Comparator<TransDataFileResponse> comparator;
        switch (sortBy) {
            case COL_FILE_TYPE:
                comparator = Comparator.comparing(
                        TransDataFileResponse::getFileType, Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case COL_FILE_NAME:
                comparator = Comparator.comparing(
                        TransDataFileResponse::getFileName, Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case COL_FILE_SIZE:
                comparator =
                        Comparator.comparing(TransDataFileResponse::getFileSize, Comparator.nullsLast(Long::compareTo));
                break;
            case COL_CREATE_DATE:
            default:
                comparator = Comparator.comparing(
                        TransDataFileResponse::getCreateDate, Comparator.nullsLast(LocalDateTime::compareTo));
                break;
        }

        if ("DESC".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        return files.stream().sorted(comparator).toList();
    }

    private PageResponse<TransDataFileResponse> buildPageResponse(
            List<TransDataFileResponse> files, PageRequest pageRequest) {

        long totalElements = files.size();
        int size = pageRequest.getSize();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int page = pageRequest.getPage(); // 0-based
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, (int) totalElements);

        List<TransDataFileResponse> pageContent =
                (fromIndex >= totalElements) ? List.of() : files.subList(fromIndex, toIndex);

        return PageResponse.<TransDataFileResponse>builder()
                .content(pageContent)
                .currentPage(page + 1) // 1-based for frontend
                .totalPages(totalPages)
                .totalElements(totalElements)
                .size(size)
                .hasNext(page + 1 < totalPages)
                .hasPrevious(page > 0)
                .build();
    }

    private PageResponse<TransDataFileResponse> buildEmptyPage(PageRequest pageRequest) {
        return PageResponse.<TransDataFileResponse>builder()
                .content(List.of())
                .currentPage(1)
                .totalPages(0)
                .totalElements(0L)
                .size(pageRequest.getSize())
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    public List<TransDataFileUploadResponse> uploadSqlFiles(List<MultipartFile> files) {
        List<TransDataFileUploadResponse> results = new ArrayList<>();

        Path sqlDir = Paths.get(basePath, "SQL");
        try {
            Files.createDirectories(sqlDir);
        } catch (IOException e) {
            log.error("SQL 디렉토리 생성 실패: {}", sqlDir, e);
            files.forEach(f -> results.add(TransDataFileUploadResponse.builder()
                    .fileName(f.getOriginalFilename())
                    .size(f.getSize())
                    .status("FAIL")
                    .message("업로드 디렉토리 생성 실패")
                    .build()));
            return results;
        }

        for (MultipartFile file : files) {
            results.add(processSingleUpload(file, sqlDir));
        }

        return results;
    }

    public TransDataSqlExecuteResponse executeSqlFile(String filePath) {
        Path path = resolveAndValidateFilePath(filePath);

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new InvalidInputException("파일을 찾을 수 없습니다: " + path.getFileName());
        }

        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("SQL 파일 읽기 실패: {}", filePath, e);
            throw new InternalException("SQL 파일 읽기에 실패했습니다.", e);
        }

        String tranSeq = TransDataSqlParser.extractTranSeq(content);
        List<ParsedSection> sections = TransDataSqlParser.parseSections(content);

        SqlExecutionCounters counters = new SqlExecutionCounters();
        List<String> failSqlList = new ArrayList<>();
        List<String> failReasonList = new ArrayList<>();

        for (ParsedSection section : sections) {
            TransDataSqlExecuteResponse earlyResult =
                    executeSectionStatements(section, path, tranSeq, counters, failSqlList, failReasonList);
            if (earlyResult != null) {
                return earlyResult;
            }
        }

        updateFailResult(failSqlList, failReasonList, tranSeq);

        log.info(
                "SQL 파일 실행 완료 - file: {}, total: {}, success: {}, fail: {}",
                path.getFileName(),
                counters.total(),
                counters.success,
                counters.fail);

        return TransDataSqlExecuteResponse.builder()
                .totalCount(counters.total())
                .successCount(counters.success)
                .failCount(counters.fail)
                .errors(failReasonList)
                .build();
    }

    /**
     * 단일 파일 업로드를 처리합니다.
     * 유효성 검증, 경로 보안 검사, 파일 저장을 수행하고 결과를 반환합니다.
     */
    private TransDataFileUploadResponse processSingleUpload(MultipartFile file, Path sqlDir) {
        String originalFileName =
                StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");

        TransDataFileUploadResponse validationError = buildValidationError(file, originalFileName);
        if (validationError != null) {
            return validationError;
        }

        try {
            Path dest = sqlDir.resolve(originalFileName).normalize().toAbsolutePath();
            if (!dest.startsWith(sqlDir.toAbsolutePath())) {
                return TransDataFileUploadResponse.builder()
                        .fileName(originalFileName)
                        .size(file.getSize())
                        .status("FAIL")
                        .message("허용되지 않은 파일 경로입니다.")
                        .build();
            }

            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
            log.info("SQL 파일 업로드 완료: {}", dest);

            return TransDataFileUploadResponse.builder()
                    .fileName(originalFileName)
                    .size(file.getSize())
                    .status("SUCCESS")
                    .message("업로드 성공")
                    .build();
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", originalFileName, e);
            return TransDataFileUploadResponse.builder()
                    .fileName(originalFileName)
                    .size(file.getSize())
                    .status("FAIL")
                    .message("파일 저장 중 오류 발생")
                    .build();
        }
    }

    /**
     * 단일 섹션의 SQL 문들을 실행합니다.
     * 메타데이터 섹션에서 실패 시 조기 종료 결과를 반환하고,
     * 그 외의 경우 null을 반환하여 다음 섹션 처리를 계속합니다.
     *
     * @param counters SQL 실행 성공/실패 카운터
     * @return 조기 종료가 필요한 경우 결과 객체, 아니면 null
     */
    private TransDataSqlExecuteResponse executeSectionStatements(
            ParsedSection section,
            Path path,
            String tranSeq,
            SqlExecutionCounters counters,
            List<String> failSqlList,
            List<String> failReasonList) {
        boolean isMetadata = section.name().contains("이행회차") || section.name().contains("이행이력");
        boolean isHisSection = section.name().contains("이행이력");

        for (String sql : section.statements()) {
            try {
                jdbcTemplate.execute(sql);
                counters.success++;
            } catch (Exception e) {
                counters.fail++;
                String msg = e.getMessage() != null ? e.getMessage() : "알 수 없는 오류";
                log.error("SQL 실행 실패 [{}] 섹션={}: {}", path.getFileName(), section.name(), msg);

                if (isMetadata) {
                    handleMetadataFailure(isHisSection, tranSeq);
                    log.info(
                            "SQL 파일 실행 중단 - file: {}, section: {}, success: {}, fail: {}",
                            path.getFileName(),
                            section.name(),
                            counters.success,
                            counters.fail);
                    return TransDataSqlExecuteResponse.builder()
                            .totalCount(counters.total())
                            .successCount(counters.success)
                            .failCount(counters.fail)
                            .errors(List.of(msg))
                            .build();
                }

                failSqlList.add(sql);
                failReasonList.add(msg);
            }
        }
        return null;
    }

    private void handleMetadataFailure(boolean isHisSection, String tranSeq) {
        if (isHisSection && tranSeq != null) {
            try {
                jdbcTemplate.update("UPDATE FWK_TRANS_DATA_TIMES SET TRAN_RESULT = 'F' WHERE TRAN_SEQ = ?", tranSeq);
            } catch (Exception ue) {
                log.error("이행회차 TRAN_RESULT 업데이트 실패. TRAN_SEQ={}", tranSeq, ue);
            }
        }
    }

    private void updateFailResult(List<String> failSqlList, List<String> failReasonList, String tranSeq) {
        if (failSqlList.isEmpty() || tranSeq == null) {
            return;
        }
        String failSql = TransDataSqlParser.truncate(String.join("\n---\n", failSqlList), 2000);
        String failReason = TransDataSqlParser.truncate(String.join("\n---\n", failReasonList), 2000);
        try {
            jdbcTemplate.update("UPDATE FWK_TRANS_DATA_TIMES SET TRAN_RESULT = 'F' WHERE TRAN_SEQ = ?", tranSeq);
            jdbcTemplate.update(
                    "UPDATE FWK_TRANS_DATA_HIS SET TRAN_RESULT = 'F', TRAN_FAIL_SQL = ?, TRAN_FAIL_REASON = ? WHERE TRAN_SEQ = ?",
                    failSql,
                    failReason,
                    tranSeq);
            log.info("데이터 이행 실패로 TRAN_RESULT='F' 업데이트 완료. TRAN_SEQ={}", tranSeq);
        } catch (Exception e) {
            log.error("TRAN_RESULT 업데이트 실패. TRAN_SEQ={}", tranSeq, e);
        }
    }

    /**
     * 파일명에서 TranType 코드를 추출합니다.
     * 파일명이 "CODE@TEST0219_guest01_202602190121" 형식이면 '@' 앞 부분을 TranType으로 해석합니다.
     * TranType에 해당하지 않으면 디렉토리명(fallback)을 반환합니다.
     */
    private String resolveFileType(String directoryName, String fileName) {
        int atIndex = fileName.indexOf('@');
        if (atIndex > 0) {
            String typeCode = fileName.substring(0, atIndex);
            TranType tranType = TranType.fromCode(typeCode);
            if (tranType != null) {
                return tranType.getCode();
            }
        }
        return directoryName;
    }

    private TransDataFileUploadResponse buildValidationError(MultipartFile file, String originalFileName) {
        if (file.isEmpty() || originalFileName.isBlank()) {
            return TransDataFileUploadResponse.builder()
                    .fileName(originalFileName)
                    .size(0L)
                    .status("FAIL")
                    .message("파일이 비어 있습니다.")
                    .build();
        }
        if (!originalFileName.toLowerCase().endsWith(".sql")) {
            return TransDataFileUploadResponse.builder()
                    .fileName(originalFileName)
                    .size(file.getSize())
                    .status("FAIL")
                    .message("SQL 파일(.sql)만 업로드 가능합니다.")
                    .build();
        }
        if (originalFileName.contains("..") || originalFileName.contains("/") || originalFileName.contains("\\")) {
            return TransDataFileUploadResponse.builder()
                    .fileName(originalFileName)
                    .size(file.getSize())
                    .status("FAIL")
                    .message("허용되지 않은 파일명입니다.")
                    .build();
        }
        return null;
    }

    private Path resolveAndValidateFilePath(String filePath) {
        Path normalizedBase = Paths.get(basePath).normalize().toAbsolutePath();
        Path resolvedPath = normalizedBase.resolve(filePath).normalize().toAbsolutePath();
        if (!resolvedPath.startsWith(normalizedBase)) {
            throw new InvalidInputException("허용되지 않은 파일 경로입니다.");
        }
        return resolvedPath;
    }
}

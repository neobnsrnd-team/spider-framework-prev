package com.example.spideradmin.domain.transdata.controller;

import com.example.spideradmin.domain.transdata.dto.TransDataFilePreviewResponse;
import com.example.spideradmin.domain.transdata.dto.TransDataFileResponse;
import com.example.spideradmin.domain.transdata.dto.TransDataFileSearchRequest;
import com.example.spideradmin.domain.transdata.dto.TransDataFileUploadResponse;
import com.example.spideradmin.domain.transdata.dto.TransDataSqlExecuteRequest;
import com.example.spideradmin.domain.transdata.dto.TransDataSqlExecuteResponse;
import com.example.spideradmin.domain.transdata.service.TransDataFileService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이행 데이터 파일 검색/조회 REST API
 * - 서버 로컬 파일 시스템에서 파일 목록 조회
 * - READ-ONLY 기능만 제공
 */
@RestController
@RequestMapping("/api/trans/trans-data-inqlist")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TRANS_DATA_LIST:R')")
public class TransDataFileController {

    private final TransDataFileService transDataFileService;

    /**
     * 파일 목록 페이지네이션 검색
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<TransDataFileResponse>>> searchFiles(
            @ModelAttribute PageRequest pageRequest, @ModelAttribute TransDataFileSearchRequest searchDTO) {

        // Frontend sends 1-based page, convert to 0-based for service
        pageRequest.setPage(pageRequest.getPage() - 1);

        return ResponseEntity.ok(ApiResponse.success(transDataFileService.searchFiles(pageRequest, searchDTO)));
    }

    /**
     * 파일 내용 미리보기
     */
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<TransDataFilePreviewResponse>> previewFile(@RequestParam String filePath) {
        return ResponseEntity.ok(ApiResponse.success(transDataFileService.previewFile(filePath)));
    }

    /**
     * 파일 유형 목록 (콤보 드롭다운용) - TranType enum 기반
     */
    @GetMapping("/file-types")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getFileTypes() {
        return ResponseEntity.ok(ApiResponse.success(transDataFileService.getFileTypes()));
    }

    /**
     * 이행 데이터 파일 목록 엑셀 내보내기
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportFiles(
            @ModelAttribute TransDataFileSearchRequest searchDTO,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] excelBytes = transDataFileService.exportFiles(searchDTO, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("TransDataFiles", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * SQL 파일 업로드 ({basePath}/SQL/ 디렉토리에 저장)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('TRANS_DATA_LIST:W')")
    public ResponseEntity<ApiResponse<List<TransDataFileUploadResponse>>> uploadSqlFiles(
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success(transDataFileService.uploadSqlFiles(files)));
    }

    /**
     * SQL 파일 내 구문 실행
     */
    @PostMapping("/execute-sql")
    @PreAuthorize("hasAuthority('TRANS_DATA_LIST:W')")
    public ResponseEntity<ApiResponse<TransDataSqlExecuteResponse>> executeSqlFile(
            @Valid @RequestBody TransDataSqlExecuteRequest dto) {
        return ResponseEntity.ok(ApiResponse.success(transDataFileService.executeSqlFile(dto.getFilePath())));
    }
}

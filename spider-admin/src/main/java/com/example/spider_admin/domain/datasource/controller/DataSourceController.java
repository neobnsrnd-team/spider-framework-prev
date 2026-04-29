package com.example.spider_admin.domain.datasource.controller;

import com.example.spider_admin.domain.datasource.dto.DataSourceCreateRequest;
import com.example.spider_admin.domain.datasource.dto.DataSourceResponse;
import com.example.spider_admin.domain.datasource.dto.DataSourceSearchRequest;
import com.example.spider_admin.domain.datasource.dto.DataSourceUpdateRequest;
import com.example.spider_admin.domain.datasource.service.DataSourceService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 데이터소스 관리 REST Controller
 *
 * <p>클래스 레벨: DATASOURCE:R (읽기 기본), 쓰기 작업은 메서드 레벨에서 DATASOURCE:W 요구
 */
@Slf4j
@RestController
@RequestMapping("/api/datasources")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DATASOURCE:R')")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    /**
     * 데이터소스 페이징 검색 조회
     * GET /api/datasources/page
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<DataSourceResponse>>> getPage(
            @ModelAttribute DataSourceSearchRequest searchDTO) {
        log.info(
                "GET /api/datasources/page - page:{}, size:{}, searchField:{}, searchValue:{}, jndiYnFilter:{}",
                searchDTO.getPage(),
                searchDTO.getSize(),
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getJndiYnFilter());
        return ResponseEntity.ok(ApiResponse.success(dataSourceService.getDataSourcesWithSearch(searchDTO)));
    }

    /**
     * 데이터소스 단건 조회
     * GET /api/datasources/{dbId}
     */
    @GetMapping("/{dbId}")
    public ResponseEntity<ApiResponse<DataSourceResponse>> getById(@PathVariable String dbId) {
        log.info("GET /api/datasources/{}", dbId);
        return ResponseEntity.ok(ApiResponse.success(dataSourceService.getById(dbId)));
    }

    /**
     * 데이터소스 엑셀 내보내기
     * GET /api/datasources/export
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String jndiYnFilter,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        byte[] excelBytes =
                dataSourceService.exportDataSources(searchField, searchValue, jndiYnFilter, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("DataSource", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * 데이터소스 등록
     * POST /api/datasources
     */
    @PostMapping
    @PreAuthorize("hasAuthority('DATASOURCE:W')")
    public ResponseEntity<ApiResponse<DataSourceResponse>> create(@Valid @RequestBody DataSourceCreateRequest dto) {
        log.info("POST /api/datasources - dbId:{}", dto.getDbId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dataSourceService.create(dto)));
    }

    /**
     * 데이터소스 수정
     * PUT /api/datasources/{dbId}
     */
    @PutMapping("/{dbId}")
    @PreAuthorize("hasAuthority('DATASOURCE:W')")
    public ResponseEntity<ApiResponse<DataSourceResponse>> update(
            @PathVariable String dbId, @Valid @RequestBody DataSourceUpdateRequest dto) {
        log.info("PUT /api/datasources/{}", dbId);
        return ResponseEntity.ok(ApiResponse.success(dataSourceService.update(dbId, dto)));
    }

    /**
     * 데이터소스 삭제
     * DELETE /api/datasources/{dbId}
     */
    @DeleteMapping("/{dbId}")
    @PreAuthorize("hasAuthority('DATASOURCE:W')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String dbId) {
        log.info("DELETE /api/datasources/{}", dbId);
        dataSourceService.delete(dbId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

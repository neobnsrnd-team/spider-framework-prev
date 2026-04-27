package com.example.admin_demo.domain.sqlquery.controller;

import com.example.admin_demo.domain.sqlquery.dto.SqlGroupResponse;
import com.example.admin_demo.domain.sqlquery.dto.SqlQueryCreateRequest;
import com.example.admin_demo.domain.sqlquery.dto.SqlQueryHistoryResponse;
import com.example.admin_demo.domain.sqlquery.dto.SqlQueryResponse;
import com.example.admin_demo.domain.sqlquery.dto.SqlQuerySearchRequest;
import com.example.admin_demo.domain.sqlquery.dto.SqlQueryTestRequest;
import com.example.admin_demo.domain.sqlquery.dto.SqlQueryTestResponse;
import com.example.admin_demo.domain.sqlquery.dto.SqlQueryUpdateRequest;
import com.example.admin_demo.domain.sqlquery.service.SqlQueryService;
import com.example.admin_demo.global.client.SpiderLinkReloadClient;
import com.example.admin_demo.global.dto.ApiResponse;
import com.example.admin_demo.global.dto.PageResponse;
import com.example.admin_demo.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/sql-queries")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SQL_QUERY:R')")
public class SqlQueryController {

    private final SqlQueryService sqlQueryService;
    private final SpiderLinkReloadClient spiderLinkReloadClient;

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<SqlQueryResponse>>> getSqlQueriesWithPagination(
            @ModelAttribute SqlQuerySearchRequest searchDTO) {
        log.info(
                "GET /api/sql-queries/page - page: {}, size: {}, queryId: {}, queryName: {}, useYn: {}",
                searchDTO.getPage(),
                searchDTO.getSize(),
                searchDTO.getQueryId(),
                searchDTO.getQueryName(),
                searchDTO.getUseYn());
        return ResponseEntity.ok(ApiResponse.success(sqlQueryService.getSqlQueriesWithSearch(searchDTO)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@ModelAttribute SqlQuerySearchRequest searchDTO) {
        log.info("GET /api/sql-queries/export");

        byte[] excelBytes = sqlQueryService.exportExcel(searchDTO);
        String fileName = ExcelExportUtil.generateFileName("SqlQuery", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping("/{queryId}")
    public ResponseEntity<ApiResponse<SqlQueryResponse>> getById(@PathVariable String queryId) {
        log.info("GET /api/sql-queries/{}", queryId);
        return ResponseEntity.ok(ApiResponse.success(sqlQueryService.getById(queryId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SQL_QUERY:W')")
    public ResponseEntity<ApiResponse<SqlQueryResponse>> create(@Valid @RequestBody SqlQueryCreateRequest dto) {
        log.info("POST /api/sql-queries - queryId: {}", dto.getQueryId());
        SqlQueryResponse result = sqlQueryService.create(dto);
        // 트랜잭션 커밋 후 spider-link에 실시간 반영
        spiderLinkReloadClient.reload(result.getQueryId(), result.getUseYn());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @PutMapping("/{queryId}")
    @PreAuthorize("hasAuthority('SQL_QUERY:W')")
    public ResponseEntity<ApiResponse<SqlQueryResponse>> update(
            @PathVariable String queryId, @Valid @RequestBody SqlQueryUpdateRequest dto) {
        log.info("PUT /api/sql-queries/{}", queryId);
        SqlQueryResponse result = sqlQueryService.update(queryId, dto);
        // USE_YN='N'이면 statement 제거, 그 외는 리로드
        spiderLinkReloadClient.reload(result.getQueryId(), result.getUseYn());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{queryId}")
    @PreAuthorize("hasAuthority('SQL_QUERY:W')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String queryId) {
        log.info("DELETE /api/sql-queries/{}", queryId);
        sqlQueryService.delete(queryId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{queryId}/test")
    @PreAuthorize("hasAuthority('SQL_QUERY:W')")
    public ResponseEntity<ApiResponse<SqlQueryTestResponse>> testQuery(
            @PathVariable String queryId, @RequestBody(required = false) SqlQueryTestRequest request) {
        log.info("POST /api/sql-queries/{}/test", queryId);
        return ResponseEntity.ok(ApiResponse.success(sqlQueryService.testQuery(queryId, request)));
    }

    @PostMapping("/{queryId}/backup")
    @PreAuthorize("hasAuthority('SQL_QUERY:W')")
    public ResponseEntity<ApiResponse<Void>> backupQuery(@PathVariable String queryId) {
        log.info("POST /api/sql-queries/{}/backup", queryId);
        sqlQueryService.backupQuery(queryId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{queryId}/use-yn")
    @PreAuthorize("hasAuthority('SQL_QUERY:W')")
    public ResponseEntity<ApiResponse<SqlQueryResponse>> toggleUseYn(@PathVariable String queryId) {
        log.info("PATCH /api/sql-queries/{}/use-yn", queryId);
        SqlQueryResponse result = sqlQueryService.toggleUseYn(queryId);
        // USE_YN='N'이면 statement 제거, 'Y'이면 리로드
        spiderLinkReloadClient.reload(result.getQueryId(), result.getUseYn());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{queryId}/history")
    public ResponseEntity<ApiResponse<List<SqlQueryHistoryResponse>>> getHistoryList(@PathVariable String queryId) {
        log.info("GET /api/sql-queries/{}/history", queryId);
        return ResponseEntity.ok(ApiResponse.success(sqlQueryService.getHistoryList(queryId)));
    }

    @GetMapping("/{queryId}/history/{versionId}")
    public ResponseEntity<ApiResponse<SqlQueryHistoryResponse>> getHistoryDetail(
            @PathVariable String queryId, @PathVariable String versionId) {
        log.info("GET /api/sql-queries/{}/history/{}", queryId, versionId);
        return ResponseEntity.ok(ApiResponse.success(sqlQueryService.getHistoryDetail(queryId, versionId)));
    }

    @PostMapping("/{queryId}/restore/{versionId}")
    @PreAuthorize("hasAuthority('SQL_QUERY:W')")
    public ResponseEntity<ApiResponse<SqlQueryResponse>> restoreFromHistory(
            @PathVariable String queryId, @PathVariable String versionId) {
        log.info("POST /api/sql-queries/{}/restore/{}", queryId, versionId);
        SqlQueryResponse result = sqlQueryService.restoreFromHistory(queryId, versionId);
        // 복원 후 변경된 SQL을 spider-link에 반영
        spiderLinkReloadClient.reload(result.getQueryId(), result.getUseYn());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/group-search")
    public ResponseEntity<ApiResponse<List<SqlGroupResponse>>> searchGroups(
            @RequestParam(required = false) String keyword) {
        log.info("GET /api/sql-queries/group-search - keyword: {}", keyword);
        return ResponseEntity.ok(ApiResponse.success(sqlQueryService.searchGroups(keyword)));
    }
}

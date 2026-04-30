package com.example.spideradmin.domain.org.controller;

import com.example.spideradmin.domain.org.dto.OrgBatchRequest;
import com.example.spideradmin.domain.org.dto.OrgResponse;
import com.example.spideradmin.domain.org.service.OrgService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ORG:R')")
public class OrgController {

    private final OrgService orgService;

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<OrgResponse>>> getAllOrgs() {
        List<OrgResponse> orgs = orgService.getAllOrgs();
        return ResponseEntity.ok(ApiResponse.success(orgs));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrgResponse>>> getOrgs(
            @RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "1000") Integer size) {

        PageRequest pageRequest =
                PageRequest.builder().page(page - 1).size(size).build();

        PageResponse<OrgResponse> response = orgService.searchOrgs(pageRequest, null, null);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<OrgResponse>>> getOrgsPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<OrgResponse> response = orgService.searchOrgs(pageRequest, searchField, searchValue);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrgs(
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        byte[] excelBytes = orgService.exportOrgs(searchField, searchValue, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("Org", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('ORG:W')")
    public ResponseEntity<ApiResponse<Void>> saveOrgBatch(@Valid @RequestBody OrgBatchRequest request) {
        orgService.saveBatch(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

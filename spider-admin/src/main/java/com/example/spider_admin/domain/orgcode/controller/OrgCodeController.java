package com.example.spider_admin.domain.orgcode.controller;

import com.example.spider_admin.domain.orgcode.dto.OrgCodePopupResponse;
import com.example.spider_admin.domain.orgcode.dto.OrgCodeResponse;
import com.example.spider_admin.domain.orgcode.dto.OrgCodeSaveRequest;
import com.example.spider_admin.domain.orgcode.dto.OrgCodeSearchRequest;
import com.example.spider_admin.domain.orgcode.service.OrgCodeService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/org-codes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ORG_CODE:R')")
public class OrgCodeController {

    private final OrgCodeService orgCodeService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrgCodes(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) String codeGroupId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        byte[] excelBytes = orgCodeService.exportOrgCodes(orgId, codeGroupId, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("OrgCode", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrgCodeResponse>>> search(
            @ModelAttribute @Valid OrgCodeSearchRequest request) {
        log.info("GET /api/org-codes - orgId={}, codeGroupId={}", request.getOrgId(), request.getCodeGroupId());
        return ResponseEntity.ok(ApiResponse.success(orgCodeService.search(request)));
    }

    @GetMapping("/popup-data")
    public ResponseEntity<ApiResponse<List<OrgCodePopupResponse>>> getPopupData(
            @RequestParam String codeGroupId, @RequestParam String orgId) {
        log.info("GET /api/org-codes/popup-data - codeGroupId={}, orgId={}", codeGroupId, orgId);
        return ResponseEntity.ok(ApiResponse.success(orgCodeService.getPopupData(codeGroupId, orgId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORG_CODE:W')")
    public ResponseEntity<ApiResponse<Void>> saveAll(@Valid @RequestBody OrgCodeSaveRequest request) {
        log.info(
                "POST /api/org-codes - orgId={}, codeGroupId={}, rows={}",
                request.getOrgId(),
                request.getCodeGroupId(),
                request.getRows() != null ? request.getRows().size() : 0);
        orgCodeService.saveAll(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }
}

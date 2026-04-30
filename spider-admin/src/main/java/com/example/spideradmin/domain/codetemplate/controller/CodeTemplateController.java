package com.example.spideradmin.domain.codetemplate.controller;

import com.example.spideradmin.domain.codetemplate.dto.CodeTemplateCreateRequest;
import com.example.spideradmin.domain.codetemplate.dto.CodeTemplateResponse;
import com.example.spideradmin.domain.codetemplate.dto.CodeTemplateUpdateRequest;
import com.example.spideradmin.domain.codetemplate.service.CodeTemplateService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/code-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CODE_TEMPLATE:R')")
public class CodeTemplateController {

    private final CodeTemplateService codeTemplateService;

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<CodeTemplateResponse>>> getCodeTemplatesWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .searchField(searchField)
                .searchValue(searchValue)
                .build();

        PageResponse<CodeTemplateResponse> response = codeTemplateService.getCodeTemplates(pageRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<ApiResponse<CodeTemplateResponse>> getCodeTemplate(@PathVariable String templateId) {
        CodeTemplateResponse template = codeTemplateService.getCodeTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCodeTemplates(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue) {

        byte[] excelBytes = codeTemplateService.exportCodeTemplates(searchField, searchValue, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("CodeTemplate", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CODE_TEMPLATE:W')")
    public ResponseEntity<ApiResponse<CodeTemplateResponse>> createCodeTemplate(
            @Valid @RequestBody CodeTemplateCreateRequest dto) {
        CodeTemplateResponse created = codeTemplateService.createCodeTemplate(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("템플릿이 생성되었습니다", created));
    }

    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('CODE_TEMPLATE:W')")
    public ResponseEntity<ApiResponse<CodeTemplateResponse>> updateCodeTemplate(
            @PathVariable String templateId, @Valid @RequestBody CodeTemplateUpdateRequest dto) {
        CodeTemplateResponse updated = codeTemplateService.updateCodeTemplate(templateId, dto);
        return ResponseEntity.ok(ApiResponse.success("템플릿이 수정되었습니다", updated));
    }

    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAuthority('CODE_TEMPLATE:W')")
    public ResponseEntity<ApiResponse<Void>> deleteCodeTemplate(@PathVariable String templateId) {
        codeTemplateService.deleteCodeTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("템플릿이 삭제되었습니다", null));
    }

    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateSource(
            @RequestParam String trxId, @RequestParam(required = false) String orgId) {
        byte[] zipBytes = codeTemplateService.generateSourceZip(trxId, orgId);
        String fileName = trxId + "_source.zip";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok().headers(headers).body(zipBytes);
    }
}

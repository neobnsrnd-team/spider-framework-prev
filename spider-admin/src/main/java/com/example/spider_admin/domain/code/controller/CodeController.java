package com.example.spider_admin.domain.code.controller;

import com.example.spider_admin.domain.code.dto.CodeCreateRequest;
import com.example.spider_admin.domain.code.dto.CodeIdRequest;
import com.example.spider_admin.domain.code.dto.CodeResponse;
import com.example.spider_admin.domain.code.dto.CodeUpdateRequest;
import com.example.spider_admin.domain.code.dto.CodeWithGroupResponse;
import com.example.spider_admin.domain.code.service.CodeService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CODE_MANAGEMENT:R')")
public class CodeController {

    private final CodeService codeService;

    @GetMapping("/page-with-group")
    public ResponseEntity<ApiResponse<PageResponse<CodeWithGroupResponse>>> getCodesWithPaginationAndGroup(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String codeGroupId,
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

        PageResponse<CodeWithGroupResponse> response = codeService.getCodesWithGroup(pageRequest, codeGroupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{codeGroupId}/{code}")
    public ResponseEntity<ApiResponse<CodeWithGroupResponse>> getCodeById(
            @PathVariable String codeGroupId, @PathVariable String code) {
        CodeWithGroupResponse codeDTO = codeService.getCodeById(codeGroupId, code);
        return ResponseEntity.ok(ApiResponse.success(codeDTO));
    }

    @GetMapping("/by-group/{codeGroupId}")
    public ResponseEntity<ApiResponse<List<CodeResponse>>> getCodesByCodeGroupId(@PathVariable String codeGroupId) {
        List<CodeResponse> codes = codeService.getCodesByCodeGroupId(codeGroupId);
        return ResponseEntity.ok(ApiResponse.success(codes));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CODE_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<CodeResponse>> createCode(@Valid @RequestBody CodeCreateRequest dto) {
        CodeResponse createdCode = codeService.createCode(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("코드가 생성되었습니다", createdCode));
    }

    @PutMapping("/{codeGroupId}/{code}")
    @PreAuthorize("hasAuthority('CODE_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<CodeResponse>> updateCode(
            @PathVariable String codeGroupId, @PathVariable String code, @Valid @RequestBody CodeUpdateRequest dto) {
        CodeResponse updatedCode = codeService.updateCode(codeGroupId, code, dto);
        return ResponseEntity.ok(ApiResponse.success("코드가 수정되었습니다", updatedCode));
    }

    @DeleteMapping("/{codeGroupId}/{code}")
    @PreAuthorize("hasAuthority('CODE_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> deleteCode(@PathVariable String codeGroupId, @PathVariable String code) {
        codeService.deleteCode(codeGroupId, code);
        return ResponseEntity.ok(ApiResponse.success("코드가 삭제되었습니다", null));
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('CODE_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> deleteMultipleCodes(@RequestBody List<CodeIdRequest> codeIds) {
        codeService.deleteMultipleCodes(codeIds);
        return ResponseEntity.ok(ApiResponse.success("선택한 코드가 삭제되었습니다", null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCodes(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String codeGroupId,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue) {
        byte[] excelBytes = codeService.exportCodes(codeGroupId, searchField, searchValue, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("Code", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping("/count/{codeGroupId}")
    public ResponseEntity<ApiResponse<Long>> countByCodeGroupId(@PathVariable String codeGroupId) {
        long count = codeService.countByCodeGroupId(codeGroupId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}

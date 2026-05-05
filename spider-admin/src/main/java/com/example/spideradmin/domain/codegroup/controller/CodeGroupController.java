package com.example.spideradmin.domain.codegroup.controller;

import com.example.spideradmin.domain.codegroup.dto.CodeGroupCreateRequest;
import com.example.spideradmin.domain.codegroup.dto.CodeGroupResponse;
import com.example.spideradmin.domain.codegroup.dto.CodeGroupUpdateRequest;
import com.example.spideradmin.domain.codegroup.dto.CodeGroupWithCodesResponse;
import com.example.spideradmin.domain.codegroup.service.CodeGroupService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/code-groups")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CODE_GROUP:R')")
public class CodeGroupController {

    private final CodeGroupService codeGroupService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CodeGroupResponse>>> getAllCodeGroups() {
        List<CodeGroupResponse> codeGroups = codeGroupService.getAllCodeGroupsWithCodeCount();
        return ResponseEntity.ok(ApiResponse.success(codeGroups));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<CodeGroupResponse>>> getCodeGroupsWithPagination(
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

        PageResponse<CodeGroupResponse> response = codeGroupService.getCodeGroups(pageRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCodeGroups(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue) {

        byte[] excelBytes = codeGroupService.exportCodeGroups(searchField, searchValue, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("CodeGroup", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping("/{codeGroupId}/with-codes")
    public ResponseEntity<ApiResponse<CodeGroupWithCodesResponse>> getCodeGroupWithCodes(
            @PathVariable String codeGroupId) {
        CodeGroupWithCodesResponse codeGroup = codeGroupService.getCodeGroupWithCodes(codeGroupId);
        return ResponseEntity.ok(ApiResponse.success(codeGroup));
    }

    @GetMapping("/by-biz-group/{bizGroupId}")
    public ResponseEntity<ApiResponse<List<CodeGroupResponse>>> getCodeGroupsByBizGroupId(
            @PathVariable String bizGroupId) {
        List<CodeGroupResponse> codeGroups = codeGroupService.getCodeGroupsByBizGroupId(bizGroupId);
        return ResponseEntity.ok(ApiResponse.success(codeGroups));
    }

    @PostMapping("/with-codes")
    @PreAuthorize("hasAuthority('CODE_GROUP:W')")
    public ResponseEntity<ApiResponse<CodeGroupWithCodesResponse>> createCodeGroupWithCodes(
            @Valid @RequestBody CodeGroupCreateRequest dto) {
        CodeGroupWithCodesResponse createdCodeGroup = codeGroupService.createCodeGroupWithCodes(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("코드 그룹과 하위 코드가 생성되었습니다", createdCodeGroup));
    }

    @PutMapping("/{codeGroupId}/with-codes")
    @PreAuthorize("hasAuthority('CODE_GROUP:W')")
    public ResponseEntity<ApiResponse<CodeGroupWithCodesResponse>> updateCodeGroupWithCodes(
            @PathVariable String codeGroupId, @Valid @RequestBody CodeGroupUpdateRequest dto) {
        CodeGroupWithCodesResponse updatedCodeGroup = codeGroupService.updateCodeGroupWithCodes(codeGroupId, dto);
        return ResponseEntity.ok(ApiResponse.success("코드 그룹과 하위 코드가 수정되었습니다", updatedCodeGroup));
    }

    @DeleteMapping("/{codeGroupId}/with-codes")
    @PreAuthorize("hasAuthority('CODE_GROUP:W')")
    public ResponseEntity<ApiResponse<Void>> deleteCodeGroupWithCodes(@PathVariable String codeGroupId) {
        codeGroupService.deleteCodeGroupWithCodes(codeGroupId);
        return ResponseEntity.ok(ApiResponse.success("코드 그룹과 하위 코드가 모두 삭제되었습니다", null));
    }
}

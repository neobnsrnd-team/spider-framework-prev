package com.example.spideradmin.domain.accessuser.controller;

import com.example.spideradmin.domain.accessuser.dto.AccessUserCreateRequest;
import com.example.spideradmin.domain.accessuser.dto.AccessUserResponse;
import com.example.spideradmin.domain.accessuser.dto.AccessUserSearchRequest;
import com.example.spideradmin.domain.accessuser.dto.AccessUserUpdateRequest;
import com.example.spideradmin.domain.accessuser.service.AccessUserService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <h3>중지거래 접근허용자 REST Controller</h3>
 * <p>중지거래 접근허용자 관리 API를 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/access-users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ACCESS_USER:R')")
public class AccessUserController {

    private final AccessUserService accessUserService;

    /**
     * 페이징 처리된 중지거래 접근허용자 목록을 조회합니다.
     *
     * @param page 페이지 번호 (1-based index)
     * @param size 페이지 당 항목 수
     * @param trxId 거래/서비스 ID 검색 조건
     * @param gubunType 구분유형 검색 조건
     * @param custUserId 접근허용 사용자 ID 검색 조건
     * @return 페이징 처리된 중지거래 접근허용자 목록
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<AccessUserResponse>>> getAccessUsersWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String trxId,
            @RequestParam(required = false) String gubunType,
            @RequestParam(required = false) String custUserId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1) // 1-based → 0-based 변환
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        AccessUserSearchRequest searchDTO = AccessUserSearchRequest.builder()
                .trxId(trxId)
                .gubunType(gubunType)
                .custUserId(custUserId)
                .build();

        PageResponse<AccessUserResponse> response =
                accessUserService.searchAccessUsersWithPagination(pageRequest, searchDTO);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAccessUsers(
            @RequestParam(required = false) String trxId,
            @RequestParam(required = false) String gubunType,
            @RequestParam(required = false) String custUserId) {
        byte[] excelBytes = accessUserService.exportAccessUsers(trxId, gubunType, custUserId);
        String fileName = ExcelExportUtil.generateFileName("AccessUser", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCESS_USER:W')")
    public ResponseEntity<ApiResponse<AccessUserResponse>> createAccessUser(
            @Valid @RequestBody AccessUserCreateRequest dto) {
        AccessUserResponse accessor = accessUserService.createAccessUser(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("중지거래 접근허용자가 생성되었습니다.", accessor));
    }

    /**
     * 중지거래 접근허용자를 수정합니다.
     *
     * @param dto 수정 요청 DTO (body에 PK 3개 포함)
     * @return 수정된 중지거래 접근허용자 정보
     */
    @PutMapping
    @PreAuthorize("hasAuthority('ACCESS_USER:W')")
    public ResponseEntity<ApiResponse<AccessUserResponse>> updateAccessUser(
            @Valid @RequestBody AccessUserUpdateRequest dto) {
        AccessUserResponse accessor = accessUserService.updateAccessUser(dto);

        return ResponseEntity.ok(ApiResponse.success("중지거래 접근허용자가 수정되었습니다.", accessor));
    }

    /**
     * 중지거래 접근허용자를 삭제합니다. (복합 PK)
     *
     * @param gubunType 구분유형
     * @param trxId 거래/서비스 ID
     * @param custUserId 접근허용 사용자 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{gubunType}/{trxId}/{custUserId}")
    @PreAuthorize("hasAuthority('ACCESS_USER:W')")
    public ResponseEntity<ApiResponse<Void>> deleteAccessUser(
            @PathVariable String gubunType, @PathVariable String trxId, @PathVariable String custUserId) {
        accessUserService.deleteAccessUser(gubunType, trxId, custUserId);

        return ResponseEntity.ok(ApiResponse.success("중지거래 접근허용자가 삭제되었습니다.", null));
    }
}

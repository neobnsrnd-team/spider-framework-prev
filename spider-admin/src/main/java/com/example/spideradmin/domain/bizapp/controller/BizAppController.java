package com.example.spideradmin.domain.bizapp.controller;

import com.example.spideradmin.domain.bizapp.dto.BizAppCreateRequest;
import com.example.spideradmin.domain.bizapp.dto.BizAppResponse;
import com.example.spideradmin.domain.bizapp.dto.BizAppSearchRequest;
import com.example.spideradmin.domain.bizapp.dto.BizAppUpdateRequest;
import com.example.spideradmin.domain.bizapp.service.BizAppService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Biz App 관리 REST Controller
 *
 * <p>클래스 레벨: BIZ_APP:R (읽기 기본), 쓰기 엔드포인트는 BIZ_APP:W 오버라이드
 */
@Slf4j
@RestController
@RequestMapping("/api/biz-apps")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BIZ_APP:R')")
public class BizAppController {

    private final BizAppService bizAppService;

    /** Biz App 페이징 검색 조회 */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<BizAppResponse>>> getBizAppsWithPagination(
            @ModelAttribute BizAppSearchRequest searchDTO) {
        log.info(
                "GET /api/biz-apps/page - page: {}, size: {}, bizAppId: {}, bizAppName: {}, dupCheckYn: {}, logYn: {}",
                searchDTO.getPage(),
                searchDTO.getSize(),
                searchDTO.getBizAppId(),
                searchDTO.getBizAppName(),
                searchDTO.getDupCheckYn(),
                searchDTO.getLogYn());
        return ResponseEntity.ok(ApiResponse.success(bizAppService.getBizAppsWithSearch(searchDTO)));
    }

    /** Biz App 단건 조회 */
    @GetMapping("/{bizAppId}")
    public ResponseEntity<ApiResponse<BizAppResponse>> getById(@PathVariable String bizAppId) {
        log.info("GET /api/biz-apps/{}", bizAppId);
        return ResponseEntity.ok(ApiResponse.success(bizAppService.getById(bizAppId)));
    }

    /** Biz App 등록 */
    @PostMapping
    @PreAuthorize("hasAuthority('BIZ_APP:W')")
    public ResponseEntity<ApiResponse<BizAppResponse>> create(@Valid @RequestBody BizAppCreateRequest dto) {
        log.info("POST /api/biz-apps - bizAppId: {}", dto.getBizAppId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(bizAppService.create(dto)));
    }

    /** Biz App 수정 */
    @PutMapping("/{bizAppId}")
    @PreAuthorize("hasAuthority('BIZ_APP:W')")
    public ResponseEntity<ApiResponse<BizAppResponse>> update(
            @PathVariable String bizAppId, @Valid @RequestBody BizAppUpdateRequest dto) {
        log.info("PUT /api/biz-apps/{}", bizAppId);
        return ResponseEntity.ok(ApiResponse.success(bizAppService.update(bizAppId, dto)));
    }

    /** Biz App 삭제 */
    @DeleteMapping("/{bizAppId}")
    @PreAuthorize("hasAuthority('BIZ_APP:W')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String bizAppId) {
        log.info("DELETE /api/biz-apps/{}", bizAppId);
        bizAppService.delete(bizAppId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

package com.example.spideradmin.domain.component.controller;

import com.example.spideradmin.domain.component.dto.ComponentCreateRequest;
import com.example.spideradmin.domain.component.dto.ComponentResponse;
import com.example.spideradmin.domain.component.dto.ComponentSearchRequest;
import com.example.spideradmin.domain.component.dto.ComponentUpdateRequest;
import com.example.spideradmin.domain.component.service.ComponentService;
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
 * 컴포넌트 관리 REST Controller
 *
 * <p>클래스 레벨: COMPONENT:R (읽기 기본), 쓰기 엔드포인트는 COMPONENT:W 오버라이드
 */
@Slf4j
@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('COMPONENT:R')")
public class ComponentController {

    private final ComponentService componentService;

    /** 컴포넌트 페이징 검색 조회 */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<ComponentResponse>>> getComponentsWithPagination(
            @ModelAttribute ComponentSearchRequest searchDTO) {
        log.info("GET /api/components/page - search: {}", searchDTO);
        return ResponseEntity.ok(ApiResponse.success(componentService.getComponentsWithSearch(searchDTO)));
    }

    /** 컴포넌트 단건 조회 (파라미터 포함) */
    @GetMapping("/{componentId}")
    public ResponseEntity<ApiResponse<ComponentResponse>> getById(@PathVariable String componentId) {
        log.info("GET /api/components/{}", componentId);
        return ResponseEntity.ok(ApiResponse.success(componentService.getById(componentId)));
    }

    /** 컴포넌트 등록 */
    @PostMapping
    @PreAuthorize("hasAuthority('COMPONENT:W')")
    public ResponseEntity<ApiResponse<ComponentResponse>> create(@Valid @RequestBody ComponentCreateRequest dto) {
        log.info("POST /api/components - componentId: {}", dto.getComponentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(componentService.create(dto)));
    }

    /** 컴포넌트 수정 */
    @PutMapping("/{componentId}")
    @PreAuthorize("hasAuthority('COMPONENT:W')")
    public ResponseEntity<ApiResponse<ComponentResponse>> update(
            @PathVariable String componentId, @Valid @RequestBody ComponentUpdateRequest dto) {
        log.info("PUT /api/components/{}", componentId);
        return ResponseEntity.ok(ApiResponse.success(componentService.update(componentId, dto)));
    }

    /** 컴포넌트 삭제 */
    @DeleteMapping("/{componentId}")
    @PreAuthorize("hasAuthority('COMPONENT:W')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String componentId) {
        log.info("DELETE /api/components/{}", componentId);
        // componentType은 WorkList 이력 적재 시 workId로 사용된다.
        String componentType = componentService.getById(componentId).getComponentType();
        componentService.delete(componentId, componentType);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

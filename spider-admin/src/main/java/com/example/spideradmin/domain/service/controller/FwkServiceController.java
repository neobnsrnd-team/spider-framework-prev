package com.example.spideradmin.domain.service.controller;

import com.example.spideradmin.domain.service.dto.FwkServiceCreateRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceDetailResponse;
import com.example.spideradmin.domain.service.dto.FwkServiceResponse;
import com.example.spideradmin.domain.service.dto.FwkServiceSearchRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceUpdateRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceUseYnBulkRequest;
import com.example.spideradmin.domain.service.dto.WorkSpaceResponse;
import com.example.spideradmin.domain.service.service.FwkServiceService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.io.IOException;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 관리 REST Controller (FWK_SERVICE).
 *
 * <p>클래스 레벨: FWK_SERVICE:R (읽기 기본), 쓰기 엔드포인트는 FWK_SERVICE:W 오버라이드.
 * 클래스명에 Fwk 접두어 사용 — domain/service 패키지가 Spring @Service 어노테이션과 혼동 방지.
 */
@Slf4j
@RestController
@RequestMapping("/api/fwk-services")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('FWK_SERVICE:R')")
public class FwkServiceController {

    private final FwkServiceService fwkServiceService;

    /** 서비스 페이징 검색 조회 */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<FwkServiceResponse>>> getServicesWithPagination(
            @ModelAttribute FwkServiceSearchRequest searchDTO) {
        log.info("GET /api/fwk-services/page - search: {}", searchDTO);
        return ResponseEntity.ok(ApiResponse.success(fwkServiceService.getServicesWithSearch(searchDTO)));
    }

    /** 서비스 단건 상세 조회 (연결 컴포넌트 포함) */
    @GetMapping("/{serviceId}")
    public ResponseEntity<ApiResponse<FwkServiceDetailResponse>> getById(@PathVariable String serviceId) {
        log.info("GET /api/fwk-services/{}", serviceId);
        return ResponseEntity.ok(ApiResponse.success(fwkServiceService.getById(serviceId)));
    }

    /** 서비스 등록 */
    @PostMapping
    @PreAuthorize("hasAuthority('FWK_SERVICE:W')")
    public ResponseEntity<ApiResponse<FwkServiceDetailResponse>> create(
            @Valid @RequestBody FwkServiceCreateRequest dto) {
        log.info("POST /api/fwk-services - serviceId: {}", dto.getServiceId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(fwkServiceService.create(dto)));
    }

    /** 서비스 수정 */
    @PutMapping("/{serviceId}")
    @PreAuthorize("hasAuthority('FWK_SERVICE:W')")
    public ResponseEntity<ApiResponse<FwkServiceDetailResponse>> update(
            @PathVariable String serviceId, @Valid @RequestBody FwkServiceUpdateRequest dto) {
        log.info("PUT /api/fwk-services/{}", serviceId);
        return ResponseEntity.ok(ApiResponse.success(fwkServiceService.update(serviceId, dto)));
    }

    /** 서비스 삭제 (3계층 cascade: RELATION_PARAM → RELATION → SERVICE) */
    @DeleteMapping("/{serviceId}")
    @PreAuthorize("hasAuthority('FWK_SERVICE:W')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String serviceId) {
        log.info("DELETE /api/fwk-services/{}", serviceId);
        // serviceType은 WorkList 이력 적재 시 workId로 사용된다.
        String serviceType = fwkServiceService.getById(serviceId).getServiceType();
        fwkServiceService.delete(serviceId, serviceType);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** USE_YN 일괄 변경 */
    @PatchMapping("/use-yn/bulk")
    @PreAuthorize("hasAuthority('FWK_SERVICE:W')")
    public ResponseEntity<ApiResponse<Void>> bulkUpdateUseYn(@Valid @RequestBody FwkServiceUseYnBulkRequest dto) {
        log.info(
                "PATCH /api/fwk-services/use-yn/bulk - count: {}",
                dto.getServiceIds().size());
        fwkServiceService.bulkUpdateUseYn(dto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** WorkSpace 팝업 조회 */
    @GetMapping("/popup/workspaces")
    public ResponseEntity<ApiResponse<PageResponse<WorkSpaceResponse>>> getWorkspacePopup(
            @RequestParam(required = false) String workSpaceId,
            @RequestParam(required = false) String workSpaceName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/fwk-services/popup/workspaces - workSpaceId: {}", workSpaceId);
        int offset = (page - 1) * size;
        List<WorkSpaceResponse> content = fwkServiceService.getWorkspacePage(workSpaceId, workSpaceName, offset, size);
        int total = fwkServiceService.countWorkspace(workSpaceId, workSpaceName);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(content, total, page, size)));
    }

    /** 서비스 목록 Excel 내보내기 */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute FwkServiceSearchRequest searchDTO) throws IOException {
        log.info("GET /api/fwk-services/export");
        byte[] bytes = fwkServiceService.exportFwkServices(searchDTO);
        String fileName = ExcelExportUtil.generateFileName("FwkService", LocalDate.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}

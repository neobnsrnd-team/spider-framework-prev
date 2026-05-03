/**
 * @file ReactApprovalController.java
 * @description React 코드 승인 관리 API 컨트롤러.
 *     승인 대기 목록 조회, 승인, 반려 엔드포인트를 제공한다.
 *     읽기는 REACT_APPROVAL:R, 쓰기(승인·반려)는 REACT_APPROVAL:W 권한이 필요하다.
 */
package com.example.reactplatform.domain.reactapproval.controller;

import com.example.reactplatform.domain.reactapproval.service.ReactApprovalService;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateApprovalResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactRejectRequest;
import com.example.reactplatform.global.dto.ApiResponse;
import com.example.reactplatform.global.util.SecurityUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated // @RequestParam에 적용된 @Min/@Max 검증을 활성화
@RestController
@RequestMapping("/api/react-approval")
@RequiredArgsConstructor
public class ReactApprovalController {

    private final ReactApprovalService reactApprovalService;

    /**
     * 승인 대기(PENDING_APPROVAL) 목록을 페이지네이션 형태로 조회한다.
     *
     * @param page 페이지 번호 (기본값 1, 최솟값 1)
     * @param size 페이지당 건수 (기본값 10, 범위 1~100)
     * @return list(목록), totalCount, page, size
     */
    @GetMapping
    @PreAuthorize("hasAuthority('REACT_APPROVAL:R')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingList(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(reactApprovalService.getPendingList(page, size)));
    }

    /**
     * 승인 이력(APPROVED / REJECTED) 목록을 페이지네이션 형태로 조회한다.
     *
     * @param page           페이지 번호 (기본값 1, 최솟값 1)
     * @param size           페이지당 건수 (기본값 10, 범위 1~100)
     * @param status         상태 필터 (APPROVED / REJECTED, 미입력 시 전체)
     * @param title          화면 제목 부분 일치 검색
     * @param componentName  컴포넌트명 부분 일치 검색
     * @param approvalUserId 처리자 ID 부분 일치 검색
     * @param createUserId   요청자 ID 부분 일치 검색
     * @param fromDate       처리일시 시작 (yyyyMMdd)
     * @param toDate         처리일시 종료 (yyyyMMdd)
     * @return list(목록), totalCount, page, size
     */
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('REACT_APPROVAL:R')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String componentName,
            @RequestParam(defaultValue = "") String approvalUserId,
            @RequestParam(defaultValue = "") String createUserId,
            @RequestParam(defaultValue = "") String fromDate,
            @RequestParam(defaultValue = "") String toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reactApprovalService.getHistory(page, size, status, title, componentName, approvalUserId, createUserId, fromDate, toDate)));
    }

    /**
     * 승인 대기 코드를 승인한다.
     *
     * <p>요청자 본인은 승인할 수 없으며, PENDING_APPROVAL 상태인 코드만 승인 가능하다.
     *
     * @param id 승인할 코드 ID
     * @return 변경된 상태 및 승인자 정보
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('REACT_APPROVAL:W')")
    public ResponseEntity<ApiResponse<ReactGenerateApprovalResponse>> approve(@PathVariable String id) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(reactApprovalService.approve(id, currentUserId)));
    }

    /**
     * 코드를 반려한다.
     *
     * <p>상태에 관계없이 어느 단계에서든 반려 가능하다.
     *
     * @param id      반려할 코드 ID
     * @param request 반려 사유 (최대 500자, 선택)
     * @return 변경된 상태 및 반려자 정보
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('REACT_APPROVAL:W')")
    public ResponseEntity<ApiResponse<ReactGenerateApprovalResponse>> reject(
            @PathVariable String id, @RequestBody(required = false) @Valid ReactRejectRequest request) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(ApiResponse.success(reactApprovalService.reject(id, currentUserId, reason)));
    }
}

/**
 * @file ReactDeployController.java
 * @description React 코드 배포 관리 API 컨트롤러.
 *     배포 가능 목록, 재배포 실행, 전체 이력, 코드별 이력 엔드포인트를 제공한다.
 *     읽기는 REACT_DEPLOY:R, 재배포 실행은 REACT_DEPLOY:W 권한이 필요하다.
 */
package com.example.reactplatform.domain.reactdeploy.controller;

import com.example.reactplatform.domain.reactdeploy.dto.ReactRedeployResponse;
import com.example.reactplatform.domain.reactdeploy.service.ReactDeployService;
import com.example.reactplatform.global.dto.ApiResponse;
import com.example.reactplatform.global.util.SecurityUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/react-deploy")
@RequiredArgsConstructor
public class ReactDeployController {

    private final ReactDeployService reactDeployService;

    /**
     * 배포 가능 목록을 조회한다 (APPROVED 코드 + 최근 배포 이력).
     *
     * @param page   페이지 번호 (기본값 1)
     * @param size   페이지당 건수 (기본값 10)
     * @param search 코드 ID 또는 요청자 ID 검색 키워드
     */
    @GetMapping
    @PreAuthorize("hasAuthority('REACT_DEPLOY:R')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeployList(
            @RequestParam(defaultValue = "1")  @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(ApiResponse.success(reactDeployService.findDeployList(page, size, search)));
    }

    /**
     * 재배포를 실행한다.
     *
     * <p>APPROVED 상태 코드만 재배포 가능하다.
     *
     * @param codeId 재배포할 코드 ID
     */
    @PostMapping("/{codeId}/redeploy")
    @PreAuthorize("hasAuthority('REACT_DEPLOY:W')")
    public ResponseEntity<ApiResponse<ReactRedeployResponse>> redeploy(@PathVariable String codeId) {
        String currentUserId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(reactDeployService.redeploy(codeId, currentUserId)));
    }

    /**
     * 전체 배포 이력을 조회한다 (하단 이력 테이블용).
     *
     * @param page     페이지 번호 (기본값 1)
     * @param size     페이지당 건수 (기본값 20)
     * @param search   코드 ID 또는 실행자 ID 검색 키워드
     * @param onlyMine true이면 로그인 사용자의 이력만 조회
     */
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('REACT_DEPLOY:R')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllHistory(
            @RequestParam(defaultValue = "1")  @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "false") boolean onlyMine) {
        String userId = onlyMine ? SecurityUtil.getCurrentUserId() : null;
        // onlyMine 요청인데 사용자 ID를 확인할 수 없으면 전체 이력 노출을 방지하고 빈 결과를 반환한다
        if (onlyMine && userId == null) {
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("list", List.of(), "totalCount", 0, "page", page, "size", size)));
        }
        return ResponseEntity.ok(ApiResponse.success(reactDeployService.findAllHistoryList(page, size, search, userId)));
    }

    /**
     * 특정 코드의 배포 이력을 조회한다 (모달용).
     *
     * @param codeId 조회할 코드 ID
     * @param page   페이지 번호 (기본값 1)
     * @param size   페이지당 건수 (기본값 20)
     */
    @GetMapping("/{codeId}/history")
    @PreAuthorize("hasAuthority('REACT_DEPLOY:R')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistoryByCodeId(
            @PathVariable String codeId,
            @RequestParam(defaultValue = "1")  @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.success(reactDeployService.findHistoryByCodeId(codeId, page, size)));
    }
}

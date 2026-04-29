package com.example.spider_admin.domain.board.controller;

import com.example.spider_admin.domain.board.dto.BoardAuthCreateRequest;
import com.example.spider_admin.domain.board.dto.BoardAuthResponse;
import com.example.spider_admin.domain.board.dto.BoardAuthUpdateRequest;
import com.example.spider_admin.domain.board.service.BoardAuthService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.SecurityUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 게시판 권한 관리 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/board-auth")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BOARD_AUTH:R')")
public class BoardAuthController {

    private final BoardAuthService boardAuthService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardAuthResponse>>> getAllAuthorities() {
        log.info("GET /api/board-auth - Fetching all board authorities");
        List<BoardAuthResponse> authorities = boardAuthService.getAllAuthorities();
        return ResponseEntity.ok(ApiResponse.success(authorities));
    }

    /**
     * 필터 조건으로 권한 목록 페이징 조회 (서버 사이드 필터링)
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<BoardAuthResponse>>> getAuthoritiesPage(
            @RequestParam(required = false) String boardId,
            @RequestParam(required = false) String userKeyword,
            @RequestParam(required = false) String authCode,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info(
                "GET /api/board-auth/page - Fetching board authorities with filter: boardId={}, userKeyword={}, authCode={}, page={}, size={}",
                boardId,
                userKeyword,
                authCode,
                page,
                size);
        PageRequest pageRequest =
                PageRequest.builder().page(page - 1).size(size).build();
        PageResponse<BoardAuthResponse> authorities = boardAuthService.getAuthoritiesWithFilter(
                boardId, userKeyword, authCode, sortBy, sortDirection, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(authorities));
    }

    @GetMapping("/{userId}/{boardId}")
    public ResponseEntity<ApiResponse<BoardAuthResponse>> getAuthById(
            @PathVariable String userId, @PathVariable String boardId) {
        log.info("GET /api/board-auth/{}/{} - Fetching board auth", userId, boardId);
        BoardAuthResponse auth = boardAuthService.getAuthById(userId, boardId);
        return ResponseEntity.ok(ApiResponse.success(auth));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<BoardAuthResponse>>> getAuthsByUserId(@PathVariable String userId) {
        log.info("GET /api/board-auth/user/{} - Fetching authorities by user ID", userId);
        List<BoardAuthResponse> authorities = boardAuthService.getAuthsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(authorities));
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<ApiResponse<List<BoardAuthResponse>>> getAuthsByBoardId(@PathVariable String boardId) {
        log.info("GET /api/board-auth/board/{} - Fetching authorities by board ID", boardId);
        List<BoardAuthResponse> authorities = boardAuthService.getAuthsByBoardId(boardId);
        return ResponseEntity.ok(ApiResponse.success(authorities));
    }

    @GetMapping("/check/{userId}/{boardId}")
    public ResponseEntity<ApiResponse<String>> checkAuthCode(
            @PathVariable String userId, @PathVariable String boardId) {
        log.info("GET /api/board-auth/check/{}/{} - Checking auth code", userId, boardId);
        String authCode = boardAuthService.getAuthCode(userId, boardId);
        return ResponseEntity.ok(ApiResponse.success(authCode));
    }

    @GetMapping("/can-write/{userId}/{boardId}")
    public ResponseEntity<ApiResponse<Boolean>> checkWritePermission(
            @PathVariable String userId, @PathVariable String boardId) {
        log.info("GET /api/board-auth/can-write/{}/{} - Checking write permission", userId, boardId);
        boolean hasPermission = boardAuthService.hasWritePermission(userId, boardId);
        return ResponseEntity.ok(ApiResponse.success(hasPermission));
    }

    @GetMapping("/my/{boardId}")
    public ResponseEntity<ApiResponse<String>> getMyAuth(@PathVariable String boardId) {
        log.info("GET /api/board-auth/my/{} - Fetching current user's auth", boardId);
        String userId = SecurityUtil.getCurrentUserIdOrSystem();
        log.info("Current userId from SecurityUtil: '{}', boardId: '{}'", userId, boardId);
        String authCode = boardAuthService.getAuthCode(userId, boardId);
        log.info("AuthCode result for userId='{}', boardId='{}': '{}'", userId, boardId, authCode);
        return ResponseEntity.ok(ApiResponse.success(authCode));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BOARD_AUTH:W')")
    public ResponseEntity<ApiResponse<BoardAuthResponse>> createAuth(
            @Valid @RequestBody BoardAuthCreateRequest request) {
        log.info(
                "POST /api/board-auth - Creating board auth: userId={}, boardId={}, authCode={}",
                request.getUserId(),
                request.getBoardId(),
                request.getAuthCode());
        String updateUserId = SecurityUtil.getCurrentUserIdOrSystem();
        BoardAuthResponse createdAuth = boardAuthService.createAuth(
                request.getUserId(), request.getBoardId(), request.getAuthCode(), updateUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("권한이 부여되었습니다", createdAuth));
    }

    @PutMapping("/{userId}/{boardId}")
    @PreAuthorize("hasAuthority('BOARD_AUTH:W')")
    public ResponseEntity<ApiResponse<BoardAuthResponse>> updateAuth(
            @PathVariable String userId,
            @PathVariable String boardId,
            @Valid @RequestBody BoardAuthUpdateRequest request) {
        log.info("PUT /api/board-auth/{}/{} - Updating board auth", userId, boardId);
        String updateUserId = SecurityUtil.getCurrentUserIdOrSystem();
        BoardAuthResponse updatedAuth =
                boardAuthService.updateAuth(userId, boardId, request.getAuthCode(), updateUserId);
        return ResponseEntity.ok(ApiResponse.success("권한이 수정되었습니다", updatedAuth));
    }

    @DeleteMapping("/{userId}/{boardId}")
    @PreAuthorize("hasAuthority('BOARD_AUTH:W')")
    public ResponseEntity<ApiResponse<Void>> deleteAuth(@PathVariable String userId, @PathVariable String boardId) {
        log.info("DELETE /api/board-auth/{}/{} - Deleting board auth", userId, boardId);
        boardAuthService.deleteAuth(userId, boardId);
        return ResponseEntity.ok(ApiResponse.success("권한이 삭제되었습니다", null));
    }

    @DeleteMapping("/board/{boardId}")
    @PreAuthorize("hasAuthority('BOARD_AUTH:W')")
    public ResponseEntity<ApiResponse<Void>> deleteAuthsByBoardId(@PathVariable String boardId) {
        log.info("DELETE /api/board-auth/board/{} - Deleting all authorities for board", boardId);
        boardAuthService.deleteAuthsByBoardId(boardId);
        return ResponseEntity.ok(ApiResponse.success("게시판의 모든 권한이 삭제되었습니다", null));
    }
}

package com.example.spider_admin.domain.board.controller;

import com.example.spider_admin.domain.board.dto.BoardCategoryCreateRequest;
import com.example.spider_admin.domain.board.dto.BoardCategoryResponse;
import com.example.spider_admin.domain.board.dto.BoardCategoryUpdateRequest;
import com.example.spider_admin.domain.board.service.BoardCategoryService;
import com.example.spider_admin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 게시판 카테고리 관리 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/board-categories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BOARD_MANAGEMENT:R')")
public class BoardCategoryController {

    private final BoardCategoryService boardCategoryService;

    @GetMapping("/board/{boardId}")
    public ResponseEntity<ApiResponse<List<BoardCategoryResponse>>> getCategoriesByBoardId(
            @PathVariable String boardId) {
        log.info("GET /api/board-categories/board/{} - Fetching categories by board ID", boardId);
        List<BoardCategoryResponse> categories = boardCategoryService.getCategoriesByBoardId(boardId);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{boardId}/{categorySeq}")
    public ResponseEntity<ApiResponse<BoardCategoryResponse>> getCategoryById(
            @PathVariable String boardId, @PathVariable String categorySeq) {
        log.info("GET /api/board-categories/{}/{} - Fetching category", boardId, categorySeq);
        BoardCategoryResponse category = boardCategoryService.getCategoryById(boardId, categorySeq);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping("/{boardId}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<BoardCategoryResponse>> createCategory(
            @PathVariable String boardId, @Valid @RequestBody BoardCategoryCreateRequest request) {
        log.info("POST /api/board-categories/{} - Creating new category: {}", boardId, request.getCategoryName());
        BoardCategoryResponse createdCategory = boardCategoryService.createCategory(boardId, request.getCategoryName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("카테고리가 생성되었습니다", createdCategory));
    }

    @PutMapping("/{boardId}/{categorySeq}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<BoardCategoryResponse>> updateCategory(
            @PathVariable String boardId,
            @PathVariable String categorySeq,
            @Valid @RequestBody BoardCategoryUpdateRequest request) {
        log.info("PUT /api/board-categories/{}/{} - Updating category", boardId, categorySeq);
        BoardCategoryResponse updatedCategory =
                boardCategoryService.updateCategory(boardId, categorySeq, request.getCategoryName());
        return ResponseEntity.ok(ApiResponse.success("카테고리가 수정되었습니다", updatedCategory));
    }

    @DeleteMapping("/{boardId}/{categorySeq}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable String boardId, @PathVariable String categorySeq) {
        log.info("DELETE /api/board-categories/{}/{} - Deleting category", boardId, categorySeq);
        boardCategoryService.deleteCategory(boardId, categorySeq);
        return ResponseEntity.ok(ApiResponse.success("카테고리가 삭제되었습니다", null));
    }
}

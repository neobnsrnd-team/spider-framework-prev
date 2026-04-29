package com.example.spider_admin.domain.board.controller;

import com.example.spider_admin.domain.board.dto.BoardCreateRequest;
import com.example.spider_admin.domain.board.dto.BoardResponse;
import com.example.spider_admin.domain.board.dto.BoardSearchRequest;
import com.example.spider_admin.domain.board.dto.BoardUpdateRequest;
import com.example.spider_admin.domain.board.service.BoardService;
import com.example.spider_admin.global.dto.ApiResponse;
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
 * 게시판 관리 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BOARD_MANAGEMENT:R')")
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getAllBoards() {
        log.info("GET /api/boards - Fetching all boards");
        List<BoardResponse> boards = boardService.getAllBoards();
        return ResponseEntity.ok(ApiResponse.success(boards));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<BoardResponse>>> getBoardsWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String boardId,
            @RequestParam(required = false) String boardName,
            @RequestParam(required = false) String boardType,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        log.info(
                "GET /api/boards/page - page: {}, size: {}, boardId: {}, boardName: {}, boardType: {}",
                page,
                size,
                boardId,
                boardName,
                boardType);
        BoardSearchRequest searchRequest = BoardSearchRequest.builder()
                .page(page)
                .size(size)
                .boardId(boardId)
                .boardName(boardName)
                .boardType(boardType)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        PageResponse<BoardResponse> response = boardService.searchBoards(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<BoardResponse>> getBoardById(@PathVariable String boardId) {
        log.info("GET /api/boards/{} - Fetching board by ID", boardId);
        BoardResponse board = boardService.getBoardById(boardId);
        return ResponseEntity.ok(ApiResponse.success(board));
    }

    @GetMapping("/type/{boardType}")
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoardsByType(@PathVariable String boardType) {
        log.info("GET /api/boards/type/{} - Fetching boards by type", boardType);
        List<BoardResponse> boards = boardService.getBoardsByType(boardType);
        return ResponseEntity.ok(ApiResponse.success(boards));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<BoardResponse>> createBoard(@Valid @RequestBody BoardCreateRequest requestDTO) {
        log.info("POST /api/boards - Creating new board: {}", requestDTO.getBoardId());
        String userId = SecurityUtil.getCurrentUserIdOrSystem();
        BoardResponse createdBoard = boardService.createBoard(requestDTO, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("게시판이 생성되었습니다", createdBoard));
    }

    @PutMapping("/{boardId}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<BoardResponse>> updateBoard(
            @PathVariable String boardId, @Valid @RequestBody BoardUpdateRequest requestDTO) {
        log.info("PUT /api/boards/{} - Updating board", boardId);
        String userId = SecurityUtil.getCurrentUserIdOrSystem();
        BoardResponse updatedBoard = boardService.updateBoard(boardId, requestDTO, userId);
        return ResponseEntity.ok(ApiResponse.success("게시판이 수정되었습니다", updatedBoard));
    }

    @DeleteMapping("/{boardId}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(@PathVariable String boardId) {
        log.info("DELETE /api/boards/{} - Deleting board", boardId);
        boardService.deleteBoard(boardId);
        return ResponseEntity.ok(ApiResponse.success("게시판이 삭제되었습니다", null));
    }

    @GetMapping("/check/{boardId}")
    public ResponseEntity<ApiResponse<Boolean>> checkBoardIdExists(@PathVariable String boardId) {
        log.info("GET /api/boards/check/{} - Checking board ID exists", boardId);
        boolean exists = boardService.existsById(boardId);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }
}

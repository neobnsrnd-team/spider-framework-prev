package com.example.spider_admin.domain.article.controller;

import com.example.spider_admin.domain.article.dto.ArticleUserResponse;
import com.example.spider_admin.domain.article.service.ArticleUserService;
import com.example.spider_admin.global.dto.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자별 게시판 관리 REST Controller
 * 사용자의 게시글 열람/작성 이력 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/article-users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BOARD_MANAGEMENT:R')")
public class ArticleUserController {

    private final ArticleUserService articleUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ArticleUserResponse>>> getAllArticleUsers() {
        log.info("GET /api/article-users - Fetching all article users");
        List<ArticleUserResponse> articleUsers = articleUserService.getAllArticleUsers();
        return ResponseEntity.ok(ApiResponse.success(articleUsers));
    }

    @GetMapping("/{userId}/{boardId}/{articleSeq}")
    public ResponseEntity<ApiResponse<ArticleUserResponse>> getArticleUserById(
            @PathVariable String userId, @PathVariable String boardId, @PathVariable Long articleSeq) {
        log.info("GET /api/article-users/{}/{}/{} - Fetching article user", userId, boardId, articleSeq);
        ArticleUserResponse articleUser = articleUserService.getArticleUserById(userId, boardId, articleSeq);
        return ResponseEntity.ok(ApiResponse.success(articleUser));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ArticleUserResponse>>> getArticleUsersByUserId(@PathVariable String userId) {
        log.info("GET /api/article-users/user/{} - Fetching article users by user ID", userId);
        List<ArticleUserResponse> articleUsers = articleUserService.getArticleUsersByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(articleUsers));
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<ApiResponse<List<ArticleUserResponse>>> getArticleUsersByBoardId(
            @PathVariable String boardId) {
        log.info("GET /api/article-users/board/{} - Fetching article users by board ID", boardId);
        List<ArticleUserResponse> articleUsers = articleUserService.getArticleUsersByBoardId(boardId);
        return ResponseEntity.ok(ApiResponse.success(articleUsers));
    }

    @GetMapping("/user/{userId}/board/{boardId}")
    public ResponseEntity<ApiResponse<List<ArticleUserResponse>>> getArticleUsersByUserIdAndBoardId(
            @PathVariable String userId, @PathVariable String boardId) {
        log.info("GET /api/article-users/user/{}/board/{} - Fetching article users", userId, boardId);
        List<ArticleUserResponse> articleUsers = articleUserService.getArticleUsersByUserIdAndBoardId(userId, boardId);
        return ResponseEntity.ok(ApiResponse.success(articleUsers));
    }

    @GetMapping("/article/{articleSeq}")
    public ResponseEntity<ApiResponse<List<ArticleUserResponse>>> getArticleUsersByArticleSeq(
            @PathVariable Long articleSeq) {
        log.info("GET /api/article-users/article/{} - Fetching article users by article seq", articleSeq);
        List<ArticleUserResponse> articleUsers = articleUserService.getArticleUsersByArticleSeq(articleSeq);
        return ResponseEntity.ok(ApiResponse.success(articleUsers));
    }

    @GetMapping("/exists/{userId}/{boardId}/{articleSeq}")
    public ResponseEntity<ApiResponse<Boolean>> checkExists(
            @PathVariable String userId, @PathVariable String boardId, @PathVariable Long articleSeq) {
        log.info("GET /api/article-users/exists/{}/{}/{} - Checking existence", userId, boardId, articleSeq);
        boolean exists = articleUserService.existsArticleUser(userId, boardId, articleSeq);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    @GetMapping("/count/user/{userId}")
    public ResponseEntity<ApiResponse<Integer>> countByUserId(@PathVariable String userId) {
        log.info("GET /api/article-users/count/user/{} - Counting by user ID", userId);
        int count = articleUserService.countByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/board/{boardId}")
    public ResponseEntity<ApiResponse<Integer>> countByBoardId(@PathVariable String boardId) {
        log.info("GET /api/article-users/count/board/{} - Counting by board ID", boardId);
        int count = articleUserService.countByBoardId(boardId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/user/{userId}/board/{boardId}/article/{articleSeq}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<ArticleUserResponse>> upsertArticleUser(
            @PathVariable String userId, @PathVariable String boardId, @PathVariable Long articleSeq) {
        log.info(
                "PUT /api/article-users/user/{}/board/{}/article/{} - Upserting article user",
                userId,
                boardId,
                articleSeq);
        ArticleUserResponse articleUser = articleUserService.createArticleUser(userId, boardId, articleSeq);
        return ResponseEntity.ok(ApiResponse.success("사용자별 게시글 정보가 저장되었습니다", articleUser));
    }

    @DeleteMapping("/{userId}/{boardId}/{articleSeq}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> deleteArticleUser(
            @PathVariable String userId, @PathVariable String boardId, @PathVariable Long articleSeq) {
        log.info("DELETE /api/article-users/{}/{}/{} - Deleting article user", userId, boardId, articleSeq);
        articleUserService.deleteArticleUser(userId, boardId, articleSeq);
        return ResponseEntity.ok(ApiResponse.success("사용자별 게시글 정보가 삭제되었습니다", null));
    }

    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> deleteArticleUsersByUserId(@PathVariable String userId) {
        log.info("DELETE /api/article-users/user/{} - Deleting all article users for user", userId);
        articleUserService.deleteArticleUsersByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자의 모든 게시글 정보가 삭제되었습니다", null));
    }

    @DeleteMapping("/board/{boardId}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> deleteArticleUsersByBoardId(@PathVariable String boardId) {
        log.info("DELETE /api/article-users/board/{} - Deleting all article users for board", boardId);
        articleUserService.deleteArticleUsersByBoardId(boardId);
        return ResponseEntity.ok(ApiResponse.success("게시판의 모든 사용자 정보가 삭제되었습니다", null));
    }

    @DeleteMapping("/article/{articleSeq}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> deleteArticleUsersByArticleSeq(@PathVariable Long articleSeq) {
        log.info("DELETE /api/article-users/article/{} - Deleting all article users for article", articleSeq);
        articleUserService.deleteArticleUsersByArticleSeq(articleSeq);
        return ResponseEntity.ok(ApiResponse.success("게시글의 모든 사용자 정보가 삭제되었습니다", null));
    }
}

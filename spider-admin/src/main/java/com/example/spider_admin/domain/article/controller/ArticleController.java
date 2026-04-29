package com.example.spider_admin.domain.article.controller;

import com.example.spider_admin.domain.article.dto.ArticleCreateRequest;
import com.example.spider_admin.domain.article.dto.ArticleDetailResponse;
import com.example.spider_admin.domain.article.dto.ArticleListResponse;
import com.example.spider_admin.domain.article.dto.ArticleResponse;
import com.example.spider_admin.domain.article.dto.ArticleUpdateRequest;
import com.example.spider_admin.domain.article.dto.FileDownloadResponse;
import com.example.spider_admin.domain.article.service.ArticleService;
import com.example.spider_admin.global.dto.ApiResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.util.FileUtil;
import com.example.spider_admin.global.util.SecurityUtil;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 게시글 관리 REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('BOARD_MANAGEMENT:R')")
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping("/board/{boardId}")
    public ResponseEntity<ApiResponse<List<ArticleListResponse>>> getArticlesByBoardId(@PathVariable String boardId) {
        log.info("GET /api/articles/board/{} - Fetching articles by board ID", boardId);
        List<ArticleListResponse> articles = articleService.getArticlesByBoardId(boardId);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }

    @GetMapping("/board/{boardId}/page")
    public ResponseEntity<ApiResponse<PageResponse<ArticleListResponse>>> getArticlesWithPagination(
            @PathVariable String boardId,
            @RequestParam(required = false) String categorySeq,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info(
                "GET /api/articles/board/{}/page - categorySeq: {}, searchField: {}, keyword: {}, page: {}, size: {}",
                boardId,
                categorySeq,
                searchField,
                keyword,
                page,
                size);
        PageRequest pageRequest =
                PageRequest.builder().page(page - 1).size(size).build();
        PageResponse<ArticleListResponse> response = articleService.getArticlesByCategoryAndSearch(
                boardId, categorySeq, searchField, keyword, sortBy, sortDirection, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{articleSeq}")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> getArticleById(@PathVariable Long articleSeq) {
        log.info("GET /api/articles/{} - Fetching article by ID", articleSeq);
        ArticleDetailResponse article = articleService.getArticleById(articleSeq);
        return ResponseEntity.ok(ApiResponse.success(article));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticle(
            @Valid @RequestBody ArticleCreateRequest requestDTO) {
        log.info("POST /api/articles - Creating new article in board: {}", requestDTO.getBoardId());
        String userId = SecurityUtil.getCurrentUserIdOrSystem();
        String userName = SecurityUtil.getCurrentUsernameOrSystem();
        ArticleResponse createdArticle = articleService.createArticle(requestDTO, userId, userName);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("게시글이 등록되었습니다", createdArticle));
    }

    @PutMapping("/{articleSeq}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(
            @PathVariable Long articleSeq, @Valid @RequestBody ArticleUpdateRequest requestDTO) {
        log.info("PUT /api/articles/{} - Updating article", articleSeq);
        String userId = SecurityUtil.getCurrentUserIdOrSystem();
        ArticleResponse updatedArticle = articleService.updateArticle(articleSeq, requestDTO, userId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다", updatedArticle));
    }

    @DeleteMapping("/{articleSeq}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable Long articleSeq) {
        log.info("DELETE /api/articles/{} - Deleting article", articleSeq);
        articleService.deleteArticle(articleSeq);
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다", null));
    }

    @PostMapping("/{articleSeq}/download/{fileNo}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> incrementDownloadCnt(
            @PathVariable Long articleSeq, @PathVariable int fileNo) {
        log.info("POST /api/articles/{}/download/{} - Incrementing download count", articleSeq, fileNo);
        articleService.incrementDownloadCnt(articleSeq, fileNo);
        return ResponseEntity.ok(ApiResponse.success("다운로드 카운트가 증가되었습니다", null));
    }

    // ==================== 파일 첨부 관련 API ====================

    @PostMapping(value = "/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticleWithFiles(
            @Valid @RequestPart("article") ArticleCreateRequest requestDTO,
            @RequestPart(value = "file1", required = false) MultipartFile file1,
            @RequestPart(value = "file2", required = false) MultipartFile file2,
            @RequestPart(value = "file3", required = false) MultipartFile file3) {

        log.info("POST /api/articles/with-files - Creating article with files in board: {}", requestDTO.getBoardId());
        String userId = SecurityUtil.getCurrentUserIdOrSystem();
        String userName = SecurityUtil.getCurrentUsernameOrSystem();

        ArticleResponse createdArticle =
                articleService.createArticleWithFiles(requestDTO, userId, userName, file1, file2, file3);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("게시글이 등록되었습니다", createdArticle));
    }

    @PutMapping(value = "/{articleSeq}/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticleWithFiles(
            @PathVariable Long articleSeq,
            @Valid @RequestPart("article") ArticleUpdateRequest requestDTO,
            @RequestPart(value = "file1", required = false) MultipartFile file1,
            @RequestPart(value = "file2", required = false) MultipartFile file2,
            @RequestPart(value = "file3", required = false) MultipartFile file3) {

        log.info("PUT /api/articles/{}/with-files - Updating article with files", articleSeq);
        String userId = SecurityUtil.getCurrentUserIdOrSystem();

        ArticleResponse updatedArticle =
                articleService.updateArticleWithFiles(articleSeq, requestDTO, userId, file1, file2, file3);

        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다", updatedArticle));
    }

    @GetMapping("/{articleSeq}/files/{fileNo}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long articleSeq, @PathVariable int fileNo) {

        log.info("GET /api/articles/{}/files/{} - Downloading file", articleSeq, fileNo);

        FileDownloadResponse fileInfo = articleService.getFileForDownload(articleSeq, fileNo);
        Resource resource = FileUtil.downloadFile(fileInfo.getFilePath());

        articleService.incrementDownloadCnt(articleSeq, fileNo);

        String originalFileName = fileInfo.getOriginalFileName();
        String encodedFileName =
                URLEncoder.encode(originalFileName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @DeleteMapping("/{articleSeq}/files/{fileNo}")
    @PreAuthorize("hasAuthority('BOARD_MANAGEMENT:W')")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long articleSeq, @PathVariable int fileNo) {

        log.info("DELETE /api/articles/{}/files/{} - Deleting file", articleSeq, fileNo);
        String userId = SecurityUtil.getCurrentUserIdOrSystem();

        articleService.deleteFile(articleSeq, fileNo, userId);

        return ResponseEntity.ok(ApiResponse.success("파일이 삭제되었습니다", null));
    }
}

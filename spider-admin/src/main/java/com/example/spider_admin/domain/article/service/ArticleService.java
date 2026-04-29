package com.example.spider_admin.domain.article.service;

import com.example.spider_admin.domain.article.constant.ArticleConstants;
import com.example.spider_admin.domain.article.dto.ArticleCreateRequest;
import com.example.spider_admin.domain.article.dto.ArticleDetailResponse;
import com.example.spider_admin.domain.article.dto.ArticleListResponse;
import com.example.spider_admin.domain.article.dto.ArticleResponse;
import com.example.spider_admin.domain.article.dto.ArticleUpdateRequest;
import com.example.spider_admin.domain.article.dto.FileDownloadResponse;
import com.example.spider_admin.domain.article.mapper.ArticleMapper;
import com.example.spider_admin.domain.board.dto.BoardCategoryResponse;
import com.example.spider_admin.domain.board.dto.BoardResponse;
import com.example.spider_admin.domain.board.service.BoardCategoryService;
import com.example.spider_admin.domain.board.service.BoardService;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import com.example.spider_admin.global.util.FileUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 게시글 관리 Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleMapper articleMapper;
    private final BoardService boardService;
    private final BoardCategoryService boardCategoryService;

    public List<ArticleListResponse> getArticlesByBoardId(String boardId) {
        return articleMapper.findByBoardId(boardId);
    }

    public PageResponse<ArticleListResponse> getArticlesByBoardIdWithPaging(String boardId, PageRequest pageRequest) {
        int page = pageRequest.getPage();
        int size = pageRequest.getSize();
        int offset = pageRequest.getOffset();

        long total = articleMapper.countByBoardId(boardId);
        List<ArticleListResponse> dtos = articleMapper.findByBoardIdWithPaging(boardId, offset, size);

        return PageResponse.of(dtos, total, page, size);
    }

    @Transactional
    public ArticleDetailResponse getArticleById(Long articleSeq) {
        if (articleMapper.countById(articleSeq) == 0) {
            throw new NotFoundException("articleSeq: " + articleSeq);
        }

        // 조회수 먼저 증가
        articleMapper.incrementReadCnt(articleSeq);

        // 증가된 값으로 조회
        ArticleResponse article = articleMapper.selectResponseById(articleSeq);

        // boardName 조회 (cross-domain)
        BoardResponse board = boardService.getBoardById(article.getBoardId());
        String boardName = board.getBoardName();

        // categoryName 조회 (cross-domain)
        String categoryName = null;
        if (article.getCategorySeq() != null) {
            BoardCategoryResponse category =
                    boardCategoryService.getCategoryById(article.getBoardId(), article.getCategorySeq());
            categoryName = category.getCategoryName();
        }

        ArticleDetailResponse response = toDetailResponse(article, boardName, categoryName);

        List<ArticleListResponse> replies = articleMapper.selectRepliesByRefArticleSeq(articleSeq).stream()
                .map(this::toListResponse)
                .toList();
        response.setReplies(replies);

        return response;
    }

    @Transactional
    public ArticleResponse createArticle(ArticleCreateRequest requestDTO, String userId, String userName) {
        log.info("Creating article - boardId: {}, userId: {}, userName: {}", requestDTO.getBoardId(), userId, userName);
        return doCreateArticle(requestDTO, userId, userName);
    }

    private ArticleResponse doCreateArticle(ArticleCreateRequest requestDTO, String userId, String userName) {
        String now = AuditUtil.now();
        int step = requestDTO.getRefArticleSeq() != null ? ArticleConstants.STEP_REPLY : ArticleConstants.STEP_ORIGINAL;
        int position = ArticleConstants.INITIAL_POSITION;

        if (requestDTO.getRefArticleSeq() != null) {
            int replyCount = articleMapper.countRepliesByRefArticleSeq(requestDTO.getRefArticleSeq());
            position = replyCount + 1;
        }

        Long articleSeq = articleMapper.selectNextArticleSeq();
        articleMapper.insertArticle(articleSeq, requestDTO, step, position, userId, userName, now, now, userId);

        log.info("Article inserted with seq: {}", articleSeq);
        return articleMapper.selectResponseById(articleSeq);
    }

    @Transactional
    public ArticleResponse updateArticle(Long articleSeq, ArticleUpdateRequest requestDTO, String userId) {
        if (articleMapper.countById(articleSeq) == 0) {
            throw new NotFoundException("articleSeq: " + articleSeq);
        }

        return doUpdateArticle(articleSeq, requestDTO, userId);
    }

    private ArticleResponse doUpdateArticle(Long articleSeq, ArticleUpdateRequest requestDTO, String userId) {
        String now = AuditUtil.now();
        articleMapper.updateArticle(articleSeq, requestDTO, now, userId);
        return articleMapper.selectResponseById(articleSeq);
    }

    @Transactional
    public void deleteArticle(Long articleSeq) {
        ArticleResponse article = articleMapper.selectResponseById(articleSeq);
        if (article == null) {
            throw new NotFoundException("articleSeq: " + articleSeq);
        }

        // 원본 게시글의 첨부파일 삭제
        FileUtil.deleteFile(article.getAttachFilePath1());
        FileUtil.deleteFile(article.getAttachFilePath2());
        FileUtil.deleteFile(article.getAttachFilePath3());

        // 답글의 첨부파일도 삭제
        List<ArticleResponse> replies = articleMapper.selectRepliesByRefArticleSeq(articleSeq);
        for (ArticleResponse reply : replies) {
            FileUtil.deleteFile(reply.getAttachFilePath1());
            FileUtil.deleteFile(reply.getAttachFilePath2());
            FileUtil.deleteFile(reply.getAttachFilePath3());
        }

        // 답글을 한 번에 삭제 (N+1 문제 해결)
        articleMapper.deleteRepliesByRefArticleSeq(articleSeq);

        // 원본 게시글 삭제
        articleMapper.deleteArticleById(articleSeq);
    }

    @Transactional
    public void incrementDownloadCnt(Long articleSeq, int fileNo) {
        articleMapper.incrementDownloadCnt(articleSeq, fileNo);
    }

    public PageResponse<ArticleListResponse> getArticlesByCategoryAndSearch(
            String boardId,
            String categorySeq,
            String searchField,
            String keyword,
            String sortBy,
            String sortDirection,
            PageRequest pageRequest) {
        int page = pageRequest.getPage();
        int size = pageRequest.getSize();
        int offset = pageRequest.getOffset();

        long total = articleMapper.countByBoardIdAndCategory(boardId, categorySeq, searchField, keyword);
        List<ArticleListResponse> dtos = articleMapper.findByBoardIdAndCategory(
                boardId, categorySeq, searchField, keyword, sortBy, sortDirection, offset, size);

        return PageResponse.of(dtos, total, page, size);
    }

    // ==================== 파일 첨부 관련 ====================

    @Transactional
    public ArticleResponse createArticleWithFiles(
            ArticleCreateRequest requestDTO,
            String userId,
            String userName,
            MultipartFile file1,
            MultipartFile file2,
            MultipartFile file3) {
        log.info("Creating article with files - boardId: {}", requestDTO.getBoardId());

        // 1. 먼저 게시글 생성 (articleSeq 확보)
        ArticleResponse createdArticle = doCreateArticle(requestDTO, userId, userName);
        Long articleSeq = createdArticle.getArticleSeq();
        String boardId = requestDTO.getBoardId();

        // 2. 파일 업로드 처리
        boolean hasFileUpdate = false;
        String filePath1 = null;
        String filePath2 = null;
        String filePath3 = null;

        if (file1 != null && !file1.isEmpty()) {
            filePath1 = FileUtil.uploadFile(file1, boardId, articleSeq);
            hasFileUpdate = true;
        }
        if (file2 != null && !file2.isEmpty()) {
            filePath2 = FileUtil.uploadFile(file2, boardId, articleSeq);
            hasFileUpdate = true;
        }
        if (file3 != null && !file3.isEmpty()) {
            filePath3 = FileUtil.uploadFile(file3, boardId, articleSeq);
            hasFileUpdate = true;
        }

        // 3. 파일 경로 업데이트
        if (hasFileUpdate) {
            String now = AuditUtil.now();
            articleMapper.updateFilePaths(articleSeq, filePath1, filePath2, filePath3, now, userId);
            return articleMapper.selectResponseById(articleSeq);
        }

        return createdArticle;
    }

    @Transactional
    public ArticleResponse updateArticleWithFiles(
            Long articleSeq,
            ArticleUpdateRequest requestDTO,
            String userId,
            MultipartFile file1,
            MultipartFile file2,
            MultipartFile file3) {
        log.info("Updating article {} with files", articleSeq);

        ArticleResponse existingArticle = articleMapper.selectResponseById(articleSeq);
        if (existingArticle == null) {
            throw new NotFoundException("articleSeq: " + articleSeq);
        }

        String boardId = existingArticle.getBoardId();

        // 새 파일이 업로드되면 기존 파일 삭제 후 새 파일 저장
        if (file1 != null && !file1.isEmpty()) {
            FileUtil.deleteFile(existingArticle.getAttachFilePath1());
            String storedPath = FileUtil.uploadFile(file1, boardId, articleSeq);
            requestDTO.setAttachFilePath1(storedPath);
        }
        if (file2 != null && !file2.isEmpty()) {
            FileUtil.deleteFile(existingArticle.getAttachFilePath2());
            String storedPath = FileUtil.uploadFile(file2, boardId, articleSeq);
            requestDTO.setAttachFilePath2(storedPath);
        }
        if (file3 != null && !file3.isEmpty()) {
            FileUtil.deleteFile(existingArticle.getAttachFilePath3());
            String storedPath = FileUtil.uploadFile(file3, boardId, articleSeq);
            requestDTO.setAttachFilePath3(storedPath);
        }

        return doUpdateArticle(articleSeq, requestDTO, userId);
    }

    public FileDownloadResponse getFileForDownload(Long articleSeq, int fileNo) {
        ArticleResponse article = articleMapper.selectResponseById(articleSeq);
        if (article == null) {
            throw new NotFoundException("articleSeq: " + articleSeq);
        }

        String filePath = getFilePathByNumber(article, fileNo);

        if (filePath == null || filePath.isBlank()) {
            throw new NotFoundException("articleSeq=" + articleSeq + ", fileNo=" + fileNo);
        }

        String originalFileName = FileUtil.extractOriginalFileName(filePath);

        return FileDownloadResponse.builder()
                .filePath(filePath)
                .originalFileName(originalFileName)
                .build();
    }

    @Transactional
    public void deleteFile(Long articleSeq, int fileNo, String userId) {
        ArticleResponse article = articleMapper.selectResponseById(articleSeq);
        if (article == null) {
            throw new NotFoundException("articleSeq: " + articleSeq);
        }

        String filePath = getFilePathByNumber(article, fileNo);

        if (filePath == null || filePath.isBlank()) {
            throw new NotFoundException("articleSeq=" + articleSeq + ", fileNo=" + fileNo);
        }

        // 물리적 파일 삭제
        FileUtil.deleteFile(filePath);

        // DB에서 파일 경로 제거
        String now = AuditUtil.now();
        articleMapper.clearFilePath(articleSeq, fileNo, now, userId);

        log.info("File deleted - articleSeq: {}, fileNo: {}", articleSeq, fileNo);
    }

    /**
     * 파일 번호로 파일 경로 조회
     */
    private String getFilePathByNumber(ArticleResponse article, int fileNo) {
        return switch (fileNo) {
            case 1 -> article.getAttachFilePath1();
            case 2 -> article.getAttachFilePath2();
            case 3 -> article.getAttachFilePath3();
            default -> throw new InvalidInputException("fileNo: " + fileNo + " (valid: 1-3)");
        };
    }

    /**
     * ArticleResponse -> ArticleDetailResponse 변환
     */
    private ArticleDetailResponse toDetailResponse(ArticleResponse dto, String boardName, String categoryName) {
        return ArticleDetailResponse.builder()
                .articleSeq(dto.getArticleSeq())
                .boardId(dto.getBoardId())
                .boardName(boardName)
                .categorySeq(dto.getCategorySeq())
                .categoryName(categoryName)
                .topYn(dto.getTopYn())
                .refArticleSeq(dto.getRefArticleSeq())
                .step(dto.getStep())
                .title(dto.getTitle())
                .writerId(dto.getWriterId())
                .writerName(dto.getWriterName())
                .readCnt(dto.getReadCnt())
                .attachFilePath1(dto.getAttachFilePath1())
                .downloadCnt1(dto.getDownloadCnt1())
                .attachFilePath2(dto.getAttachFilePath2())
                .downloadCnt2(dto.getDownloadCnt2())
                .attachFilePath3(dto.getAttachFilePath3())
                .downloadCnt3(dto.getDownloadCnt3())
                .registDtime(dto.getRegistDtime())
                .lastUpdateDtime(dto.getLastUpdateDtime())
                .content(dto.getContent())
                .build();
    }

    /**
     * ArticleResponse -> ArticleListResponse 변환 (답글 목록용)
     */
    private ArticleListResponse toListResponse(ArticleResponse dto) {
        boolean hasAttachment = dto.getAttachFilePath1() != null
                || dto.getAttachFilePath2() != null
                || dto.getAttachFilePath3() != null;
        return ArticleListResponse.builder()
                .articleSeq(dto.getArticleSeq())
                .boardId(dto.getBoardId())
                .categorySeq(dto.getCategorySeq())
                .topYn(dto.getTopYn())
                .step(dto.getStep())
                .title(dto.getTitle())
                .writerName(dto.getWriterName())
                .readCnt(dto.getReadCnt())
                .hasAttachment(hasAttachment)
                .registDtime(dto.getRegistDtime())
                .build();
    }
}

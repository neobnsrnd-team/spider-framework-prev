package com.example.spideradmin.domain.article.mapper;

import com.example.spideradmin.domain.article.dto.ArticleCreateRequest;
import com.example.spideradmin.domain.article.dto.ArticleListResponse;
import com.example.spideradmin.domain.article.dto.ArticleResponse;
import com.example.spideradmin.domain.article.dto.ArticleUpdateRequest;
import com.example.spideradmin.domain.article.dto.ArticleUserResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 게시글 Mapper (Command + Query)
 */
public interface ArticleMapper {

    // ==================== Read ====================

    ArticleResponse selectResponseById(Long articleSeq);

    List<ArticleResponse> selectRepliesByRefArticleSeq(Long refArticleSeq);

    int countById(Long articleSeq);

    // ==================== Create ====================

    void insertArticle(
            @Param("articleSeq") Long articleSeq,
            @Param("dto") ArticleCreateRequest dto,
            @Param("step") int step,
            @Param("position") int position,
            @Param("writerId") String writerId,
            @Param("writerName") String writerName,
            @Param("registDtime") String registDtime,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    // ==================== Update ====================

    void updateArticle(
            @Param("articleSeq") Long articleSeq,
            @Param("dto") ArticleUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void updateFilePaths(
            @Param("articleSeq") Long articleSeq,
            @Param("attachFilePath1") String attachFilePath1,
            @Param("attachFilePath2") String attachFilePath2,
            @Param("attachFilePath3") String attachFilePath3,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void incrementReadCnt(Long articleSeq);

    void incrementDownloadCnt(@Param("articleSeq") Long articleSeq, @Param("fileNo") int fileNo);

    void clearFilePath(
            @Param("articleSeq") Long articleSeq,
            @Param("fileNo") int fileNo,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    // ==================== Delete ====================

    void deleteArticleById(Long articleSeq);

    void deleteRepliesByRefArticleSeq(Long refArticleSeq);

    // ==================== Query ====================

    /**
     * 전체 게시글 목록 조회
     */
    List<ArticleListResponse> findAll();

    /**
     * 게시판별 게시글 목록 조회
     */
    List<ArticleListResponse> findByBoardId(String boardId);

    /**
     * 게시판별 게시글 목록 조회 (ROW_NUMBER 페이징)
     */
    List<ArticleListResponse> findByBoardIdWithPaging(
            @Param("boardId") String boardId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 게시판별 게시글 건수 조회
     */
    long countByBoardId(@Param("boardId") String boardId);

    /**
     * 카테고리 + 검색 조건 통합 조회 (ROW_NUMBER 페이징)
     */
    List<ArticleListResponse> findByBoardIdAndCategory(
            @Param("boardId") String boardId,
            @Param("categorySeq") String categorySeq,
            @Param("searchField") String searchField,
            @Param("keyword") String keyword,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 카테고리 + 검색 조건 건수 조회
     */
    long countByBoardIdAndCategory(
            @Param("boardId") String boardId,
            @Param("categorySeq") String categorySeq,
            @Param("searchField") String searchField,
            @Param("keyword") String keyword);

    /**
     * 답글 수 조회
     */
    int countRepliesByRefArticleSeq(Long refArticleSeq);

    /**
     * 다음 ARTICLE_SEQ 시퀀스 값 조회
     */
    Long selectNextArticleSeq();

    // ==================== Article User ====================

    ArticleUserResponse selectArticleUserById(
            @Param("userId") String userId, @Param("boardId") String boardId, @Param("articleSeq") Long articleSeq);

    List<ArticleUserResponse> selectAllArticleUsers();

    void insertArticleUser(
            @Param("userId") String userId,
            @Param("boardId") String boardId,
            @Param("articleSeq") Long articleSeq,
            @Param("lastUpdateDtime") String lastUpdateDtime);

    void updateArticleUser(
            @Param("userId") String userId,
            @Param("boardId") String boardId,
            @Param("articleSeq") Long articleSeq,
            @Param("lastUpdateDtime") String lastUpdateDtime);

    void deleteArticleUserById(
            @Param("userId") String userId, @Param("boardId") String boardId, @Param("articleSeq") Long articleSeq);

    List<ArticleUserResponse> selectArticleUsersByUserId(String userId);

    List<ArticleUserResponse> selectArticleUsersByBoardId(String boardId);

    List<ArticleUserResponse> selectArticleUsersByUserIdAndBoardId(
            @Param("userId") String userId, @Param("boardId") String boardId);

    List<ArticleUserResponse> selectArticleUsersByArticleSeq(Long articleSeq);

    void deleteArticleUsersByUserId(String userId);

    void deleteArticleUsersByBoardId(String boardId);

    void deleteArticleUsersByArticleSeq(Long articleSeq);

    int countArticleUsersByUserId(String userId);

    int countArticleUsersByBoardId(String boardId);

    boolean existsArticleUserById(
            @Param("userId") String userId, @Param("boardId") String boardId, @Param("articleSeq") Long articleSeq);
}

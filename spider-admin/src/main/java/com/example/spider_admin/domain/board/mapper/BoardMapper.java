package com.example.spider_admin.domain.board.mapper;

import com.example.spider_admin.domain.board.dto.BoardAuthResponse;
import com.example.spider_admin.domain.board.dto.BoardCategoryResponse;
import com.example.spider_admin.domain.board.dto.BoardCreateRequest;
import com.example.spider_admin.domain.board.dto.BoardResponse;
import com.example.spider_admin.domain.board.dto.BoardSearchRequest;
import com.example.spider_admin.domain.board.dto.BoardUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BoardMapper {

    BoardResponse selectResponseById(String boardId);

    List<BoardResponse> selectAll();

    void insertBoard(
            @Param("dto") BoardCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void updateBoard(
            @Param("boardId") String boardId,
            @Param("dto") BoardUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteBoardById(String boardId);

    List<BoardResponse> selectByBoardType(String boardType);

    int countById(String boardId);

    /**
     * 전체 게시판 목록 조회 (ROW_NUMBER 페이징)
     */
    List<BoardResponse> findAllWithPaging(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 전체 게시판 건수 조회
     */
    long countAll();

    /**
     * 검색 조건으로 게시판 목록 조회 (ROW_NUMBER 페이징)
     */
    List<BoardResponse> findAllWithSearch(
            @Param("search") BoardSearchRequest search, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 검색 조건 게시판 건수 조회
     */
    long countWithSearch(@Param("search") BoardSearchRequest search);

    // ==================== Board Auth ====================

    BoardAuthResponse selectBoardAuthById(@Param("userId") String userId, @Param("boardId") String boardId);

    List<BoardAuthResponse> selectAllBoardAuth();

    void insertBoardAuth(
            @Param("userId") String userId,
            @Param("boardId") String boardId,
            @Param("authCode") String authCode,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void updateBoardAuth(
            @Param("userId") String userId,
            @Param("boardId") String boardId,
            @Param("authCode") String authCode,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteBoardAuthById(@Param("userId") String userId, @Param("boardId") String boardId);

    List<BoardAuthResponse> selectBoardAuthByUserId(String userId);

    List<BoardAuthResponse> selectBoardAuthByBoardId(String boardId);

    String selectAuthCode(@Param("userId") String userId, @Param("boardId") String boardId);

    void deleteBoardAuthByBoardId(String boardId);

    int countBoardAuthById(@Param("userId") String userId, @Param("boardId") String boardId);

    List<BoardAuthResponse> selectBoardAuthWithFilter(
            @Param("boardId") String boardId,
            @Param("userKeyword") String userKeyword,
            @Param("authCode") String authCode,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countBoardAuthWithFilter(
            @Param("boardId") String boardId,
            @Param("userKeyword") String userKeyword,
            @Param("authCode") String authCode);

    // ==================== Board Category ====================

    BoardCategoryResponse selectCategoryById(
            @Param("boardId") String boardId, @Param("categorySeq") String categorySeq);

    List<BoardCategoryResponse> selectAllCategories();

    void insertCategory(
            @Param("boardId") String boardId,
            @Param("categorySeq") String categorySeq,
            @Param("categoryName") String categoryName);

    void updateCategory(
            @Param("boardId") String boardId,
            @Param("categorySeq") String categorySeq,
            @Param("categoryName") String categoryName);

    void deleteCategoryById(@Param("boardId") String boardId, @Param("categorySeq") String categorySeq);

    List<BoardCategoryResponse> selectCategoriesByBoardId(String boardId);

    String selectNextCategorySeq(String boardId);

    int countCategoriesByBoardId(String boardId);

    // ==================== Cascade Delete (for Board deletion) ====================

    void deleteArticleUsersByBoardId(String boardId);

    void deleteArticlesByBoardId(String boardId);

    void deleteCategoriesByBoardId(String boardId);
}

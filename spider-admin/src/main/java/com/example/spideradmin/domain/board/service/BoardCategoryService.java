package com.example.spideradmin.domain.board.service;

import com.example.spideradmin.domain.board.dto.BoardCategoryResponse;
import com.example.spideradmin.domain.board.mapper.BoardMapper;
import com.example.spideradmin.global.exception.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 카테고리 관리 Service 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCategoryService {

    private final BoardMapper boardMapper;

    public List<BoardCategoryResponse> getCategoriesByBoardId(String boardId) {
        return boardMapper.selectCategoriesByBoardId(boardId);
    }

    public BoardCategoryResponse getCategoryById(String boardId, String categorySeq) {
        BoardCategoryResponse category = boardMapper.selectCategoryById(boardId, categorySeq);
        if (category == null) {
            throw new NotFoundException("boardId: " + boardId + ", categorySeq: " + categorySeq);
        }

        return category;
    }

    @Transactional
    public BoardCategoryResponse createCategory(String boardId, String categoryName) {
        String nextSeq = boardMapper.selectNextCategorySeq(boardId);
        boardMapper.insertCategory(boardId, nextSeq, categoryName);
        return boardMapper.selectCategoryById(boardId, nextSeq);
    }

    @Transactional
    public BoardCategoryResponse updateCategory(String boardId, String categorySeq, String categoryName) {
        BoardCategoryResponse existingCategory = boardMapper.selectCategoryById(boardId, categorySeq);
        if (existingCategory == null) {
            throw new NotFoundException("boardId: " + boardId + ", categorySeq: " + categorySeq);
        }
        boardMapper.updateCategory(boardId, categorySeq, categoryName);
        return boardMapper.selectCategoryById(boardId, categorySeq);
    }

    @Transactional
    public void deleteCategory(String boardId, String categorySeq) {
        BoardCategoryResponse category = boardMapper.selectCategoryById(boardId, categorySeq);
        if (category == null) {
            throw new NotFoundException("boardId: " + boardId + ", categorySeq: " + categorySeq);
        }

        boardMapper.deleteCategoryById(boardId, categorySeq);
    }
}

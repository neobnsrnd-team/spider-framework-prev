package com.example.spider_admin.domain.article.service;

import com.example.spider_admin.domain.article.dto.ArticleUserResponse;
import com.example.spider_admin.domain.article.mapper.ArticleMapper;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자별 게시판 관리 Service 구현체
 * 사용자의 게시글 열람/작성 이력 관리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleUserService {

    private final ArticleMapper articleMapper;

    public List<ArticleUserResponse> getAllArticleUsers() {
        return articleMapper.selectAllArticleUsers();
    }

    public ArticleUserResponse getArticleUserById(String userId, String boardId, Long articleSeq) {
        ArticleUserResponse articleUser = articleMapper.selectArticleUserById(userId, boardId, articleSeq);
        if (articleUser == null) {
            throw new NotFoundException("userId: " + userId + ", boardId: " + boardId + ", articleSeq: " + articleSeq);
        }

        return articleUser;
    }

    public List<ArticleUserResponse> getArticleUsersByUserId(String userId) {
        return articleMapper.selectArticleUsersByUserId(userId);
    }

    public List<ArticleUserResponse> getArticleUsersByBoardId(String boardId) {
        return articleMapper.selectArticleUsersByBoardId(boardId);
    }

    public List<ArticleUserResponse> getArticleUsersByUserIdAndBoardId(String userId, String boardId) {
        return articleMapper.selectArticleUsersByUserIdAndBoardId(userId, boardId);
    }

    public List<ArticleUserResponse> getArticleUsersByArticleSeq(Long articleSeq) {
        return articleMapper.selectArticleUsersByArticleSeq(articleSeq);
    }

    public boolean existsArticleUser(String userId, String boardId, Long articleSeq) {
        return articleMapper.existsArticleUserById(userId, boardId, articleSeq);
    }

    public int countByUserId(String userId) {
        return articleMapper.countArticleUsersByUserId(userId);
    }

    public int countByBoardId(String boardId) {
        return articleMapper.countArticleUsersByBoardId(boardId);
    }

    @Transactional
    public ArticleUserResponse createArticleUser(String userId, String boardId, Long articleSeq) {
        if (articleMapper.existsArticleUserById(userId, boardId, articleSeq)) {
            return doUpdateArticleUser(userId, boardId, articleSeq);
        }

        String now = AuditUtil.now();
        articleMapper.insertArticleUser(userId, boardId, articleSeq, now);

        return articleMapper.selectArticleUserById(userId, boardId, articleSeq);
    }

    @Transactional
    public ArticleUserResponse updateArticleUser(String userId, String boardId, Long articleSeq) {

        if (articleMapper.selectArticleUserById(userId, boardId, articleSeq) == null) {
            throw new NotFoundException("userId: " + userId + ", boardId: " + boardId + ", articleSeq: " + articleSeq);
        }

        return doUpdateArticleUser(userId, boardId, articleSeq);
    }

    private ArticleUserResponse doUpdateArticleUser(String userId, String boardId, Long articleSeq) {
        String now = AuditUtil.now();
        articleMapper.updateArticleUser(userId, boardId, articleSeq, now);
        return articleMapper.selectArticleUserById(userId, boardId, articleSeq);
    }

    @Transactional
    public void deleteArticleUser(String userId, String boardId, Long articleSeq) {
        ArticleUserResponse articleUser = articleMapper.selectArticleUserById(userId, boardId, articleSeq);
        if (articleUser == null) {
            throw new NotFoundException("userId: " + userId + ", boardId: " + boardId + ", articleSeq: " + articleSeq);
        }

        articleMapper.deleteArticleUserById(userId, boardId, articleSeq);
    }

    @Transactional
    public void deleteArticleUsersByUserId(String userId) {
        articleMapper.deleteArticleUsersByUserId(userId);
    }

    @Transactional
    public void deleteArticleUsersByBoardId(String boardId) {
        articleMapper.deleteArticleUsersByBoardId(boardId);
    }

    @Transactional
    public void deleteArticleUsersByArticleSeq(Long articleSeq) {
        articleMapper.deleteArticleUsersByArticleSeq(articleSeq);
    }
}

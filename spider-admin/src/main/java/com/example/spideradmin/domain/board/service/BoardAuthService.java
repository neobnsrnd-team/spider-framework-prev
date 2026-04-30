package com.example.spideradmin.domain.board.service;

import com.example.spideradmin.domain.board.dto.BoardAuthResponse;
import com.example.spideradmin.domain.board.mapper.BoardMapper;
import com.example.spideradmin.global.common.enums.AuthCode;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 권한 관리 Service 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardAuthService {

    private final BoardMapper boardMapper;

    public List<BoardAuthResponse> getAllAuthorities() {
        return boardMapper.selectAllBoardAuth();
    }

    public BoardAuthResponse getAuthById(String userId, String boardId) {
        BoardAuthResponse auth = boardMapper.selectBoardAuthById(userId, boardId);
        if (auth == null) {
            throw new NotFoundException("userId: " + userId + ", boardId: " + boardId);
        }

        return auth;
    }

    public List<BoardAuthResponse> getAuthsByUserId(String userId) {
        return boardMapper.selectBoardAuthByUserId(userId);
    }

    public List<BoardAuthResponse> getAuthsByBoardId(String boardId) {
        return boardMapper.selectBoardAuthByBoardId(boardId);
    }

    public String getAuthCode(String userId, String boardId) {
        return boardMapper.selectAuthCode(userId, boardId);
    }

    public boolean hasWritePermission(String userId, String boardId) {
        String authCode = boardMapper.selectAuthCode(userId, boardId);
        return AuthCode.WRITE.getCode().equals(authCode);
    }

    public boolean hasReadPermission(String userId, String boardId) {
        String authCode = boardMapper.selectAuthCode(userId, boardId);
        return authCode != null;
    }

    @Transactional
    public BoardAuthResponse createAuth(String userId, String boardId, String authCode, String updateUserId) {
        if (boardMapper.countBoardAuthById(userId, boardId) > 0) {
            throw new DuplicateException("userId: " + userId + ", boardId: " + boardId);
        }

        String now = AuditUtil.now();
        boardMapper.insertBoardAuth(userId, boardId, authCode, now, updateUserId);

        return boardMapper.selectBoardAuthById(userId, boardId);
    }

    @Transactional
    public BoardAuthResponse updateAuth(String userId, String boardId, String authCode, String updateUserId) {
        if (boardMapper.countBoardAuthById(userId, boardId) == 0) {
            throw new NotFoundException("userId: " + userId + ", boardId: " + boardId);
        }

        String now = AuditUtil.now();
        boardMapper.updateBoardAuth(userId, boardId, authCode, now, updateUserId);

        return boardMapper.selectBoardAuthById(userId, boardId);
    }

    @Transactional
    public void deleteAuth(String userId, String boardId) {
        if (boardMapper.countBoardAuthById(userId, boardId) == 0) {
            throw new NotFoundException("userId: " + userId + ", boardId: " + boardId);
        }

        boardMapper.deleteBoardAuthById(userId, boardId);
    }

    @Transactional
    public void deleteAuthsByBoardId(String boardId) {
        boardMapper.deleteBoardAuthByBoardId(boardId);
    }

    public PageResponse<BoardAuthResponse> getAuthoritiesWithFilter(
            String boardId,
            String userKeyword,
            String authCode,
            String sortBy,
            String sortDirection,
            PageRequest pageRequest) {
        int page = pageRequest.getPage();
        int size = pageRequest.getSize();
        int offset = pageRequest.getOffset();

        long total = boardMapper.countBoardAuthWithFilter(boardId, userKeyword, authCode);
        List<BoardAuthResponse> dtos = boardMapper.selectBoardAuthWithFilter(
                boardId, userKeyword, authCode, sortBy, sortDirection, offset, size);

        return PageResponse.of(dtos, total, page, size);
    }
}

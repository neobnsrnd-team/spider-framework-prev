package com.example.spider_admin.domain.board.service;

import com.example.spider_admin.domain.board.dto.BoardCreateRequest;
import com.example.spider_admin.domain.board.dto.BoardResponse;
import com.example.spider_admin.domain.board.dto.BoardSearchRequest;
import com.example.spider_admin.domain.board.dto.BoardUpdateRequest;
import com.example.spider_admin.domain.board.mapper.BoardMapper;
import com.example.spider_admin.domain.menu.dto.MenuCreateRequest;
import com.example.spider_admin.domain.menu.dto.MenuResponse;
import com.example.spider_admin.domain.menu.mapper.MenuMapper;
import com.example.spider_admin.domain.menu.mapper.UserMenuMapper;
import com.example.spider_admin.domain.user.mapper.UserMapper;
import com.example.spider_admin.global.aop.WorkListRecord;
import com.example.spider_admin.global.common.enums.AuthCode;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 관리 Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private static final String DEFAULT_PRIOR_MENU_ID = "BOARD_V2";
    private static final String DEFAULT_MENU_IMAGE = "fa-clipboard-list";
    private static final String DEFAULT_WEB_APP_ID = "admin";
    private static final String MENU_URL_PREFIX = "board/";
    private static final String MENU_ID_PREFIX = "FWKB_";

    private final BoardMapper boardMapper;
    private final MenuMapper menuMapper;
    private final UserMenuMapper userMenuMapper;
    private final UserMapper userMapper;

    public List<BoardResponse> getAllBoards() {
        return boardMapper.selectAll();
    }

    public PageResponse<BoardResponse> getBoards(PageRequest pageRequest) {
        int page = pageRequest.getPage();
        int size = pageRequest.getSize();
        int offset = pageRequest.getOffset();

        long total = boardMapper.countAll();
        List<BoardResponse> dtos = boardMapper.findAllWithPaging(offset, size);

        return PageResponse.of(dtos, total, page, size);
    }

    public PageResponse<BoardResponse> searchBoards(BoardSearchRequest searchRequest) {
        PageRequest pageRequest = searchRequest.toPageRequest();
        int page = pageRequest.getPage();
        int size = pageRequest.getSize();
        int offset = pageRequest.getOffset();

        long total = boardMapper.countWithSearch(searchRequest);
        List<BoardResponse> dtos = boardMapper.findAllWithSearch(searchRequest, offset, size);

        return PageResponse.of(dtos, total, page, size);
    }

    public BoardResponse getBoardById(String boardId) {
        BoardResponse board = boardMapper.selectResponseById(boardId);
        if (board == null) {
            throw new NotFoundException("boardId: " + boardId);
        }

        return board;
    }

    public List<BoardResponse> getBoardsByType(String boardType) {
        return boardMapper.selectByBoardType(boardType);
    }

    @Transactional
    @WorkListRecord(workId = "Board_Service", crudType = "C", pkExpression = "#requestDTO.boardId", workName = "게시판")
    public BoardResponse createBoard(BoardCreateRequest requestDTO, String userId) {
        if (boardMapper.countById(requestDTO.getBoardId()) > 0) {
            throw new DuplicateException("boardId: " + requestDTO.getBoardId());
        }

        String now = AuditUtil.now();

        boardMapper.insertBoard(requestDTO, now, userId);

        String adminId = requestDTO.getAdminId();

        // adminId 존재 여부 검증
        if (adminId != null && !adminId.isBlank() && userMapper.countByUserId(adminId) == 0) {
            throw new NotFoundException("adminId: " + adminId);
        }

        // 관리자에게 자동으로 W(쓰기) 권한 부여 (FWK_BOARD_AUTH)
        if (adminId != null && !adminId.isBlank()) {
            boardMapper.insertBoardAuth(adminId, requestDTO.getBoardId(), AuthCode.WRITE.getCode(), now, userId);
        }

        // 메뉴 자동 생성 (상위 메뉴: BOARD_V2 고정)
        String menuId = generateMenuId(requestDTO.getBoardId());
        int sortOrder = calculateNextSortOrder(DEFAULT_PRIOR_MENU_ID);

        MenuCreateRequest menuDTO = MenuCreateRequest.builder()
                .menuId(menuId)
                .menuName(requestDTO.getBoardName())
                .menuUrl(MENU_URL_PREFIX + requestDTO.getBoardId())
                .menuImage(DEFAULT_MENU_IMAGE)
                .priorMenuId(DEFAULT_PRIOR_MENU_ID)
                .sortOrder(sortOrder)
                .displayYn("Y")
                .useYn("Y")
                .webAppId(DEFAULT_WEB_APP_ID)
                .build();
        menuMapper.insert(menuDTO, now, userId);

        log.info(
                "게시판 메뉴 자동 생성: menuId={}, boardId={}, priorMenuId={}",
                menuId,
                requestDTO.getBoardId(),
                DEFAULT_PRIOR_MENU_ID);

        // 관리자에게 FWK_USER_MENU 권한 부여
        if (adminId != null && !adminId.isBlank()) {
            userMenuMapper.insert(adminId, menuId, AuthCode.WRITE.getCode(), 0, now, userId);
            log.info("게시판 메뉴 권한 자동 부여: userId={}, menuId={}", adminId, menuId);
        }

        return boardMapper.selectResponseById(requestDTO.getBoardId());
    }

    /**
     * 게시판 ID로 메뉴 ID 생성
     * 예: test_board -> FWKB_TEST_BOARD
     */
    private String generateMenuId(String boardId) {
        String upperBoardId = boardId.toUpperCase().replace("-", "_");
        return MENU_ID_PREFIX + upperBoardId;
    }

    /**
     * 상위 메뉴 하위의 다음 정렬 순서 계산
     */
    private int calculateNextSortOrder(String priorMenuId) {
        List<MenuResponse> childMenus = menuMapper.findByPriorMenuId(priorMenuId);
        if (childMenus.isEmpty()) {
            return 1;
        }
        return childMenus.stream()
                        .map(MenuResponse::getSortOrder)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(0)
                + 1;
    }

    @Transactional
    @WorkListRecord(workId = "Board_Service", crudType = "U", pkExpression = "#boardId", workName = "게시판")
    public BoardResponse updateBoard(String boardId, BoardUpdateRequest requestDTO, String userId) {
        if (boardMapper.countById(boardId) == 0) {
            throw new NotFoundException("boardId: " + boardId);
        }

        String now = AuditUtil.now();
        boardMapper.updateBoard(boardId, requestDTO, now, userId);

        return boardMapper.selectResponseById(boardId);
    }

    @Transactional
    @WorkListRecord(workId = "Board_Service", crudType = "D", pkExpression = "#boardId", workName = "게시판")
    public void deleteBoard(String boardId) {
        if (boardMapper.countById(boardId) == 0) {
            throw new NotFoundException("boardId: " + boardId);
        }

        String menuId = generateMenuId(boardId);

        // 1. FWK_USER_MENU 삭제
        userMenuMapper.deleteByMenuId(menuId);
        // 2. FWK_MENU 삭제
        menuMapper.deleteById(menuId);
        // 3. FWK_ARTICLE_USER 삭제
        boardMapper.deleteArticleUsersByBoardId(boardId);
        // 4. FWK_ARTICLE 삭제
        boardMapper.deleteArticlesByBoardId(boardId);
        // 5. FWK_BOARD_AUTH 삭제
        boardMapper.deleteBoardAuthByBoardId(boardId);
        // 6. FWK_BOARD_CATEGORY 삭제
        boardMapper.deleteCategoriesByBoardId(boardId);
        // 7. FWK_BOARD 삭제
        boardMapper.deleteBoardById(boardId);

        log.info("게시판 삭제 완료: boardId={}, menuId={}", boardId, menuId);
    }

    public boolean existsById(String boardId) {
        return boardMapper.countById(boardId) > 0;
    }
}

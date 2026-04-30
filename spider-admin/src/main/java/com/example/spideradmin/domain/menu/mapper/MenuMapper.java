package com.example.spideradmin.domain.menu.mapper;

import com.example.spideradmin.domain.menu.dto.MenuCreateRequest;
import com.example.spideradmin.domain.menu.dto.MenuResponse;
import com.example.spideradmin.domain.menu.dto.MenuUpdateRequest;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.annotations.Param;

/**
 * 메뉴 Mapper (CRUD + Query)
 * - insert / update / delete / 단건 조회 (Command 중심)
 * - 목록 조회, 검색, 계층 조회 등 Query
 */
public interface MenuMapper {

    // ==================== 단건 조회 ====================

    /**
     * 메뉴 ID로 단건 조회 (MenuResponse 반환)
     */
    MenuResponse selectResponseById(String menuId);

    // ==================== 존재 확인 ====================

    /**
     * 메뉴 ID 존재 여부 (count)
     */
    int countById(String menuId);

    /**
     * 메뉴 ID 목록으로 존재하는 ID만 반환
     */
    List<String> findExistingMenuIds(@Param("menuIds") Set<String> menuIds);

    /**
     * 메뉴명 중복 확인
     */
    int countByMenuName(String menuName);

    /**
     * 하위 메뉴 존재 확인 (삭제 전 체크용)
     */
    long countByPriorMenuIdExcludeSelf(@Param("priorMenuId") String priorMenuId, @Param("menuId") String menuId);

    // ==================== 생성 ====================

    /**
     * 메뉴 생성
     */
    void insert(
            @Param("dto") MenuCreateRequest dto,
            @Param("now") String now,
            @Param("currentUserId") String currentUserId);

    // ==================== 수정 ====================

    /**
     * 메뉴 수정
     */
    void update(
            @Param("menuId") String menuId,
            @Param("dto") MenuUpdateRequest dto,
            @Param("now") String now,
            @Param("currentUserId") String currentUserId);

    // ==================== 삭제 ====================

    /**
     * 메뉴 삭제
     */
    void deleteById(String menuId);

    // ==================== 목록 조회 ====================

    /**
     * 전체 메뉴 목록 조회
     */
    List<MenuResponse> findAll();

    /**
     * 활성화된 메뉴 목록 조회 (USE_YN='Y', DISPLAY_YN='Y')
     */
    List<MenuResponse> findAllActive();

    /**
     * 사이드바용 전체 메뉴 계층 조회 — 루트부터 CONNECT BY로 완전한 트리 반환
     */
    List<MenuResponse> findAllHierarchy();

    /**
     * 상위 메뉴 ID로 하위 메뉴 목록 조회
     */
    List<MenuResponse> findByPriorMenuId(String priorMenuId);

    // ==================== 검색 조회 ====================

    /**
     * 검색 조건으로 메뉴 목록 조회 (페이징, hasChildren 포함)
     */
    List<MenuResponse> findAllWithSearchPaging(
            @Param("menuId") String menuId,
            @Param("menuName") String menuName,
            @Param("menuUrl") String menuUrl,
            @Param("parentMenuId") String parentMenuId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 검색 결과 총 개수
     */
    long countBySearchCondition(
            @Param("menuId") String menuId,
            @Param("menuName") String menuName,
            @Param("menuUrl") String menuUrl,
            @Param("parentMenuId") String parentMenuId);

    // ==================== 계층 조회 (Oracle CONNECT BY) ====================

    /**
     * 메뉴 ID 목록으로 계층 구조 조회 (할당된 메뉴 + 부모 메뉴)
     * Oracle CONNECT BY 사용
     */
    List<MenuResponse> findMenuHierarchyByMenuIds(
            @Param("menuIds") List<String> menuIds, @Param("useYn") String useYn, @Param("displayYn") String displayYn);

    /**
     * 엑셀 내보내기용 전체 목록 조회 (페이징 없음)
     */
    List<MenuResponse> findAllForExport(
            @Param("menuId") String menuId,
            @Param("menuName") String menuName,
            @Param("menuUrl") String menuUrl,
            @Param("parentMenuId") String parentMenuId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}

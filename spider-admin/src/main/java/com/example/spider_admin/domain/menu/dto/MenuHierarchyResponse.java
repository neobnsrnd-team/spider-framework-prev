package com.example.spider_admin.domain.menu.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 계층형 메뉴 구조 DTO
 * FWK_MENU 테이블의 PRIOR_MENU_ID를 통한 자기참조로 계층 구조 표현
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuHierarchyResponse {

    private String menuId;
    private String menuName;
    private String menuNameEn;
    private String parentMenuId;
    private Integer menuLevel;
    private Integer sortOrder;
    private String iconClass;
    private String menuUrl;
    private String useYn;
    private String displayYn;
    private String description;

    // 하위 메뉴 목록
    @Builder.Default
    private List<MenuHierarchyResponse> children = new ArrayList<>();

    // 메뉴 타입 (category: 상위 카테고리, page: 실제 페이지)
    private String type;

    // 권한 정보 (읽기 전용 등)
    private Boolean readOnly;
}

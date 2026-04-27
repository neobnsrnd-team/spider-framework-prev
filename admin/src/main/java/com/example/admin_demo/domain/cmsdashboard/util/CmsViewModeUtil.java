package com.example.admin_demo.domain.cmsdashboard.util;

/**
 * CMS VIEW_MODE 호환성 유틸리티.
 *
 * <p>기존 DB에는 데스크톱 웹 레이아웃이 {@code PC}로 저장된 데이터가 남아 있을 수 있어,
 * 현재 UI의 {@code web} 값과 동일한 의미로 취급한다.
 */
public final class CmsViewModeUtil {

    public static final String WEB_VIEW_MODE = "web";
    public static final String LEGACY_PC_VIEW_MODE = "PC";

    private CmsViewModeUtil() {}

    /**
     * 새 페이지 생성 시 선택한 레이아웃과 템플릿 레이아웃의 호환 여부를 확인한다.
     *
     * <p>기능 동작을 유지하기 위해 다음 규칙을 따른다.
     * <ul>
     *   <li>선택 레이아웃이 없으면 허용</li>
     *   <li>템플릿 레이아웃이 정확히 일치하면 허용</li>
     *   <li>{@code web} 선택 시 기존 {@code PC} 템플릿도 허용</li>
     * </ul>
     */
    public static boolean isTemplateCompatible(String selectedViewMode, String templateViewMode) {
        return selectedViewMode == null
                || selectedViewMode.equals(templateViewMode)
                || (WEB_VIEW_MODE.equals(selectedViewMode) && LEGACY_PC_VIEW_MODE.equals(templateViewMode));
    }
}

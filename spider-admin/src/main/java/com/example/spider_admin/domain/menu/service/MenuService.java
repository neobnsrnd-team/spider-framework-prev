package com.example.spider_admin.domain.menu.service;

import com.example.spider_admin.domain.menu.dto.MenuCreateRequest;
import com.example.spider_admin.domain.menu.dto.MenuHierarchyResponse;
import com.example.spider_admin.domain.menu.dto.MenuResponse;
import com.example.spider_admin.domain.menu.dto.MenuUpdateRequest;
import com.example.spider_admin.domain.menu.dto.UserMenuResponse;
import com.example.spider_admin.domain.menu.mapper.MenuMapper;
import com.example.spider_admin.domain.menu.mapper.UserMenuMapper;
import com.example.spider_admin.domain.role.dto.RoleMenuResponse;
import com.example.spider_admin.domain.role.mapper.RoleMenuMapper;
import com.example.spider_admin.domain.user.mapper.UserMapper;
import com.example.spider_admin.global.config.MenuProperties;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import com.example.spider_admin.global.util.ExcelColumnDefinition;
import com.example.spider_admin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private static final String ROOT_MENU_MARKER = "*";

    private final MenuMapper menuMapper;
    private final UserMapper userMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserMenuMapper userMenuMapper;
    private final MenuProperties menuProperties;

    // ==================== 조회 (Query) ====================

    public List<MenuResponse> getAllMenus() {
        List<MenuResponse> menuList = menuMapper.findAll();

        // 자식 메뉴 개수 계산
        Map<String, Long> childCountMap = new HashMap<>();
        for (MenuResponse menu : menuList) {
            String priorMenuId = menu.getPriorMenuId();
            if (priorMenuId != null && !priorMenuId.equals(menu.getMenuId())) {
                childCountMap.merge(priorMenuId, 1L, Long::sum);
            }
        }

        menuList.forEach(menu -> {
            Long childCount = childCountMap.getOrDefault(menu.getMenuId(), 0L);
            menu.setHasChildren(childCount > 0);
        });

        return menuList;
    }

    public PageResponse<MenuResponse> searchMenus(
            PageRequest pageRequest, String menuId, String menuName, String menuUrl, String parentMenuId) {

        int offset = pageRequest.getPage() * pageRequest.getSize();
        int limit = pageRequest.getSize();

        List<MenuResponse> content = menuMapper.findAllWithSearchPaging(
                menuId,
                menuName,
                menuUrl,
                parentMenuId,
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                offset,
                limit);

        long totalCount = menuMapper.countBySearchCondition(menuId, menuName, menuUrl, parentMenuId);

        int totalPages = (int) Math.ceil((double) totalCount / pageRequest.getSize());

        PageResponse<MenuResponse> response = new PageResponse<>();
        response.setContent(content);
        response.setTotalElements(totalCount);
        response.setTotalPages(totalPages);
        response.setCurrentPage(pageRequest.getPage() + 1);
        response.setSize(pageRequest.getSize());
        response.setHasPrevious(pageRequest.getPage() > 0);
        response.setHasNext(pageRequest.getPage() + 1 < totalPages);

        return response;
    }

    public List<MenuResponse> getChildMenus(String priorMenuId) {
        return menuMapper.findByPriorMenuId(priorMenuId);
    }

    public List<MenuResponse> getAllActiveMenus() {
        return menuMapper.findAllActive();
    }

    public List<MenuResponse> getMenuTree() {
        List<MenuResponse> allMenus = menuMapper.findAllActive();
        return buildDtoTree(allMenus);
    }

    public List<MenuResponse> getRootMenus() {
        return menuMapper.findAll().stream()
                .filter(m -> isRootMenu(m.getPriorMenuId(), m.getMenuId()))
                .toList();
    }

    private boolean isRootMenu(String priorMenuId, String menuId) {
        if (priorMenuId == null || priorMenuId.isEmpty()) {
            return true;
        }
        if (ROOT_MENU_MARKER.equals(priorMenuId)) {
            return true;
        }
        return priorMenuId.equals(menuId);
    }

    public MenuResponse getMenuById(String menuId) {
        MenuResponse dto = menuMapper.selectResponseById(menuId);
        if (dto == null) {
            throw new NotFoundException("menuId: " + menuId);
        }

        long childCount = menuMapper.countByPriorMenuIdExcludeSelf(menuId, menuId);
        dto.setHasChildren(childCount > 0);

        return dto;
    }

    public List<MenuResponse> getSelectableParentMenus() {
        List<MenuResponse> allMenus = menuMapper.findAll();

        Map<String, List<String>> childrenMap = new HashMap<>();
        for (MenuResponse menu : allMenus) {
            String priorMenuId = menu.getPriorMenuId();
            if (priorMenuId != null && !priorMenuId.equals(menu.getMenuId())) {
                childrenMap.computeIfAbsent(priorMenuId, k -> new ArrayList<>()).add(menu.getMenuId());
            }
        }

        return allMenus.stream()
                .filter(menu -> {
                    List<String> children = childrenMap.get(menu.getMenuId());
                    if (children == null || children.isEmpty()) {
                        return false;
                    }
                    return children.stream().allMatch(childId -> !childrenMap.containsKey(childId));
                })
                .toList();
    }

    public byte[] exportMenus(
            String menuId, String menuName, String menuUrl, String parentMenuId, String sortBy, String sortDirection) {
        List<MenuResponse> data =
                menuMapper.findAllForExport(menuId, menuName, menuUrl, parentMenuId, sortBy, sortDirection);
        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }
        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("메뉴ID", 15, "menuId"),
                new ExcelColumnDefinition("메뉴명", 20, "menuName"),
                new ExcelColumnDefinition("메뉴URL", 30, "menuUrl"),
                new ExcelColumnDefinition("상위 메뉴 ID", 15, "priorMenuId"),
                new ExcelColumnDefinition("정렬순서", 10, "sortOrder"),
                new ExcelColumnDefinition("출력여부", 8, "displayYn"),
                new ExcelColumnDefinition("사용여부", 8, "useYn"),
                new ExcelColumnDefinition("웹앱 ID", 12, "webAppId"),
                new ExcelColumnDefinition("수정일시", 18, "lastUpdateDtime"),
                new ExcelColumnDefinition("수정자", 12, "lastUpdateUserId"));
        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (MenuResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("menuId", item.getMenuId());
            row.put("menuName", item.getMenuName());
            row.put("menuUrl", item.getMenuUrl());
            row.put("priorMenuId", item.getPriorMenuId());
            row.put("sortOrder", item.getSortOrder());
            row.put("displayYn", item.getDisplayYn());
            row.put("useYn", item.getUseYn());
            row.put("webAppId", item.getWebAppId());
            row.put("lastUpdateDtime", item.getLastUpdateDtime());
            row.put("lastUpdateUserId", item.getLastUpdateUserId());
            rows.add(row);
        }
        try {
            return ExcelExportUtil.createWorkbook("메뉴", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    // ==================== 생성/수정/삭제 (Command) ====================

    @Transactional
    public MenuResponse createMenu(MenuCreateRequest menuDTO) {
        if (menuMapper.countById(menuDTO.getMenuId()) > 0) {
            throw new DuplicateException("menuId: " + menuDTO.getMenuId());
        }

        if (menuDTO.getPriorMenuId() == null || menuDTO.getPriorMenuId().trim().isEmpty()) {
            throw new InvalidInputException("상위 메뉴 ID는 필수입니다");
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        menuMapper.insert(menuDTO, now, currentUserId);

        return menuMapper.selectResponseById(menuDTO.getMenuId());
    }

    @Transactional
    public MenuResponse updateMenu(String menuId, MenuUpdateRequest menuDTO) {
        MenuResponse existing = menuMapper.selectResponseById(menuId);
        if (existing == null) {
            throw new NotFoundException("menuId: " + menuId);
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        menuMapper.update(menuId, menuDTO, now, currentUserId);

        return menuMapper.selectResponseById(menuId);
    }

    @Transactional
    public void deleteMenu(String menuId) {
        if (menuMapper.countById(menuId) == 0) {
            throw new NotFoundException("menuId: " + menuId);
        }

        long childCount = menuMapper.countByPriorMenuIdExcludeSelf(menuId, menuId);
        if (childCount > 0) {
            throw new InvalidInputException("하위 메뉴가 있는 메뉴는 삭제할 수 없습니다");
        }

        menuMapper.deleteById(menuId);
    }

    // ==================== 계층 조회 (사용자/역할별) ====================

    public List<MenuHierarchyResponse> getAllMenuHierarchy() {
        List<MenuResponse> allMenus = filterHiddenMenus(menuMapper.findAllHierarchy());
        if (allMenus.isEmpty()) {
            return Collections.emptyList();
        }
        return buildMenuHierarchy(allMenus, Collections.emptyMap());
    }

    public List<MenuHierarchyResponse> getMenusByUserId(String userId) {
        if (userMapper.countByUserId(userId) == 0) {
            throw new NotFoundException("userId: " + userId);
        }

        List<UserMenuResponse> userMenus = userMenuMapper.selectByUserIdWithDetails(userId);

        if (userMenus.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> menuPermissions = userMenus.stream()
                .collect(Collectors.toMap(
                        UserMenuResponse::getMenuId,
                        um -> um.getAuthCode() != null ? um.getAuthCode() : "",
                        (existing, replacement) -> existing));

        List<String> allowedMenuIds = new ArrayList<>(menuPermissions.keySet());

        List<MenuResponse> allMenus =
                filterHiddenMenus(menuMapper.findMenuHierarchyByMenuIds(allowedMenuIds, "Y", "Y"));

        if (allMenus.isEmpty()) {
            return Collections.emptyList();
        }

        return buildMenuHierarchy(allMenus, menuPermissions);
    }

    public List<MenuHierarchyResponse> getMenusByRoleId(String roleId) {
        List<RoleMenuResponse> roleMenus = roleMenuMapper.selectByRoleId(roleId);

        if (roleMenus.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> menuPermissions = roleMenus.stream()
                .collect(Collectors.toMap(
                        RoleMenuResponse::getMenuId,
                        RoleMenuResponse::getAuthCode,
                        (existing, replacement) -> existing));

        List<String> allowedMenuIds = new ArrayList<>(menuPermissions.keySet());

        List<MenuResponse> allMenus =
                filterHiddenMenus(menuMapper.findMenuHierarchyByMenuIds(allowedMenuIds, "Y", "Y"));

        if (allMenus.isEmpty()) {
            return Collections.emptyList();
        }

        return buildMenuHierarchy(allMenus, menuPermissions);
    }

    private List<MenuResponse> filterHiddenMenus(List<MenuResponse> menus) {
        List<String> hiddenMenuIds = menuProperties.getHiddenMenuIds();
        if (hiddenMenuIds == null || hiddenMenuIds.isEmpty()) {
            return menus;
        }

        // 부모 → 자식 ID 목록 맵 구성
        Map<String, List<String>> childrenMap = new HashMap<>();
        for (MenuResponse menu : menus) {
            String prior = menu.getPriorMenuId();
            if (prior != null && !prior.isEmpty()) {
                childrenMap.computeIfAbsent(prior, k -> new ArrayList<>()).add(menu.getMenuId());
            }
        }

        // BFS로 숨길 메뉴 + 모든 하위 메뉴 ID 수집
        Set<String> toHide = new HashSet<>();
        Queue<String> queue = new LinkedList<>(hiddenMenuIds);
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (toHide.add(id)) {
                queue.addAll(childrenMap.getOrDefault(id, Collections.emptyList()));
            }
        }

        return menus.stream().filter(m -> !toHide.contains(m.getMenuId())).collect(Collectors.toList());
    }

    private List<MenuHierarchyResponse> buildMenuHierarchy(
            List<MenuResponse> allMenus, Map<String, String> menuPermissions) {

        Map<String, MenuHierarchyResponse> menuMap = new HashMap<>();
        List<MenuHierarchyResponse> rootMenus = new ArrayList<>();

        for (MenuResponse menu : allMenus) {
            String authCode = menuPermissions.get(menu.getMenuId());
            MenuHierarchyResponse dto = toHierarchyDTO(menu, authCode);
            menuMap.put(menu.getMenuId(), dto);
        }

        for (MenuResponse menu : allMenus) {
            String priorMenuId = menu.getPriorMenuId();
            MenuHierarchyResponse currentDTO = menuMap.get(menu.getMenuId());

            boolean isRoot = priorMenuId == null
                    || priorMenuId.isEmpty()
                    || priorMenuId.equals(menu.getMenuId())
                    || !menuMap.containsKey(priorMenuId);

            if (isRoot) {
                rootMenus.add(currentDTO);
            } else {
                MenuHierarchyResponse parentDTO = menuMap.get(priorMenuId);
                if (parentDTO != null) {
                    parentDTO.getChildren().add(currentDTO);
                }
            }
        }

        rootMenus.sort(Comparator.comparing(
                MenuHierarchyResponse::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));
        rootMenus.forEach(this::sortChildren);

        return rootMenus;
    }

    private void sortChildren(MenuHierarchyResponse menu) {
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            menu.getChildren()
                    .sort(Comparator.comparing(
                            MenuHierarchyResponse::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));
            menu.getChildren().forEach(this::sortChildren);
        }
    }

    private MenuHierarchyResponse toHierarchyDTO(MenuResponse menu, String authCode) {
        boolean readOnly = "R".equals(authCode);
        String url = menu.getMenuUrl();
        String type = (url == null || url.isEmpty() || "NAN".equalsIgnoreCase(url) || "NaN".equals(url))
                ? "category"
                : "page";

        return MenuHierarchyResponse.builder()
                .menuId(menu.getMenuId())
                .menuName(menu.getMenuName())
                .menuNameEn(null)
                .parentMenuId(menu.getPriorMenuId())
                .menuLevel(null)
                .sortOrder(menu.getSortOrder())
                .iconClass(menu.getMenuImage())
                .menuUrl(menu.getMenuUrl())
                .useYn(menu.getUseYn())
                .displayYn(menu.getDisplayYn())
                .description(null)
                .readOnly(readOnly)
                .type(type)
                .children(new ArrayList<>())
                .build();
    }

    private List<MenuResponse> buildDtoTree(List<MenuResponse> flatMenus) {
        if (flatMenus == null || flatMenus.isEmpty()) {
            return new ArrayList<>();
        }

        return flatMenus.stream()
                .filter(menu ->
                        menu.getPriorMenuId() == null || menu.getPriorMenuId().equals(menu.getMenuId()))
                .map(root -> {
                    buildChildren(root, flatMenus);
                    return root;
                })
                .toList();
    }

    private void buildChildren(MenuResponse parent, List<MenuResponse> allMenus) {
        List<MenuResponse> children = allMenus.stream()
                .filter(menu -> parent.getMenuId().equals(menu.getPriorMenuId())
                        && !menu.getMenuId().equals(parent.getMenuId()))
                .toList();

        if (!children.isEmpty()) {
            parent.setChildren(children);
            children.forEach(child -> buildChildren(child, allMenus));
        }
    }
}

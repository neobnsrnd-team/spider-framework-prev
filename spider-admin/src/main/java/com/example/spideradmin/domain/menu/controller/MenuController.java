package com.example.spideradmin.domain.menu.controller;

import com.example.spideradmin.domain.menu.dto.MenuCreateRequest;
import com.example.spideradmin.domain.menu.dto.MenuResponse;
import com.example.spideradmin.domain.menu.dto.MenuUpdateRequest;
import com.example.spideradmin.domain.menu.service.MenuService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.util.ExcelExportUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MENU:R')")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getAllMenus() {
        List<MenuResponse> menus = menuService.getAllMenus();
        return ResponseEntity.ok(ApiResponse.success(menus));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getAllActiveMenus() {
        List<MenuResponse> menus = menuService.getAllActiveMenus();
        return ResponseEntity.ok(ApiResponse.success(menus));
    }

    @GetMapping("/root")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getRootMenus() {
        List<MenuResponse> menus = menuService.getRootMenus();
        return ResponseEntity.ok(ApiResponse.success(menus));
    }

    @GetMapping("/children/{priorMenuId}")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getChildMenus(@PathVariable String priorMenuId) {
        List<MenuResponse> menus = menuService.getChildMenus(priorMenuId);
        return ResponseEntity.ok(ApiResponse.success(menus));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<MenuResponse>>> getMenusWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String menuId,
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) String menuUrl,
            @RequestParam(required = false) String parentMenuId) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PageResponse<MenuResponse> response =
                menuService.searchMenus(pageRequest, menuId, menuName, menuUrl, parentMenuId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<ApiResponse<MenuResponse>> getMenuById(@PathVariable String menuId) {
        MenuResponse menu = menuService.getMenuById(menuId);
        return ResponseEntity.ok(ApiResponse.success(menu));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MENU:W')")
    public ResponseEntity<ApiResponse<MenuResponse>> createMenu(@Valid @RequestBody MenuCreateRequest menuDTO) {
        MenuResponse createdMenu = menuService.createMenu(menuDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("메뉴가 생성되었습니다", createdMenu));
    }

    @PutMapping("/{menuId}")
    @PreAuthorize("hasAuthority('MENU:W')")
    public ResponseEntity<ApiResponse<MenuResponse>> updateMenu(
            @PathVariable String menuId, @Valid @RequestBody MenuUpdateRequest menuDTO) {
        MenuResponse updatedMenu = menuService.updateMenu(menuId, menuDTO);
        return ResponseEntity.ok(ApiResponse.success("메뉴가 수정되었습니다", updatedMenu));
    }

    @DeleteMapping("/{menuId}")
    @PreAuthorize("hasAuthority('MENU:W')")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(@PathVariable String menuId) {
        menuService.deleteMenu(menuId);
        return ResponseEntity.ok(ApiResponse.success("메뉴가 삭제되었습니다", null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMenus(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String menuId,
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) String menuUrl,
            @RequestParam(required = false) String parentMenuId) {
        byte[] excelBytes = menuService.exportMenus(menuId, menuName, menuUrl, parentMenuId, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("Menu", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping("/selectable-parents")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getSelectableParentMenus() {
        List<MenuResponse> menus = menuService.getSelectableParentMenus();
        return ResponseEntity.ok(ApiResponse.success(menus));
    }
}

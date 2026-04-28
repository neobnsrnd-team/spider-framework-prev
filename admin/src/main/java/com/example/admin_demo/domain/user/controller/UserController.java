package com.example.admin_demo.domain.user.controller;

import com.example.admin_demo.domain.user.dto.UserCreateRequest;
import com.example.admin_demo.domain.user.dto.UserResponse;
import com.example.admin_demo.domain.user.dto.UserSearchRequest;
import com.example.admin_demo.domain.user.dto.UserSimpleResponse;
import com.example.admin_demo.domain.user.dto.UserUpdateRequest;
import com.example.admin_demo.domain.user.dto.UserWithRoleResponse;
import com.example.admin_demo.domain.user.service.UserService;
import com.example.admin_demo.global.dto.ApiResponse;
import com.example.admin_demo.global.dto.PageRequest;
import com.example.admin_demo.global.dto.PageResponse;
import com.example.admin_demo.global.exception.ErrorType;
import com.example.admin_demo.global.exception.base.BaseException;
import com.example.admin_demo.global.security.CustomUserDetails;
import com.example.admin_demo.global.security.CustomUserDetailsService;
import com.example.admin_demo.global.util.ExcelExportUtil;
import com.example.admin_demo.global.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER:R')")
public class UserController {

    private final UserService userService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<UserWithRoleResponse>>> getUsersWithPagination(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String roleFilter,
            @RequestParam(required = false) String classNameFilter,
            @RequestParam(required = false) String userStateCodeFilter) {

        PageRequest pageRequest = PageRequest.builder()
                .page(page - 1)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        UserSearchRequest searchDTO = UserSearchRequest.builder()
                .searchField(searchField)
                .searchValue(searchValue)
                .roleFilter(roleFilter)
                .classNameFilter(classNameFilter)
                .userStateCodeFilter(userStateCodeFilter)
                .build();

        return ResponseEntity.ok(ApiResponse.success(userService.searchUsers(pageRequest, searchDTO)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(userId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER:W')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserCreateRequest requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(userService.createUser(requestDTO)));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER:W')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String userId, @Valid @RequestBody UserUpdateRequest requestDTO) {

        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(userId, requestDTO)));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER:W')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{userId}/error-count/reset")
    @PreAuthorize("hasAuthority('USER:W')")
    public ResponseEntity<ApiResponse<Void>> resetLoginError(@PathVariable String userId) {
        userService.resetLoginError(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 패스워드는 직번으로 초기화되었습니다.", null));
    }

    @PostMapping("/{userId}/impersonate")
    public ResponseEntity<ApiResponse<Void>> impersonateUser(
            @PathVariable String userId, HttpServletRequest request, HttpServletResponse response) {

        CustomUserDetails currentUser = SecurityUtil.getCurrentUser();
        if (currentUser == null || !"superadmin".equals(currentUser.getRoleId())) {
            throw new BaseException(ErrorType.FORBIDDEN, "superadmin만 사용 가능합니다.");
        }

        // 대상 사용자 존재 확인
        userService.getUserById(userId);
        CustomUserDetails targetDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(userId);

        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(targetDetails, null, targetDetails.getAuthorities());

        SecurityContext newContext = SecurityContextHolder.createEmptyContext();
        newContext.setAuthentication(newAuth);

        // 기존 세션 무효화 후 새 세션에 SecurityContext 저장
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, newContext);
        SecurityContextHolder.setContext(newContext);

        log.info("Impersonation: [{}] switched to userId=[{}]", currentUser.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success("로그인 전환이 완료되었습니다.", null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchField,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String roleFilter,
            @RequestParam(required = false) String classNameFilter,
            @RequestParam(required = false) String userStateCodeFilter) {
        byte[] excelBytes = userService.exportUsers(
                searchField, searchValue, roleFilter, classNameFilter, userStateCodeFilter, sortBy, sortDirection);
        String fileName = ExcelExportUtil.generateFileName("User", LocalDate.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping("/check/username")
    public ResponseEntity<ApiResponse<Boolean>> checkUserNameExists(@RequestParam String userName) {
        return ResponseEntity.ok(ApiResponse.success(userService.existsByUserName(userName)));
    }

    @GetMapping("/check/email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailExists(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.existsByEmail(email)));
    }

    /** 권한이양 대상 사용자 검색 — userId·userName LIKE, 최대 20건. */
    // WORK_TASK:R 권한으로도 접근 가능 — 권한이양 대상 사용자 검색용
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('USER:R', 'WORK_TASK:R')")
    public ResponseEntity<ApiResponse<List<UserSimpleResponse>>> searchForTransfer(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        return ResponseEntity.ok(ApiResponse.success(userService.searchForTransfer(keyword)));
    }
}

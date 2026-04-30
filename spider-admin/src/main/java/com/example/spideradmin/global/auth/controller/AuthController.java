package com.example.spideradmin.global.auth.controller;

import com.example.spideradmin.domain.user.dto.UserCreateRequest;
import com.example.spideradmin.domain.user.dto.UserResponse;
import com.example.spideradmin.domain.user.mapper.UserMapper;
import com.example.spideradmin.domain.user.service.UserService;
import com.example.spideradmin.global.auth.dto.CmsApproverResponse;
import com.example.spideradmin.global.auth.dto.CurrentUserResponse;
import com.example.spideradmin.global.auth.dto.LoginRequest;
import com.example.spideradmin.global.auth.dto.LoginResponse;
import com.example.spideradmin.global.auth.service.AuthService;
import com.example.spideradmin.global.dto.ApiResponse;
import com.example.spideradmin.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserCreateRequest userCreateRequestDTO) {
        UserResponse createdUser = userService.createUser(userCreateRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("회원가입이 완료되었습니다", createdUser));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // TODO: Spring Security 세션 무효화 처리
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공", null));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateCredentials(@Valid @RequestBody LoginRequest loginRequest) {
        boolean isValid = authService.validateCredentials(loginRequest.getUserId(), loginRequest.getPassword());
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Set<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toCollection(TreeSet::new));

        CurrentUserResponse response = CurrentUserResponse.builder()
                .userId(userDetails.getUserId())
                .userName(userDetails.getDisplayName())
                .roleId(userDetails.getRoleId())
                .authorities(authorities)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/cms-approvers")
    public ResponseEntity<ApiResponse<List<CmsApproverResponse>>> cmsApprovers() {
        return ResponseEntity.ok(ApiResponse.success(userMapper.findCmsApprovers()));
    }

    @GetMapping("/react-cms-approvers")
    public ResponseEntity<ApiResponse<List<CmsApproverResponse>>> reactCmsApprovers() {
        return ResponseEntity.ok(ApiResponse.success(userMapper.findReactCmsApprovers()));
    }

    @GetMapping("/permission/menu")
    public ResponseEntity<ApiResponse<Boolean>> checkMenuPermission(
            @RequestParam String userId, @RequestParam String menuId) {
        boolean hasPermission = authService.hasMenuPermission(userId, menuId);
        return ResponseEntity.ok(ApiResponse.success(hasPermission));
    }
}

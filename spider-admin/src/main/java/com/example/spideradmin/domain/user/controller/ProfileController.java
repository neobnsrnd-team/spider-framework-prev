package com.example.spideradmin.domain.user.controller;

import com.example.spideradmin.domain.user.dto.ProfileResponse;
import com.example.spideradmin.domain.user.dto.ProfileUpdateRequest;
import com.example.spideradmin.domain.user.service.UserService;
import com.example.spideradmin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 현재 로그인한 사용자의 개인정보 조회/수정 API를 제공합니다.
 * <p>
 * 이 컨트롤러는 PROFILE 권한 체계에 바인딩되어 있으며,
 * 모든 응답은 {@link ApiResponse} 규격으로 통일하여 반환합니다.
 * </p>
 *
 * @see UserService
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PROFILE:R')")
public class ProfileController {

    private final UserService userService;

    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회합니다.
     *
     * @return 프로필 정보 (읽기 전용 필드 + 수정 가능 필드)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile()));
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보를 수정합니다.
     * <p>
     * 수정 가능 필드: userName, newPassword, email, phone, address, userSsn, className
     * </p>
     *
     * @param requestDTO 수정할 프로필 정보
     * @return 수정된 프로필 정보
     */
    @PutMapping
    @PreAuthorize("hasAuthority('PROFILE:W')")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest requestDTO) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(requestDTO)));
    }
}

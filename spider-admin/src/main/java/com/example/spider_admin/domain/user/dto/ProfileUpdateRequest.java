package com.example.spider_admin.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Profile Update Request
 * Used for updating current user's own profile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {

    @NotBlank(message = "사용자ID는 필수입니다")
    @Size(max = 20, message = "사용자ID는 20자 이내여야 합니다")
    private String userId;

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(max = 50, message = "사용자명은 50자 이내여야 합니다")
    private String userName;

    @Size(max = 50, message = "비밀번호는 50자 이내여야 합니다")
    private String newPassword;

    @Size(max = 50, message = "비밀번호 확인은 50자 이내여야 합니다")
    private String confirmPassword;

    @Email(message = "유효한 이메일 형식이어야 합니다")
    @Size(max = 100, message = "이메일은 100자 이내여야 합니다")
    private String email;

    @Size(max = 13, message = "전화번호는 13자 이내여야 합니다")
    private String phone;

    @Size(max = 200, message = "주소는 200자 이내여야 합니다")
    private String address;

    @Size(max = 100, message = "직급명은 100자 이내여야 합니다")
    private String className;
}

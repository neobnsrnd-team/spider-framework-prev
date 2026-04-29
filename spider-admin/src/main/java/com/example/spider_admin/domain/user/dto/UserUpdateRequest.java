package com.example.spider_admin.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for User entity
 * Used for API request/response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    private String userId;

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(max = 50, message = "사용자명은 50자 이내여야 합니다")
    private String userName;

    @Size(max = 50, message = "비밀번호는 50자 이내여야 합니다")
    private String password;

    private String roleId;
    private String roleName;

    @Size(max = 100, message = "부서명은 100자 이내여야 합니다")
    private String positionName;

    @Size(max = 200, message = "주소는 200자 이내여야 합니다")
    private String address;

    @Size(max = 100, message = "직급명 100자 이내여야 합니다")
    private String className;

    @Email(message = "유효한 이메일 형식이어야 합니다")
    @Size(max = 100, message = "이메일은 100자 이내여야 합니다")
    private String email;

    private String userStateCode;
    private String lastUpdateDtime;
    private String lastUpdateUserId;

    @Size(max = 15, message = "접속 IP는 15자 이내여야 합니다")
    private String accessIp;

    @Size(max = 13, message = "주민번호는 13자 이내여야 합니다")
    private String userSsn;

    @Size(max = 13, message = "전화번호는 13자 이내여야 합니다")
    private String phone;

    @Size(max = 10, message = "등록요청자명은 10자 이내여야 합니다")
    private String regReqUserName;

    @Size(max = 10, message = "직책명은 10자 이내여야 합니다")
    private String titleName;

    @Size(max = 10, message = "사원번호는 10자 이내여야 합니다")
    private String empNo;

    @Size(max = 10, message = "지점번호는 10자 이내여야 합니다")
    private String branchNo;

    @Size(max = 100, message = "업무권한코드는 100자 이내여야 합니다")
    private String bizAuthCode;
}

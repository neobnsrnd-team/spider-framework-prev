package com.example.spider_admin.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Profile Response
 * Contains current user's profile information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    // ===== 읽기 전용 필드 =====
    private String userId;
    private String positionName; // 부서명
    private String roleId;
    private String roleName; // 역할명
    private String userStateCode;
    private String userStateName; // 사용자상태명 (정상/삭제/정지)
    private String accessIp;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String userSsn; // 주민등록번호 (읽기 전용)

    // ===== 수정 가능 필드 =====
    private String userName;
    private String email;
    private String phone;
    private String address;
    private String className; // 직급
}

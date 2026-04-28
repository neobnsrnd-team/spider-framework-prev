package com.example.admin_demo.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 권한이양 대상 사용자 검색용 경량 응답 DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleResponse {
    private String userId;
    private String userName;
}

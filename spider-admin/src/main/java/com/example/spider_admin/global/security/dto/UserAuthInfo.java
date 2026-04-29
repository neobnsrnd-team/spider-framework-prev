package com.example.spider_admin.global.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthInfo {

    private String userId;
    private String userName;
    private String roleId;
    private String passwd;
    private String userStateCode;
}

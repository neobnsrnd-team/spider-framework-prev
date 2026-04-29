package com.example.spider_admin.domain.user.dto;

import lombok.Data;

@Data
public class UserWithRoleResponse {

    // ===== User =====
    private String userId;
    private String userName;

    private String roleId;

    private String positionName;
    private String address;
    private String className;
    private String email;
    private String userStateCode;

    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String accessIp;
    private String userSsn;
    private String phone;

    private String regReqUserName;
    private String titleName;
    private String empNo;
    private String branchNo;
    private String bizAuthCode;

    private Integer loginFailCount;

    // ===== Role =====
    private String roleName;
    private String roleUseYn;
    private String roleDesc;
    private Integer ranking;
}

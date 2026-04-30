package com.example.spideradmin.domain.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
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
}

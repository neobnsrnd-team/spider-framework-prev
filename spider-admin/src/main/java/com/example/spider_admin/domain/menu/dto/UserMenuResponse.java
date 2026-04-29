package com.example.spider_admin.domain.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user-menu mapping response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMenuResponse {

    private String userId;
    private String menuId;
    private String menuName;
    private String authCode;
    private Integer favorMenuOrder;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}

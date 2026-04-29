package com.example.spideradmin.domain.role.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleMenuResponse {

    private String roleId;

    private String menuId;

    private String authCode;
}

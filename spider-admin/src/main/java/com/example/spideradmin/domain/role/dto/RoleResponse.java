package com.example.spideradmin.domain.role.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private String roleId;

    private String roleName;

    private String useYn;

    private String roleDesc;

    private String lastUpdateDtime;

    private String lastUpdateUserId;

    private String ranking;

    private Integer userCount;
}

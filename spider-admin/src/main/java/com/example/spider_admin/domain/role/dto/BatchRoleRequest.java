package com.example.spider_admin.domain.role.dto;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchRoleRequest {

    @Valid
    private List<RoleCreateRequest> newRoles;

    @Valid
    private List<RoleUpdateRequest> updatedRoles;

    private List<String> deletedRoleIds;
}

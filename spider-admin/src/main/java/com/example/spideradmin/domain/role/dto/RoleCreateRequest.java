package com.example.spideradmin.domain.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleCreateRequest {

    @NotBlank(message = "역할ID는 필수입니다")
    @Size(max = 10, message = "역할ID는 10자 이내여야 합니다")
    private String roleId;

    @NotBlank(message = "역할명은 필수입니다")
    @Size(max = 50, message = "역할명은 50자 이내여야 합니다")
    private String roleName;

    @Size(max = 1, message = "사용여부는 1자여야 합니다")
    private String useYn;

    @Size(max = 200, message = "역할설명은 200자 이내여야 합니다")
    private String roleDesc;

    private String ranking;
}

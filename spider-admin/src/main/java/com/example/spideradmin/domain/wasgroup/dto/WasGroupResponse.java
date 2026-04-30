package com.example.spideradmin.domain.wasgroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WAS 그룹 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasGroupResponse {

    private String wasGroupId;

    private String wasGroupName;

    private String wasGroupDesc;
}

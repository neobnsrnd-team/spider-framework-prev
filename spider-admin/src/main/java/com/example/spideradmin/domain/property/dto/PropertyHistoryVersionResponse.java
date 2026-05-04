package com.example.spideradmin.domain.property.dto;

import lombok.*;

/**
 * 프로퍼티 이력 버전 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyHistoryVersionResponse {
    private Integer version;
    private String reason;
    private String lastUpdateUserId;
    private String lastUpdateDtime;
}

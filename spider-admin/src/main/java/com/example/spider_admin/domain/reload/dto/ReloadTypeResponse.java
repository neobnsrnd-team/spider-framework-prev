package com.example.spider_admin.domain.reload.dto;

import lombok.*;

/**
 * Reload 항목 정보 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReloadTypeResponse {

    private String code;
    private String label;
    private String description;
}

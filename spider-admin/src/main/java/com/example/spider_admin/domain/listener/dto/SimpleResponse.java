package com.example.spider_admin.domain.listener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공통 셀렉트 옵션 DTO (value/label)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimpleResponse {
    private String value;
    private String label;
}

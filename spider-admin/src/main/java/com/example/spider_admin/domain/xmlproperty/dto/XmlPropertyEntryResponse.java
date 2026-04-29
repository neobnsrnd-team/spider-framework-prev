package com.example.spider_admin.domain.xmlproperty.dto;

import lombok.*;

/**
 * XML Property 항목 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XmlPropertyEntryResponse {

    private String key;
    private String value;
    private String description;
}

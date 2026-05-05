package com.example.spideradmin.domain.xmlproperty.dto;

import lombok.*;

/**
 * XML Property 파일 목록 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XmlPropertyFileResponse {

    private String fileName;
    private String description;
    private int entryCount;
    private String lastModified;
}

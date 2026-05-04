package com.example.spideradmin.domain.xmlproperty.dto;

import java.util.List;
import lombok.*;

/**
 * XML Property 파일 상세 응답 DTO (항목 포함)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XmlPropertyFileDetailResponse {

    private String fileName;
    private String description;
    private String lastModified;
    private List<XmlPropertyEntryResponse> entries;
}

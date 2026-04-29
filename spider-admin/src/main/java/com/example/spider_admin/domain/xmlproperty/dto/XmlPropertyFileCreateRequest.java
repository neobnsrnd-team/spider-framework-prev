package com.example.spider_admin.domain.xmlproperty.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * XML Property 파일 등록 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class XmlPropertyFileCreateRequest {

    @NotBlank(message = "파일명은 필수입니다")
    private String fileName;

    private String description;
}

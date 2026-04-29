package com.example.spider_admin.domain.xmlproperty.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

/**
 * XML Property 항목 일괄 저장 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class XmlPropertySaveRequest {

    @NotBlank(message = "파일명은 필수입니다")
    private String fileName;

    private String description;

    @NotNull(message = "항목 목록은 필수입니다")
    @Valid
    private List<EntryDTO> entries;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntryDTO {

        @NotBlank(message = "key는 필수입니다")
        private String key;

        private String value;

        private String description;
    }
}

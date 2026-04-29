package com.example.spider_admin.domain.board.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardCategoryUpdateRequest {

    @NotBlank(message = "카테고리명은 필수입니다")
    private String categoryName;
}

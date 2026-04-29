package com.example.spider_admin.domain.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 게시글 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleUpdateRequest {

    private String categorySeq;

    private String topYn;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 50, message = "제목은 50자 이하여야 합니다")
    private String title;

    @Size(max = 10000, message = "내용은 최대 10,000자까지 입력 가능합니다")
    private String content;

    private String attachFilePath1;

    private String attachFilePath2;

    private String attachFilePath3;
}

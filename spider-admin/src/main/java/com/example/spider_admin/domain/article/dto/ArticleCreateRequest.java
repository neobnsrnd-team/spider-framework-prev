package com.example.spider_admin.domain.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 게시글 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleCreateRequest {

    @NotBlank(message = "게시판 ID는 필수입니다")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "게시판 ID는 영문/숫자/언더스코어/하이픈만 허용합니다")
    private String boardId;

    private String categorySeq;

    private String topYn;

    private Long refArticleSeq;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 50, message = "제목은 50자 이하여야 합니다")
    private String title;

    @Size(max = 10000, message = "내용은 최대 10,000자까지 입력 가능합니다")
    private String content;

    private String attachFilePath1;

    private String attachFilePath2;

    private String attachFilePath3;
}

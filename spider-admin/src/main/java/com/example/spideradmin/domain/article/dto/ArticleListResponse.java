package com.example.spideradmin.domain.article.dto;

import lombok.*;

/**
 * 게시글 목록 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleListResponse {

    private Long articleSeq;

    private String boardId;

    private String categorySeq;

    private String categoryName;

    private String topYn;

    private Integer step;

    private String title;

    private String writerName;

    private Integer readCnt;

    private boolean hasAttachment;

    private String registDtime;
}

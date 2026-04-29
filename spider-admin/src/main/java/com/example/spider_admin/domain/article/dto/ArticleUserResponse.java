package com.example.spider_admin.domain.article.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleUserResponse {
    private String userId;
    private String boardId;
    private Long articleSeq;
    private String lastUpdateDtime;
}

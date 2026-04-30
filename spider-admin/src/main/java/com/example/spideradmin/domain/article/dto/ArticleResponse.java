package com.example.spideradmin.domain.article.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponse {
    private Long articleSeq;
    private String boardId;
    private String categorySeq;
    private String categoryName;
    private String topYn;
    private Long refArticleSeq;
    private Integer position;
    private Integer step;
    private String title;
    private String writerId;
    private String writerName;
    private Integer readCnt;
    private String attachFilePath1;
    private Integer downloadCnt1;
    private String attachFilePath2;
    private Integer downloadCnt2;
    private String attachFilePath3;
    private Integer downloadCnt3;
    private String registDtime;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
    private String content;
}

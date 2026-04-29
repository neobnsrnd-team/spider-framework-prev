package com.example.spider_admin.domain.article.dto;

import java.util.List;
import lombok.*;

/**
 * 게시글 상세 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleDetailResponse {

    private Long articleSeq;

    private String boardId;

    private String boardName;

    private String categorySeq;

    private String categoryName;

    private String topYn;

    private Long refArticleSeq;

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

    private String content;

    private List<ArticleListResponse> replies;
}

package com.example.spider_admin.domain.board.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResponse {
    private String boardId;
    private String boardName;
    private String boardType;
    private String adminId;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}

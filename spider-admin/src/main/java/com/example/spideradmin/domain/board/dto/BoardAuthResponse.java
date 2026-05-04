package com.example.spideradmin.domain.board.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardAuthResponse {
    private String userId;
    private String boardId;
    private String authCode;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}

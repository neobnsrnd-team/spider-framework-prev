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
public class BoardAuthCreateRequest {

    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    @NotBlank(message = "게시판 ID는 필수입니다")
    private String boardId;

    @NotBlank(message = "권한 코드는 필수입니다")
    private String authCode;
}

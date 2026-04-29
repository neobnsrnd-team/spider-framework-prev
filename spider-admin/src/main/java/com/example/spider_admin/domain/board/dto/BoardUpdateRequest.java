package com.example.spider_admin.domain.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 게시판 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardUpdateRequest {

    @NotBlank(message = "게시판명은 필수입니다")
    @Size(max = 100, message = "게시판명은 100자 이하여야 합니다")
    private String boardName;

    private String boardType;

    private String adminId;
}

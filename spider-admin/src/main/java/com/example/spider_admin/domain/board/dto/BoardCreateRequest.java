package com.example.spider_admin.domain.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 게시판 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardCreateRequest {

    @NotBlank(message = "게시판 ID는 필수입니다")
    @Size(max = 20, message = "게시판 ID는 20자 이하여야 합니다")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "게시판 ID는 영문/숫자/언더스코어/하이픈만 허용합니다")
    private String boardId;

    @NotBlank(message = "게시판명은 필수입니다")
    @Size(max = 100, message = "게시판명은 100자 이하여야 합니다")
    private String boardName;

    private String boardType;

    private String adminId;
}

package com.example.spideradmin.domain.board.dto;

import com.example.spideradmin.global.dto.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 게시판 검색 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardSearchRequest {

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 10;

    private String boardId;

    private String boardName;

    private String boardType;

    private String sortBy;

    private String sortDirection;

    public PageRequest toPageRequest() {
        return PageRequest.builder().page(Math.max(0, page - 1)).size(size).build();
    }
}

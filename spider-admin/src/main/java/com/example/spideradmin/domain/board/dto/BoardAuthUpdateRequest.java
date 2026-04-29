package com.example.spideradmin.domain.board.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardAuthUpdateRequest {

    @NotBlank(message = "권한 코드는 필수입니다")
    private String authCode;
}

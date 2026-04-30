package com.example.spideradmin.domain.bizapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Biz App 수정 요청 DTO (bizAppId는 PathVariable로 전달) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BizAppUpdateRequest {

    @NotBlank(message = "Biz App 명은 필수입니다")
    @Size(max = 100, message = "Biz App 명은 100자 이하여야 합니다")
    private String bizAppName;

    @Size(max = 500, message = "Biz App 설명은 500자 이하여야 합니다")
    private String bizAppDesc;

    @NotBlank(message = "이중처리 체크여부는 필수입니다")
    private String dupCheckYn;

    @NotBlank(message = "실행 큐는 필수입니다")
    private String queName;

    @NotBlank(message = "로그여부는 필수입니다")
    private String logYn;
}

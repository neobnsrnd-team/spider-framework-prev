package com.example.spider_admin.domain.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 프로퍼티 백업 요청 DTO
 */
@Getter
@Setter
public class PropertyBackupRequest {

    /**
     * 백업 사유
     */
    @NotBlank(message = "백업 사유를 입력해주세요.")
    @Size(max = 500, message = "백업 사유는 500자를 초과할 수 없습니다.")
    private String reason;
}

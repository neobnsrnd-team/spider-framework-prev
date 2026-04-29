package com.example.spider_admin.domain.wasproperty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * WAS 프로퍼티 백업 요청 DTO
 */
@Getter
@Setter
public class WasPropertyBackupRequest {

    @NotEmpty(message = "백업할 인스턴스를 선택해주세요.")
    private List<String> instanceIds;

    @NotBlank(message = "백업 사유를 입력해주세요.")
    @Size(max = 500, message = "백업 사유는 500자를 초과할 수 없습니다.")
    private String reason;
}

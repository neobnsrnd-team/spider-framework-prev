package com.example.spider_admin.domain.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 서비스 USE_YN 일괄 변경 요청 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FwkServiceUseYnBulkRequest {

    @NotEmpty(message = "서비스 ID 목록은 필수입니다")
    private List<String> serviceIds;

    @NotBlank(message = "사용여부는 필수입니다")
    @Size(max = 1)
    private String useYn;
}

package com.example.spideradmin.domain.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 개별 프로퍼티 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyCreateRequest {

    @NotBlank(message = "프로퍼티 ID는 필수입니다.")
    @Size(max = 100, message = "프로퍼티 ID는 100자 이하여야 합니다.")
    private String propertyId;

    @Size(max = 200, message = "프로퍼티명은 200자 이하여야 합니다.")
    private String propertyName;

    @Size(max = 500, message = "프로퍼티 설명은 500자 이하여야 합니다.")
    private String propertyDesc;

    @Size(max = 1, message = "데이터 타입은 1자여야 합니다.")
    private String dataType;

    @Size(max = 500, message = "유효 데이터는 500자 이하여야 합니다.")
    private String validData;

    @Size(max = 500, message = "기본값은 500자 이하여야 합니다.")
    private String defaultValue;
}

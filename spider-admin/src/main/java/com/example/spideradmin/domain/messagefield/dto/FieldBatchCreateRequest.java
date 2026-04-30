package com.example.spideradmin.domain.messagefield.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MessageField 일괄 생성 요청 DTO
 * 여러 필드를 한 번에 생성할 때 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldBatchCreateRequest {

    @NotNull(message = "필드 목록은 필수입니다")
    @NotEmpty(message = "최소 하나 이상의 필드가 필요합니다")
    @Valid
    private List<FieldCreateRequest> fields;
}

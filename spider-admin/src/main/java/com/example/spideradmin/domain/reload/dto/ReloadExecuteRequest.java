package com.example.spideradmin.domain.reload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import lombok.*;

/**
 * Reload 실행 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReloadExecuteRequest {

    @NotBlank(message = "Reload 타입은 필수입니다.")
    private String reloadType;

    @NotEmpty(message = "Reload할 WAS 인스턴스를 선택해주세요.")
    private List<String> instanceIds;

    /** gubun별 추가 파라미터 (trxId, messageId, orgId 등) */
    private Map<String, String> additionalParams;
}

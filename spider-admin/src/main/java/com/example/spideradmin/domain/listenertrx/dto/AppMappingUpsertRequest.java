package com.example.spideradmin.domain.listenertrx.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 요청처리 App 맵핑 등록/수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppMappingUpsertRequest {

    @NotBlank(message = "게이트웨이는 필수입니다.")
    private String gwId;

    @NotBlank(message = "전문식별자는 필수입니다.")
    private String reqIdCode;

    private String trxId;

    private String orgId;

    private String ioType;

    private String bizAppId;
}

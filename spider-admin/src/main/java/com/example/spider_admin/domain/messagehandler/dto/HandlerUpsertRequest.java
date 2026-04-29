package com.example.spider_admin.domain.messagehandler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HandlerUpsertRequest {

    @NotBlank(message = "기관ID는 필수입니다")
    @Size(max = 10, message = "기관ID는 10자 이내여야 합니다")
    private String orgId;

    @NotBlank(message = "거래유형은 필수입니다")
    @Size(max = 1, message = "거래유형은 1자여야 합니다")
    private String trxType;

    @NotBlank(message = "어댑터/리스너 구분은 필수입니다")
    @Size(max = 1, message = "어댑터/리스너 구분은 1자여야 합니다")
    private String ioType;

    @NotBlank(message = "운영모드는 필수입니다")
    @Size(max = 1, message = "운영모드는 1자여야 합니다")
    private String operModeType;

    @NotBlank(message = "전문처리핸들러는 필수입니다")
    @Size(max = 200, message = "전문처리핸들러는 200자 이내여야 합니다")
    private String handler;

    @Size(max = 2000, message = "전문처리핸들러설명은 2000자 이내여야 합니다")
    private String handlerDesc;

    @Size(max = 1, message = "상태값은 1자여야 합니다")
    private String stopYn;
}

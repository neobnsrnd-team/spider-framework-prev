package com.example.spideradmin.domain.transaction.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 거래중지 일괄 중지/시작 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxStopBatchRequest {

    @NotEmpty(message = "거래 ID 목록은 필수입니다")
    private List<String> trxIds;

    @NotNull(message = "중지여부는 필수입니다")
    private String trxStopYn;

    private String trxStopReason;
}

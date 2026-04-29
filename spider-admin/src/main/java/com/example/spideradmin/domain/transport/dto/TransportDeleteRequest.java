package com.example.spideradmin.domain.transport.dto;

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
public class TransportDeleteRequest {

    @NotBlank(message = "기관ID는 필수입니다")
    @Size(max = 10, message = "기관ID는 10자 이내여야 합니다")
    private String orgId;

    @NotBlank(message = "거래유형은 필수입니다")
    @Size(max = 1, message = "거래유형은 1자여야 합니다")
    private String trxType;

    @NotBlank(message = "어댑터/리스너 구분은 필수입니다")
    @Size(max = 1, message = "어댑터/리스너 구분은 1자여야 합니다")
    private String ioType;

    @NotBlank(message = "요청/응답 구분은 필수입니다")
    @Size(max = 1, message = "요청/응답 구분은 1자여야 합니다")
    private String reqResType;
}

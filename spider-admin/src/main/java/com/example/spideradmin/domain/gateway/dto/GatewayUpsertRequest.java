package com.example.spideradmin.domain.gateway.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayUpsertRequest {

    @NotBlank(message = "Gateway ID는 필수입니다")
    @Size(max = 20, message = "Gateway ID는 20자 이내여야 합니다")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Gateway ID는 영문/숫자/언더스코어만 허용합니다")
    private String gwId;

    @NotBlank(message = "Gateway 명은 필수입니다")
    @Size(max = 200, message = "Gateway 명은 200자 이내여야 합니다")
    private String gwName;

    @NotNull(message = "THREAD 수는 필수입니다")
    @Min(value = 1, message = "THREAD 수는 1 이상이어야 합니다")
    @Max(value = 999, message = "THREAD 수는 999 이하여야 합니다")
    private Integer threadCount;

    @Size(max = 500, message = "Gateway 속성은 500자 이내여야 합니다")
    private String gwProperties;

    @Size(max = 1000, message = "Gateway 설명은 1000자 이내여야 합니다")
    private String gwDesc;

    @Size(max = 200, message = "APP명은 200자 이내여야 합니다")
    private String gwAppName;

    @NotBlank(message = "송/수신 구분은 필수입니다")
    @Size(max = 1, message = "송/수신 구분은 1자여야 합니다")
    private String ioType;
}

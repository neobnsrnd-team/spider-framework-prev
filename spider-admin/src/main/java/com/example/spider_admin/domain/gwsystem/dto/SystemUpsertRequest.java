package com.example.spider_admin.domain.gwsystem.dto;

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
public class SystemUpsertRequest {

    @Size(max = 20, message = "Gateway ID는 20자 이내여야 합니다")
    private String gwId;

    @NotBlank(message = "System ID는 필수입니다")
    @Size(max = 20, message = "System ID는 20자 이내여야 합니다")
    private String systemId;

    @NotBlank(message = "운영모드는 필수입니다")
    @Size(max = 1, message = "운영모드는 1자여야 합니다")
    private String operModeType;

    @NotBlank(message = "IP는 필수입니다")
    @Size(max = 15, message = "IP는 15자 이내여야 합니다")
    private String ip;

    @NotBlank(message = "PORT는 필수입니다")
    @Size(max = 5, message = "PORT는 5자 이내여야 합니다")
    private String port;

    @Size(max = 1, message = "상태값은 1자여야 합니다")
    private String stopYn;

    @Size(max = 1000, message = "설명은 1000자 이내여야 합니다")
    private String systemDesc;

    @Size(max = 100, message = "WAS는 100자 이내여야 합니다")
    private String appliedWasInstance;
}

package com.example.spideradmin.domain.wasinstance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasInstanceRequest {

    @NotBlank(message = "인스턴스 ID는 필수입니다.")
    @Size(max = 4, message = "인스턴스 ID는 4자 이내여야 합니다.")
    private String instanceId;

    @Size(max = 50, message = "인스턴스명은 50자 이내여야 합니다.")
    private String instanceName;

    @Size(max = 200, message = "인스턴스 설명은 200자 이내여야 합니다.")
    private String instanceDesc;

    @Size(max = 10, message = "WAS 설정 ID는 10자 이내여야 합니다.")
    private String wasConfigId;

    @Size(max = 1, message = "인스턴스 구분은 1자 이내여야 합니다.")
    private String instanceType;

    @Size(max = 15, message = "IP 주소는 15자 이내여야 합니다.")
    @Pattern(regexp = "(^$|^(\\d{1,3}\\.){3}\\d{1,3}$)", message = "올바른 IP 주소 형식이 아닙니다.")
    private String ip;

    @Size(max = 5, message = "포트 번호는 5자 이내여야 합니다.")
    @Pattern(regexp = "(^$|^\\d+$)", message = "포트 번호는 숫자만 입력 가능합니다.")
    private String port;

    @Size(max = 1, message = "운영 모드 구분은 1자 이내여야 합니다.")
    @Pattern(regexp = "(^$|^[TRD]$)", message = "운영 모드는 T(테스트), R(운영), D(개발) 중 하나여야 합니다.")
    private String operModeType;
}

package com.example.spideradmin.domain.gwsystem.dto;

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
public class SystemDeleteRequest {

    @NotBlank(message = "Gateway ID는 필수입니다")
    @Size(max = 20, message = "Gateway ID는 20자 이내여야 합니다")
    private String gwId;

    @NotBlank(message = "System ID는 필수입니다")
    @Size(max = 20, message = "System ID는 20자 이내여야 합니다")
    private String systemId;
}

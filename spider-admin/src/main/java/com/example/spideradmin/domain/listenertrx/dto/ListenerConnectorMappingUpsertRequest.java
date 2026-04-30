package com.example.spideradmin.domain.listenertrx.dto;

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
public class ListenerConnectorMappingUpsertRequest {

    @NotBlank(message = "리스너 Gateway ID는 필수입니다")
    @Size(max = 20, message = "리스너 Gateway ID는 20자 이내여야 합니다")
    private String listenerGwId;

    @NotBlank(message = "리스너 System ID는 필수입니다")
    @Size(max = 20, message = "리스너 System ID는 20자 이내여야 합니다")
    private String listenerSystemId;

    @NotBlank(message = "Identifier는 필수입니다")
    @Size(max = 100, message = "Identifier는 100자 이내여야 합니다")
    private String identifier;

    @NotBlank(message = "커넥터 Gateway ID는 필수입니다")
    @Size(max = 20, message = "커넥터 Gateway ID는 20자 이내여야 합니다")
    private String connectorGwId;

    @NotBlank(message = "커넥터 System ID는 필수입니다")
    @Size(max = 20, message = "커넥터 System ID는 20자 이내여야 합니다")
    private String connectorSystemId;

    @Size(max = 200, message = "설명은 200자 이내여야 합니다")
    private String description;
}

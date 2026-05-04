package com.example.spideradmin.domain.listenertrx.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListenerConnectorMappingBatchRequest {

    @NotEmpty(message = "매핑 목록은 최소 1개 이상이어야 합니다")
    @Valid
    private List<ListenerConnectorMappingUpsertRequest> mappings;
}

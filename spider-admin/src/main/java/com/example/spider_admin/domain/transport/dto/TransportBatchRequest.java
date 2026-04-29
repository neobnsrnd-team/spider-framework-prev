package com.example.spider_admin.domain.transport.dto;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportBatchRequest {

    @Valid
    private List<TransportUpsertRequest> upserts;

    @Valid
    private List<TransportDeleteRequest> deletes;
}

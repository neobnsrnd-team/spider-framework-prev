package com.example.spideradmin.domain.messagehandler.dto;

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
public class HandlerBatchRequest {

    @Valid
    private List<HandlerUpsertRequest> upserts;

    @Valid
    private List<HandlerDeleteRequest> deletes;
}

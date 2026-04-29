package com.example.spider_admin.domain.gwsystem.dto;

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
public class SystemBatchRequest {

    @Valid
    private List<SystemUpsertRequest> upserts;

    @Valid
    private List<SystemDeleteRequest> deletes;
}

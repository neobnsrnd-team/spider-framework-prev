package com.example.spideradmin.domain.org.dto;

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
public class OrgBatchRequest {

    @Valid
    private List<OrgUpsertRequest> upserts;

    private List<String> deleteOrgIds;
}

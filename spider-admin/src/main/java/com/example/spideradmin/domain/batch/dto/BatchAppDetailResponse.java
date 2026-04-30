package com.example.spideradmin.domain.batch.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for BatchApp Detail Response
 * Contains batch app info with WAS instance assignments
 * Used to populate the batch app edit modal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAppDetailResponse {

    /**
     * Batch app basic info
     */
    private BatchAppResponse batchApp;

    /**
     * Left panel: WAS instances assigned to this batch app
     */
    private List<WasInstanceSelectionResponse> assignedInstances;

    /**
     * Right panel: All available WAS instances with assignment status
     */
    private List<WasInstanceSelectionResponse> allInstances;
}

package com.example.spideradmin.domain.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for WAS Instance Selection
 * Represents WAS instance with assignment status
 * Used in batch app modal for left-right transfer UI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WasInstanceSelectionResponse {

    /**
     * Instance ID
     */
    private String instanceId;

    /**
     * Instance name
     */
    private String instanceName;

    /**
     * Instance description
     */
    private String instanceDesc;

    /**
     * IP address
     */
    private String ip;

    /**
     * Port number
     */
    private String port;

    /**
     * Instance type
     */
    private String instanceType;

    /**
     * Whether this instance is assigned to the batch app
     */
    private Boolean isAssigned;

    /**
     * Use Y/N (if assigned)
     */
    private String useYn;
}

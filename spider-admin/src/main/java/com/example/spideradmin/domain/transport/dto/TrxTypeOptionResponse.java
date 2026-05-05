package com.example.spideradmin.domain.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxTypeOptionResponse {

    private String trxType;
    private String trxTypeName;
}

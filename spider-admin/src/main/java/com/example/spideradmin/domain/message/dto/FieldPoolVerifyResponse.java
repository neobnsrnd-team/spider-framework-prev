package com.example.spideradmin.domain.message.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldPoolVerifyResponse {
    private String fieldDomainId;
    private String domainRegistryYn;
    private String messageFieldId;
    private String fieldRegistryYn;
}

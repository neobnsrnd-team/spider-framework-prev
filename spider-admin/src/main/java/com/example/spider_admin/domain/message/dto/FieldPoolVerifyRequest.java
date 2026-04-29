package com.example.spider_admin.domain.message.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldPoolVerifyRequest {
    private List<String> messageFieldIds;
}

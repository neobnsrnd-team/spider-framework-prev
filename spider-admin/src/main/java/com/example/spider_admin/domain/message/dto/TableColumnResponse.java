package com.example.spider_admin.domain.message.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableColumnResponse {
    private String columnName;
    private String dataType;
    private Integer dataLength;
    private String comments;
}

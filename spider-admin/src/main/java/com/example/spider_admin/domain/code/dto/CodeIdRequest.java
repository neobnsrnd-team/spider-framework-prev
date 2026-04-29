package com.example.spider_admin.domain.code.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeIdRequest implements Serializable {

    private String codeGroupId;

    private String code;
}

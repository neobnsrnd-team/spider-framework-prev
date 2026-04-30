package com.example.spideradmin.domain.sqlquery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlQueryUpdateRequest {

    @NotBlank(message = "Query 명은 필수입니다")
    @Size(max = 200, message = "Query 명은 200자 이하여야 합니다")
    private String queryName;

    @Size(max = 50, message = "SQL Group ID는 50자 이하여야 합니다")
    private String sqlGroupId;

    @Size(max = 50, message = "DB ID는 50자 이하여야 합니다")
    private String dbId;

    private String sqlType;
    private String execType;
    private String cacheYn;
    private String timeOut;
    private String resultType;
    private String useYn;

    @Size(max = 4000, message = "SQL Query는 4000자 이하여야 합니다")
    private String sqlQuery;

    @Size(max = 4000, message = "SQL Query2는 4000자 이하여야 합니다")
    private String sqlQuery2;

    @Size(max = 500, message = "설명은 500자 이하여야 합니다")
    private String queryDesc;
}

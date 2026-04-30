package com.example.spideradmin.domain.sqlquery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SqlQueryResponse {

    private String queryId;
    private String queryName;
    private String sqlGroupId;
    private String sqlGroupName;
    private String dbId;
    private String dbName;
    private String sqlType;
    private String execType;
    private String cacheYn;
    private String timeOut;
    private String resultType;
    private String useYn;
    private String sqlQuery;
    private String sqlQuery2;
    private String queryDesc;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}

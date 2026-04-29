package com.example.spider_admin.domain.sqlquery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** FWK_SQL_GROUP 검색 응답 DTO (SQL 그룹 ID autocomplete용) */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SqlGroupResponse {

    private String groupId;
    private String groupName;
}

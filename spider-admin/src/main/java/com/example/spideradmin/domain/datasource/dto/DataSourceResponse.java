package com.example.spideradmin.domain.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 데이터소스 응답 DTO
 * DB_PASSWORD 는 항상 "****" 로 마스킹되어 반환된다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSourceResponse {

    private String dbId;
    private String dbName;
    private String dbDesc;
    private String connectionUrl;
    private String driverClass;
    private String dbUserId;

    /**
     * 비밀번호 — 항상 "****" 마스킹 값으로 반환 (평문 노출 금지)
     */
    private String dbPassword;

    private String jndiYn;
    private String jndiId;
    private String jndiProviderUrl;
    private String jndiContextFactory;
    private String lastUpdateDtime;
    private String lastUpdateUserId;
}

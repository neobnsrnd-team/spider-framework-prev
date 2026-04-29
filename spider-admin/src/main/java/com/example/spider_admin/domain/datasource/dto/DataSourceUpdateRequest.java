package com.example.spider_admin.domain.datasource.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 데이터소스 수정 요청 DTO
 * dbId는 PathVariable로 전달
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceUpdateRequest {

    @Size(max = 100, message = "DB 명은 100자 이하여야 합니다")
    private String dbName;

    @Size(max = 300, message = "DB 설명은 300자 이하여야 합니다")
    private String dbDesc;

    @Size(max = 500, message = "접속 URL은 500자 이하여야 합니다")
    private String connectionUrl;

    @Size(max = 200, message = "드라이버 클래스명은 200자 이하여야 합니다")
    private String driverClass;

    @Size(max = 100, message = "DB 사용자 ID는 100자 이하여야 합니다")
    private String dbUserId;

    /**
     * DB 비밀번호 — null 또는 빈 문자열이면 기존 값 유지
     */
    @Size(max = 200, message = "DB 비밀번호는 200자 이하여야 합니다")
    private String dbPassword;

    private String jndiYn;

    @Size(max = 100, message = "JNDI ID는 100자 이하여야 합니다")
    private String jndiId;

    @Size(max = 200, message = "JNDI Provider URL은 200자 이하여야 합니다")
    private String jndiProviderUrl;

    @Size(max = 200, message = "JNDI Context Factory는 200자 이하여야 합니다")
    private String jndiContextFactory;
}

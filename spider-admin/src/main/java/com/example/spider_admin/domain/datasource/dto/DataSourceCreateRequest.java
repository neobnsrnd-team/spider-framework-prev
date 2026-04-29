package com.example.spider_admin.domain.datasource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 데이터소스 등록 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceCreateRequest {

    @NotBlank(message = "DB ID는 필수입니다")
    @Size(max = 50, message = "DB ID는 50자 이하여야 합니다")
    private String dbId;

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

    @Size(max = 200, message = "DB 비밀번호는 200자 이하여야 합니다")
    private String dbPassword;

    @Builder.Default
    private String jndiYn = "N";

    @Size(max = 100, message = "JNDI ID는 100자 이하여야 합니다")
    private String jndiId;

    @Size(max = 200, message = "JNDI Provider URL은 200자 이하여야 합니다")
    private String jndiProviderUrl;

    @Size(max = 200, message = "JNDI Context Factory는 200자 이하여야 합니다")
    private String jndiContextFactory;
}

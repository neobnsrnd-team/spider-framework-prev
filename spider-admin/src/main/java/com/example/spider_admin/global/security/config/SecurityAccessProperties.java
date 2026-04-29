package com.example.spider_admin.global.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityAccessProperties {

    private String rememberMeKey;
    private String authoritySource = "user-menu";
    private int maxLoginFailCount = 5;
}

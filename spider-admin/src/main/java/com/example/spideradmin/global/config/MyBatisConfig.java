package com.example.spideradmin.global.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Configuration
 * Mapper 인터페이스 스캔 설정
 *
 * MyBatis 설정은 application-{profile}.yml의 mybatis.* 속성으로 관리됩니다.
 * - mybatis.mapper-locations: Mapper XML 경로
 * - mybatis.type-aliases-package: Type Aliases 패키지
 * - mybatis.configuration.*: MyBatis 세부 설정
 */
@Configuration
@MapperScan({"com.example.spideradmin.domain.**.mapper", "com.example.spideradmin.global.security.mapper"})
public class MyBatisConfig {}

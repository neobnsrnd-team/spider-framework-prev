package com.example.reactplatform.domain.reactgenerate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @file ReactGenerateEntity.java
 * @description FWK_REACT_CODE_HIS 테이블 INSERT 전용 엔티티.
 *
 * <p>Mapper insert()에서 다수의 @Param 대신 단일 객체를 전달하기 위해 사용한다.
 * brand·domain·componentName 등 구조화된 필드를 각각의 전용 컬럼에 저장한다.
 *
 * @see ReactGenerateMapper#insert(ReactGenerateEntity)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactGenerateEntity {

    private String codeId;
    private String figmaUrl;

    /** Figma API 응답 JSON 원본. */
    private String figmaJson;

    /** 서비스 도메인 (DomainType enum name, 예: BANKING). */
    private String domain;

    /** 브랜드 (BrandType enum name, 예: HANA). */
    private String brand;

    /** React 컴포넌트 식별자 (PascalCase, 예: LoginForm). */
    private String componentName;

    /** 화면 제목 (사람이 읽는 이름, 예: 로그인 폼). */
    private String title;

    /** 화면 목적 분류 (AUTH / MAIN / LIST / DETAIL / FORM / MYPAGE / ADMIN / EVENT / ERROR). */
    private String category;

    /** 화면 설명. */
    private String description;

    /**
     * 사용자 추가 요구사항.
     * 최초 생성 시 추가 요구사항, 재생성(REF_CODE_ID 존재) 시 변경 요청사항.
     */
    private String requirements;

    /** Claude에게 전달한 시스템 프롬프트 (감사·디버깅용). */
    private String systemPrompt;

    /** Claude에게 전달한 유저 프롬프트 (감사·디버깅용). */
    private String userPrompt;

    /** Claude가 생성한 React 코드. */
    private String reactCode;

    /** 생성 실패 사유 (STATUS=FAILED 일 때). */
    private String failReason;

    /** 생성 상태 (GENERATED / FAILED / PENDING_APPROVAL / APPROVED / REJECTED). */
    private String status;

    /** 생성 요청자 ID. */
    private String createUserId;

    /** 생성 일시 (yyyyMMddHHmmss). */
    private String createDtime;

    /** 재생성 직계 부모 CODE_ID. 최초 생성이면 null. */
    private String refCodeId;

    /** 재생성 체인 최상위 CODE_ID. 최초 생성이면 null. */
    private String rootCodeId;
}

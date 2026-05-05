package com.example.reactplatform.domain.reactgenerate.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactGenerateResponse {

    private String codeId;

    /** 화면 제목. */
    private String title;

    /** 화면 목적 분류 (AUTH/MAIN/LIST/DETAIL/FORM/MYPAGE/ADMIN/EVENT/ERROR). */
    private String category;

    /** 화면 설명. */
    private String description;

    private String figmaUrl;

    /** Figma API 응답 JSON 원본. 재생성 시 Figma API 재호출 없이 재사용한다. */
    private String figmaJson;

    /** 브랜드 (BrandType enum name). */
    private String brand;

    /** 서비스 도메인 (DomainType enum name). */
    private String domain;

    /** React 컴포넌트 식별자 (PascalCase). */
    private String componentName;

    /** 사용자 추가 요구사항. */
    private String requirements;

    /** 재생성 직계 부모 CODE_ID. 최초 생성이면 null. */
    private String refCodeId;

    /** 재생성 체인 최상위 CODE_ID. 최초 생성이면 null. */
    private String rootCodeId;

    private String reactCode;
    private String failReason;
    private String status;
    private String createUserId;
    private String createDtime;
    private String approvalUserId;
    private String approvalDtime;

    /** WARN 레벨 보안 패턴 탐지 목록 — 코드는 통과하되 프론트엔드에 경고로 표시 */
    private List<String> validationWarnings;
}

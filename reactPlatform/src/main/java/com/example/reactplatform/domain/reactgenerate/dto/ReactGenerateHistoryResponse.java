package com.example.reactplatform.domain.reactgenerate.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * React 코드 생성 이력 목록 조회 응답 DTO.
 *
 * <p>목록 화면에서 행 1건을 표현한다. CLOB 컬럼(REACT_CODE, REQUIREMENTS 등)은
 * 크기가 클 수 있으므로 목록 쿼리에서 제외하고 단건 조회({@link ReactGenerateResponse})
 * 시에만 가져온다.
 *
 * <p>MyBatis resultType 매핑을 위해 Setter가 필요하다.
 */
@Getter
@Setter
public class ReactGenerateHistoryResponse {

    private String codeId;

    /** 코드 생성 시 입력한 Figma URL. 목록에서는 말줄임 처리된다. */
    private String figmaUrl;

    /** 현재 처리 상태 (GENERATED / PENDING_APPROVAL / APPROVED / FAILED). */
    private String status;

    /** 화면 제목 (사람이 읽는 이름). */
    private String title;

    /** 브랜드 (BrandType enum name). */
    private String brand;

    /** 서비스 도메인 (DomainType enum name). */
    private String domain;

    /** 화면 목적 분류 (AUTH / MAIN / LIST / DETAIL 등). */
    private String category;

    /** React 컴포넌트 식별자 (PascalCase). */
    private String componentName;

    /** 코드 생성 요청자 ID. */
    private String createUserId;

    /** 생성 일시 (yyyyMMddHHmmss). 화면 표시 시 yyyy-MM-dd HH:mm으로 변환한다. */
    private String createDtime;

    /** 승인자 ID. 미승인 상태이면 null. */
    private String approvalUserId;

    /** 승인 일시 (yyyyMMddHHmmss). 미승인 상태이면 null. */
    private String approvalDtime;
}

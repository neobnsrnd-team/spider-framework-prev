package com.example.reactplatform.domain.reactgenerate.dto;

import com.example.reactplatform.domain.reactgenerate.enums.BrandType;
import com.example.reactplatform.domain.reactgenerate.enums.DomainType;
import com.example.reactplatform.domain.reactgenerate.enums.ReactGenerateCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file ReactGenerateRequest.java
 * @description React 코드 생성 API 요청 DTO.
 *     brand·domain은 Claude가 globals.css의 올바른 디자인 토큰 블록을 선택하는 데 사용된다.
 *     componentName은 생성될 React 컴포넌트 함수명을 명시적으로 지정한다 (미입력 시 Figma 컴포넌트명으로 결정).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactGenerateRequest {

    /** 화면 제목 (사람이 읽는 이름, 예: 로그인 폼). */
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자 이내로 입력해주세요.")
    private String title;

    /** 화면 목적 분류. FWK_REACT_CODE_HIS의 CHK_REACT_CATEGORY 제약과 일치해야 한다. */
    private ReactGenerateCategory category;

    /** 화면 설명. */
    @Size(max = 2000, message = "설명은 2000자 이내로 입력해주세요.")
    private String description;

    /** 유효한 Figma design/file URL. node-id 쿼리 파라미터를 포함해야 한다. */
    @NotBlank(message = "Figma URL을 입력해주세요.")
    @Pattern(
            regexp = "https://www\\.figma\\.com/(design|file)/[A-Za-z0-9]+/.+[?&]node-id=[^&]+.*",
            message = "유효하지 않은 Figma URL 형식입니다. (예: https://www.figma.com/design/...?node-id=1-2)")
    private String figmaUrl;

    /** 적용할 금융 브랜드. globals.css의 [data-brand] 토큰 블록 선택에 사용된다. */
    @NotNull(message = "brand를 선택해주세요.")
    private BrandType brand;

    /**
     * 적용할 금융 도메인. 미입력 시 서비스 레이어에서 BANKING을 기본값으로 적용한다.
     * globals.css의 [data-domain] 토큰 블록 선택에 사용된다.
     */
    private DomainType domain;

    /**
     * 생성할 React 컴포넌트 함수명 (PascalCase). 미입력 시 Figma 컴포넌트명을 기반으로 결정한다.
     * Container scaffold 파일명도 이 값을 기준으로 생성되므로, 여러 코드 생성 시 충돌 방지를 위해 입력을 권장한다.
     */
    @Pattern(
            regexp = "^[A-Z][A-Za-z0-9]*$",
            message = "컴포넌트명은 PascalCase로 입력해주세요. (예: LoginPage, TransferForm)")
    private String componentName;

    /**
     * Claude에게 전달하는 추가 요구사항 (자유 텍스트).
     * 재생성 시에는 변경 요청사항을 담는다.
     */
    private String requirements;
}

package com.example.reactplatform.domain.reactgenerate.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.reactplatform.domain.reactgenerate.enums.BrandType;
import com.example.reactplatform.domain.reactgenerate.enums.DomainType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @file ReactGenerateRequestTest.java
 * @description ReactGenerateRequest DTO 입력 검증 단위 테스트.
 *     title 필수 검증, figmaUrl 패턴 검증, brand 필수 검증, domain 선택 검증을 확인한다.
 */
class ReactGenerateRequestTest {

    private static Validator validator;

    /** 각 테스트에서 필수 필드(title, figmaUrl, brand)를 채운 기본 빌더 */
    private static final String VALID_FIGMA_URL =
            "https://www.figma.com/design/AbcDef123/MyDesign?node-id=1-2";

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("필수 필드를 모두 입력한 유효한 요청은 검증을 통과한다")
    void validRequest_passes() {
        ReactGenerateRequest request = ReactGenerateRequest.builder()
                .title("로그인 화면")
                .figmaUrl(VALID_FIGMA_URL)
                .brand(BrandType.HANA)
                .domain(DomainType.CARD)
                .build();

        Set<ConstraintViolation<ReactGenerateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("domain 미입력 시 검증을 통과한다 (서비스에서 BANKING 기본값 적용)")
    void nullDomain_passes() {
        ReactGenerateRequest request = ReactGenerateRequest.builder()
                .title("메인 화면")
                .figmaUrl(VALID_FIGMA_URL)
                .brand(BrandType.KB)
                .domain(null)
                .build();

        Set<ConstraintViolation<ReactGenerateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("title이 비어있으면 검증 실패")
    void blankTitle_fails() {
        ReactGenerateRequest request = ReactGenerateRequest.builder()
                .title("")
                .figmaUrl(VALID_FIGMA_URL)
                .brand(BrandType.HANA)
                .build();

        Set<ConstraintViolation<ReactGenerateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    @DisplayName("title이 null이면 검증 실패")
    void nullTitle_fails() {
        ReactGenerateRequest request = ReactGenerateRequest.builder()
                .title(null)
                .figmaUrl(VALID_FIGMA_URL)
                .brand(BrandType.HANA)
                .build();

        Set<ConstraintViolation<ReactGenerateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    @DisplayName("figmaUrl이 비어있으면 검증 실패")
    void blankFigmaUrl_fails() {
        ReactGenerateRequest request = ReactGenerateRequest.builder()
                .title("로그인 화면")
                .figmaUrl("")
                .brand(BrandType.HANA)
                .build();

        Set<ConstraintViolation<ReactGenerateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("figmaUrl"));
    }

    @Test
    @DisplayName("figmaUrl이 허용 패턴에 맞지 않으면 검증 실패")
    void invalidFigmaUrlPattern_fails() {
        ReactGenerateRequest request = ReactGenerateRequest.builder()
                .title("로그인 화면")
                .figmaUrl("https://www.figma.com/proto/AbcDef123/MyDesign?node-id=1-2")
                .brand(BrandType.HANA)
                .build();

        Set<ConstraintViolation<ReactGenerateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("figmaUrl"));
    }

    @Test
    @DisplayName("figmaUrl이 Figma 도메인이 아니면 검증 실패")
    void nonFigmaUrl_fails() {
        ReactGenerateRequest request = ReactGenerateRequest.builder()
                .title("로그인 화면")
                .figmaUrl("https://www.example.com/design/AbcDef123/MyDesign")
                .brand(BrandType.HANA)
                .build();

        Set<ConstraintViolation<ReactGenerateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("figmaUrl"));
    }

    @Test
    @DisplayName("brand가 null이면 검증 실패")
    void nullBrand_fails() {
        ReactGenerateRequest request = ReactGenerateRequest.builder()
                .title("로그인 화면")
                .figmaUrl(VALID_FIGMA_URL)
                .brand(null)
                .build();

        Set<ConstraintViolation<ReactGenerateRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("brand"));
    }
}

package com.example.reactplatform.domain.reactgenerate.figma;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @file VariantNormalizerTest.java
 * @description VariantNormalizer 단위 테스트.
 *     키 정규화, 값 정규화, 컴포넌트별 오버라이드, TEXT 타입 보호를 검증한다.
 * @see VariantNormalizer
 */
class VariantNormalizerTest {

    private VariantNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new VariantNormalizer(new ObjectMapper());
        normalizer.load();
    }

    // ================================================================
    // 키 정규화 — 전역 매핑
    // ================================================================

    @Nested
    @DisplayName("전역 키 정규화")
    class GlobalKeyTests {

        @Test
        @DisplayName("Size → size로 정규화된다")
        void globalKey_size() {
            Map<String, String> result = normalize("Button",
                    "Size", "VARIANT", "Medium");

            assertThat(result).containsKey("size");
            assertThat(result).doesNotContainKey("Size");
        }

        @Test
        @DisplayName("Variant → variant로 정규화된다")
        void globalKey_variant() {
            Map<String, String> result = normalize("Button",
                    "Variant", "VARIANT", "Primary");

            assertThat(result).containsKey("variant");
        }

        @Test
        @DisplayName("Dot → dot으로 정규화된다")
        void globalKey_dot() {
            Map<String, String> result = normalize("Badge",
                    "Dot", "BOOLEAN", true);

            assertThat(result).containsKey("dot");
        }

        @Test
        @DisplayName("매핑에 없는 키는 원본 그대로 유지된다")
        void globalKey_unknown_keepAsIs() {
            Map<String, String> result = normalize("Button",
                    "customProp", "VARIANT", "value");

            assertThat(result).containsKey("customProp");
        }

        @Test
        @DisplayName("이미 camelCase인 키는 변환되지 않는다")
        void globalKey_alreadyCamelCase_unchanged() {
            Map<String, String> result = normalize("Button",
                    "isExpanded", "BOOLEAN", false);

            assertThat(result).containsKey("isExpanded");
        }
    }

    // ================================================================
    // 키 정규화 — 컴포넌트별 오버라이드
    // ================================================================

    @Nested
    @DisplayName("컴포넌트별 키 오버라이드")
    class ComponentKeyOverrideTests {

        @Test
        @DisplayName("Input의 State → validationState로 오버라이드된다")
        void componentKey_inputState_validationState() {
            Map<String, String> result = normalize("Input",
                    "State", "VARIANT", "Error");

            assertThat(result).containsKey("validationState");
            assertThat(result).doesNotContainKey("state");
        }

        @Test
        @DisplayName("Input 이름에 슬래시 포함돼도 오버라이드가 적용된다")
        void componentKey_inputWithSlash_resolves() {
            Map<String, String> result = normalize("Input / Default",
                    "State", "VARIANT", "Error");

            assertThat(result).containsKey("validationState");
        }

        @Test
        @DisplayName("Input이 아닌 컴포넌트의 State는 전역 매핑(state)으로 처리된다")
        void componentKey_nonInput_state_usesGlobal() {
            Map<String, String> result = normalize("Button",
                    "State", "VARIANT", "Loading");

            assertThat(result).containsKey("state");
            assertThat(result).doesNotContainKey("validationState");
        }

        @Test
        @DisplayName("컴포넌트별 오버라이드는 전역 매핑보다 우선 적용된다")
        void componentKey_overridePriority() {
            // Input: State → validationState (전역은 State → state)
            Map<String, String> inputResult = normalize("Input", "State", "VARIANT", "Default");
            Map<String, String> buttonResult = normalize("Button", "State", "VARIANT", "Default");

            assertThat(inputResult).containsKey("validationState");
            assertThat(buttonResult).containsKey("state");
        }
    }

    // ================================================================
    // 값 정규화
    // ================================================================

    @Nested
    @DisplayName("값 정규화")
    class ValueNormalizationTests {

        @Test
        @DisplayName("Size=Small → size=sm으로 정규화된다")
        void value_sizeSmall_sm() {
            Map<String, String> result = normalize("Button", "Size", "VARIANT", "Small");

            assertThat(result).containsEntry("size", "sm");
        }

        @Test
        @DisplayName("Size=Medium → size=md로 정규화된다")
        void value_sizeMedium_md() {
            Map<String, String> result = normalize("Button", "Size", "VARIANT", "Medium");

            assertThat(result).containsEntry("size", "md");
        }

        @Test
        @DisplayName("Size=Large → size=lg로 정규화된다")
        void value_sizeLarge_lg() {
            Map<String, String> result = normalize("Button", "Size", "VARIANT", "Large");

            assertThat(result).containsEntry("size", "lg");
        }

        @Test
        @DisplayName("Variant=Primary → variant=primary (lowercase fallback)")
        void value_variantPrimary_lowercase() {
            Map<String, String> result = normalize("Button", "Variant", "VARIANT", "Primary");

            assertThat(result).containsEntry("variant", "primary");
        }

        @Test
        @DisplayName("Variant=Outline → variant=outline")
        void value_variantOutline_lowercase() {
            Map<String, String> result = normalize("Button", "Variant", "VARIANT", "Outline");

            assertThat(result).containsEntry("variant", "outline");
        }

        @Test
        @DisplayName("BOOLEAN True → true (소문자)")
        void value_booleanTrue_lowercase() {
            Map<String, String> result = normalize("Badge", "Dot", "BOOLEAN", true);

            assertThat(result).containsEntry("dot", "true");
        }

        @Test
        @DisplayName("BOOLEAN False → false (소문자)")
        void value_booleanFalse_lowercase() {
            Map<String, String> result = normalize("Button", "Loading", "BOOLEAN", false);

            assertThat(result).containsEntry("loading", "false");
        }

        @Test
        @DisplayName("Input State=Error → validationState=error")
        void value_inputStateError_validationStateError() {
            Map<String, String> result = normalize("Input", "State", "VARIANT", "Error");

            assertThat(result).containsEntry("validationState", "error");
        }

        @Test
        @DisplayName("Input State=Default → validationState=default")
        void value_inputStateDefault_validationStateDefault() {
            Map<String, String> result = normalize("Input", "State", "VARIANT", "Default");

            assertThat(result).containsEntry("validationState", "default");
        }
    }

    // ================================================================
    // TEXT 타입 보호
    // ================================================================

    @Nested
    @DisplayName("TEXT 타입 props 보호")
    class TextPropTests {

        @Test
        @DisplayName("TEXT 타입 값은 소문자 변환 없이 원본 그대로 유지된다")
        void text_koreanLabel_unchanged() {
            Map<String, String> result = normalize("Button", "Label", "TEXT", "확인");

            assertThat(result).containsEntry("label", "확인");
        }

        @Test
        @DisplayName("TEXT 타입의 영문 값도 대소문자 변환 없이 유지된다")
        void text_englishLabel_casePreserved() {
            Map<String, String> result = normalize("Input", "Label", "TEXT", "Enter Password");

            assertThat(result).containsEntry("label", "Enter Password");
        }
    }

    // ================================================================
    // 빈 입력 처리
    // ================================================================

    @Test
    @DisplayName("빈 props 맵이 입력되면 빈 맵을 반환한다")
    void normalize_emptyProps_returnsEmptyMap() {
        Map<String, String> result = normalizer.normalize("Button", Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null props가 입력되면 빈 맵을 반환한다")
    void normalize_nullProps_returnsEmptyMap() {
        Map<String, String> result = normalizer.normalize("Button", null);

        assertThat(result).isEmpty();
    }

    // ================================================================
    // 복합 케이스
    // ================================================================

    @Test
    @DisplayName("여러 props가 한 번에 정규화된다")
    void normalize_multipleProps_allNormalized() {
        Map<String, FigmaNode.ComponentProperty> rawProps = new LinkedHashMap<>();
        rawProps.put("Variant", prop("VARIANT", "Primary"));
        rawProps.put("Size", prop("VARIANT", "Small"));
        rawProps.put("Loading", prop("BOOLEAN", true));
        rawProps.put("Label", prop("TEXT", "전송하기"));

        Map<String, String> result = normalizer.normalize("Button", rawProps);

        assertThat(result)
                .containsEntry("variant", "primary")
                .containsEntry("size", "sm")
                .containsEntry("loading", "true")
                .containsEntry("label", "전송하기");
    }

    // ================================================================
    // helpers
    // ================================================================

    /** 단일 prop으로 normalize 호출 */
    private Map<String, String> normalize(String nodeName, String key, String type, Object value) {
        Map<String, FigmaNode.ComponentProperty> rawProps = new LinkedHashMap<>();
        rawProps.put(key, prop(type, value));
        return normalizer.normalize(nodeName, rawProps);
    }

    private FigmaNode.ComponentProperty prop(String type, Object value) {
        FigmaNode.ComponentProperty p = new FigmaNode.ComponentProperty();
        p.setType(type);
        p.setValue(value);
        return p;
    }
}

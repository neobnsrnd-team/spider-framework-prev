package com.example.reactplatform.domain.reactgenerate.figma;

import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * @file VariantNormalizer.java
 * @description Figma INSTANCE 노드의 componentProperties를 React prop 형식으로 정규화하는 컴포넌트.
 *
 * <p>variant-mapping.json 설정 파일을 기반으로 동작하며, if-else 분기 없이 데이터 기반으로 변환을 수행한다.
 *
 * <p>변환 우선순위:
 * <ol>
 *   <li>컴포넌트 별 키 오버라이드 (예: Input의 State → validationState)</li>
 *   <li>전역 키 매핑 (예: Size → size, Variant → variant)</li>
 *   <li>매핑 없는 경우 원본 키 유지</li>
 * </ol>
 *
 * <p>값 변환 우선순위:
 * <ol>
 *   <li>TEXT 타입 prop은 변환하지 않는다 (사람이 읽는 레이블·placeholder)</li>
 *   <li>정규화된 키에 대한 값 테이블 조회 (예: size.Small → sm)</li>
 *   <li>default 전략 적용 — "lowercase" 가 기본값 (Primary → primary, True → true)</li>
 * </ol>
 *
 * @see FigmaDesignExtractor
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VariantNormalizer {

    private final ObjectMapper objectMapper;

    /** 전역 키 매핑: PascalCase Figma 속성명 → camelCase React prop명 */
    private Map<String, String> globalKeyMap = Collections.emptyMap();

    /** 컴포넌트별 키 오버라이드: 컴포넌트명(소문자) → (Figma 속성명 → React prop명) */
    private Map<String, Map<String, String>> componentKeyMap = Collections.emptyMap();

    /** 값 변환 테이블: 정규화된 키명 → (Figma 값 → React 값) */
    private Map<String, Map<String, String>> valueMap = Collections.emptyMap();

    /** 값 테이블에 없는 경우의 fallback 전략 — "lowercase" 고정 */
    private String valueDefaultStrategy = "lowercase";

    /**
     * 애플리케이션 시작 시 variant-mapping.json을 로드하여 매핑 테이블을 초기화한다.
     * 파일이 없거나 읽기 실패 시 모든 매핑을 빈 상태로 두어 정규화를 비활성화한다.
     */
    @PostConstruct
    public void load() {
        ClassPathResource resource = new ClassPathResource("variant-mapping.json");
        if (!resource.exists()) {
            log.warn("variant-mapping.json 없음 — VariantNormalizer 정규화 비활성화");
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(resource.getInputStream());
            globalKeyMap = parseStringMap(root.path("global").path("keys"));
            componentKeyMap = parseComponentMap(root.path("components"));
            valueMap = parseValueMap(root.path("values"));
            valueDefaultStrategy = root.path("values").path("default").asText("lowercase");
            log.info(
                    "VariantNormalizer 초기화 완료 — global keys={}개, components={}개",
                    globalKeyMap.size(),
                    componentKeyMap.size());
        } catch (IOException e) {
            log.error("variant-mapping.json 읽기 실패 — 정규화 비활성화", e);
        }
    }

    /**
     * Figma componentProperties를 React prop 형식으로 정규화하여 반환한다.
     *
     * <p>INSTANCE_SWAP 타입은 호출 전에 제거되어 있어야 한다.
     *
     * @param nodeName Figma 노드 이름 (예: "Input / Default", "Button")
     * @param rawProps INSTANCE_SWAP 제외 후 남은 componentProperties
     * @return 정규화된 prop 맵. 입력이 비어있으면 빈 맵 반환
     */
    public Map<String, String> normalize(
            String nodeName, Map<String, FigmaNode.ComponentProperty> rawProps) {
        if (rawProps == null || rawProps.isEmpty()) {
            return Collections.emptyMap();
        }

        String componentKey = resolveComponentKey(nodeName);
        Map<String, String> result = new LinkedHashMap<>();

        rawProps.forEach((rawKeyName, prop) -> {
            if (prop == null || prop.getType() == null || prop.getValue() == null) return;

            String normalizedKey = normalizeKey(componentKey, rawKeyName);
            String normalizedValue = normalizeValue(
                    prop.getType(), normalizedKey, String.valueOf(prop.getValue()));
            result.put(normalizedKey, normalizedValue);
        });

        return result;
    }

    /**
     * 노드 이름에서 컴포넌트 키를 추출한다.
     *
     * <p>"Input / Default" → "input", "Button" → "button"
     * 슬래시 앞의 첫 번째 토큰만 사용하고 소문자로 변환한다.
     */
    private String resolveComponentKey(String nodeName) {
        return nodeName.split("[/\\\\]")[0].trim().toLowerCase();
    }

    /**
     * 키 이름을 정규화한다.
     *
     * <ol>
     *   <li>컴포넌트별 오버라이드 우선 조회</li>
     *   <li>전역 키 매핑 조회</li>
     *   <li>둘 다 없으면 원본 유지 (이미 camelCase이거나 알 수 없는 키)</li>
     * </ol>
     */
    private String normalizeKey(String componentKey, String rawKey) {
        Map<String, String> compKeys = componentKeyMap.getOrDefault(componentKey, Collections.emptyMap());
        if (compKeys.containsKey(rawKey)) return compKeys.get(rawKey);
        return globalKeyMap.getOrDefault(rawKey, rawKey);
    }

    /**
     * 값을 정규화한다.
     *
     * <p>TEXT 타입은 변환하지 않는다 — 사람이 읽는 레이블·placeholder 등이 소문자로 변환되면 안 된다.
     * VARIANT·BOOLEAN은 값 테이블 조회 후 없으면 default 전략을 적용한다.
     */
    private String normalizeValue(String propType, String normalizedKey, String rawValue) {
        // TEXT prop은 원본 그대로 (예: "확인", "계좌 선택")
        if ("TEXT".equals(propType)) return rawValue;

        Map<String, String> valTable = valueMap.get(normalizedKey);
        if (valTable != null && valTable.containsKey(rawValue)) {
            return valTable.get(rawValue);
        }

        // default 전략: "lowercase" → 소문자 변환 (Primary→primary, True→true)
        if ("lowercase".equals(valueDefaultStrategy)) {
            return rawValue.toLowerCase();
        }
        return rawValue;
    }

    // ================================================================
    // JSON 파싱 헬퍼
    // ================================================================

    private Map<String, String> parseStringMap(JsonNode node) {
        Map<String, String> map = new LinkedHashMap<>();
        if (node == null || node.isMissingNode()) return map;
        node.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().asText()));
        return map;
    }

    private Map<String, Map<String, String>> parseComponentMap(JsonNode node) {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        if (node == null || node.isMissingNode()) return map;
        node.fields().forEachRemaining(compEntry ->
                map.put(compEntry.getKey(), parseStringMap(compEntry.getValue().path("keys"))));
        return map;
    }

    private Map<String, Map<String, String>> parseValueMap(JsonNode node) {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        if (node == null || node.isMissingNode()) return map;
        node.fields().forEachRemaining(e -> {
            // "default" 키는 문자열 전략이므로 맵 파싱 대상에서 제외
            if (!"default".equals(e.getKey()) && e.getValue().isObject()) {
                map.put(e.getKey(), parseStringMap(e.getValue()));
            }
        });
        return map;
    }
}

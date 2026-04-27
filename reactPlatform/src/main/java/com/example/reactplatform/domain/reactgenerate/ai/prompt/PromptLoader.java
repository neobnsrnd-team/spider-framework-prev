package com.example.reactplatform.domain.reactgenerate.ai.prompt;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Claude API system prompt에 포함할 마크다운 파일을 로드하는 컴포넌트.
 *
 * <p>파일은 classpath:prompts/ 디렉토리에서 읽으며, 애플리케이션 시작 시 한 번만 로드하여
 * 메모리에 캐싱한다. 파일이 없을 경우 빈 문자열을 반환하여 누락 파일로 인한 기동 실패를 방지한다.
 *
 * <p>파일 배치 경로: src/main/resources/prompts/
 * <ul>
 *   <li>page-generation-rules.md — 코드 생성 규칙 + Figma → React 컴포넌트 매핑 (generated/page-generation-rules.md)</li>
 *   <li>component-types.md — 컴포넌트 TypeScript 인터페이스 레퍼런스</li>
 *   <li>design-tokens.md — CSS 변수 토큰 레퍼런스</li>
 * </ul>
 */
@Slf4j
@Component
public class PromptLoader {

    private String pageGenerationRules = "";
    private String componentTypes = "";
    private String designTokens = "";

    /**
     * 애플리케이션 시작 시 prompts/ 디렉토리의 모든 파일을 메모리에 로드한다.
     * 파일이 없으면 경고 로그만 남기고 빈 문자열로 처리(graceful degradation).
     */
    @PostConstruct
    public void load() {
        pageGenerationRules = readOrEmpty("prompts/page-generation-rules.md");
        componentTypes = readOrEmpty("prompts/component-types.md");
        designTokens = readOrEmpty("prompts/design-tokens.md");

        log.info(
                "PromptLoader 초기화 완료 — page-generation-rules={}자, component-types={}자, design-tokens={}자",
                pageGenerationRules.length(),
                componentTypes.length(),
                designTokens.length());
    }

    public String loadPageGenerationRules() {
        return pageGenerationRules;
    }

    public String loadComponentTypes() {
        return componentTypes;
    }

    public String loadDesignTokens() {
        return designTokens;
    }

    /**
     * classpath 리소스를 UTF-8 문자열로 읽는다.
     * 파일이 존재하지 않거나 읽기 실패 시 빈 문자열을 반환한다.
     */
    private String readOrEmpty(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            log.warn("프롬프트 파일 없음 (건너뜀): {}", path);
            return "";
        }
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("프롬프트 파일 읽기 실패: {}", path, e);
            return "";
        }
    }
}

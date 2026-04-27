package com.example.reactplatform.domain.reactgenerate.ai.prompt;

import com.example.reactplatform.domain.reactgenerate.enums.BrandType;
import com.example.reactplatform.domain.reactgenerate.enums.DomainType;
import com.example.reactplatform.domain.reactgenerate.figma.FigmaDesignContext;
import com.example.reactplatform.domain.reactgenerate.figma.FigmaNodeSummary;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Claude API에 전달할 system prompt와 user prompt를 조립하는 컴포넌트.
 *
 * <p>PromptLoader에서 읽은 각 마크다운 섹션을 구분자(---)와 헤더로 연결하여
 * Claude가 컨텍스트를 명확히 구분할 수 있는 단일 문자열로 반환한다.
 *
 * <p>섹션 조립 순서: 역할 정의 → page-generation-rules → component-types → design-tokens
 *
 * <p>user prompt에는 Figma URL 텍스트 대신 {@link FigmaDesignContext}에서 추출한
 * 구조화된 레이아웃·색상·텍스트 정보를 ASCII 트리 형태로 포함한다.
 */
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final PromptLoader promptLoader;

    /**
     * Claude API의 system 필드에 전달할 프롬프트를 생성한다.
     *
     * <p>비어있는 섹션은 건너뛰므로, prompts/ 파일 일부가 없어도 동작한다.
     *
     * @return 섹션이 조합된 system prompt 문자열
     */
    public String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();

        sb.append("당신은 Figma 디자인을 React 컴포넌트로 변환하는 전문가입니다.\n");
        sb.append("반드시 아래에 제공된 컴포넌트 라이브러리만 사용하여 코드를 생성하세요.\n");
        sb.append("목록에 없는 컴포넌트를 임의로 만들거나 외부 라이브러리를 추가하지 마세요.\n");

        appendSection(sb, "page-generation-rules.md (코드 생성 규칙 + Figma → React 컴포넌트 매핑)", promptLoader.loadPageGenerationRules());
        appendSection(sb, "Component Library (사용 가능한 컴포넌트 인터페이스)", promptLoader.loadComponentTypes());
        appendSection(sb, "Design Tokens (CSS 변수 레퍼런스 — 하드코딩 금지)", promptLoader.loadDesignTokens());

        return sb.toString();
    }

    /**
     * Claude API의 messages[0].content 필드에 전달할 user prompt를 생성한다.
     *
     * <p>Figma URL 텍스트 대신 {@link FigmaDesignContext}의 구조화된 정보(크기, 레이아웃,
     * 색상, 텍스트, 타이포그래피, 그림자, 그라디언트 등)를 ASCII 트리 형태로 포함하여
     * Claude의 코드 생성 정확도를 높인다.
     *
     * <p>brand·domain은 프롬프트 앞단에 명시하여 Claude가 globals.css의
     * 올바른 [data-brand]/[data-domain] 토큰 블록을 선택하도록 안내한다.
     *
     * <p>componentName이 주어지면 해당 이름으로 export default function을 생성하도록 명시한다.
     * null이면 AI가 Figma 컴포넌트명을 기반으로 결정한다.
     *
     * @param context       Figma API에서 추출한 디자인 컨텍스트
     * @param brand         적용할 금융 브랜드
     * @param domain        적용할 금융 도메인
     * @param componentName 생성할 컴포넌트 함수명 (null이면 AI가 결정)
     * @return user prompt 문자열
     */
    public String buildUserPrompt(FigmaDesignContext context, BrandType brand, DomainType domain, String componentName) {
        StringBuilder sb = new StringBuilder();

        sb.append("Generate a React component from the following Figma design.\n\n");

        // 브랜드·도메인 토큰 지시 — Claude가 globals.css의 정확한 블록을 참조하도록 선행 주입
        String brandKey = brand.name().toLowerCase();
        String domainKey = domain.name().toLowerCase();
        sb.append("## Design Token Selection\n");
        sb.append("Brand: ")
                .append(brandKey)
                .append(" → [data-brand=\"")
                .append(brandKey)
                .append("\"] 토큰을 사용할 것\n");
        sb.append("Domain: ")
                .append(domainKey)
                .append(" → [data-domain=\"")
                .append(domainKey)
                .append("\"] 토큰을 사용할 것\n\n");

        // 최상위 래퍼 지시 — 10-brand.md 규칙: 모든 page.tsx 루트에 data-brand·data-domain 명시 필수
        sb.append("## Root Wrapper Rule\n");
        sb.append("생성하는 컴포넌트의 최상위 엘리먼트에 반드시 data-brand와 data-domain 속성을 지정할 것.\n");
        sb.append("- 루트 컴포넌트가 해당 prop을 지원하면 prop으로 전달\n");
        sb.append("- 지원하지 않으면 래퍼 div로 감쌀 것:\n");
        sb.append("  <div data-brand=\"")
                .append(brandKey)
                .append("\" data-domain=\"")
                .append(domainKey)
                .append("\">\n");
        sb.append("    {/* 컴포넌트 내용 */}\n");
        sb.append("  </div>\n\n");

        // Figma 디자인 컨텍스트 섹션
        sb.append("## Figma Design Context\n");
        sb.append("Component: ")
                .append(context.getComponentName())
                .append(" (")
                .append(context.getComponentType())
                .append(")\n");
        sb.append("Canvas Size: ")
                .append(context.getWidth())
                .append(" × ")
                .append(context.getHeight())
                .append(" px\n");
        sb.append("Layout: ")
                .append(describeLayoutMode(context.getLayoutMode()))
                .append("\n");
        sb.append("Figma URL: ").append(context.getFigmaUrl()).append("\n");

        // 하위 노드 트리 섹션
        if (context.getChildren() != null && !context.getChildren().isEmpty()) {
            sb.append("\n## Element Tree\n");
            sb.append("[")
                    .append(context.getComponentType())
                    .append("] ")
                    .append(context.getComponentName())
                    .append(" (")
                    .append(context.getWidth())
                    .append("×")
                    .append(context.getHeight())
                    .append("px, ")
                    .append(describeLayoutMode(context.getLayoutMode()))
                    .append(")\n");
            formatNodes(sb, context.getChildren(), "");
        }

        sb.append("\n## Rules\n");
        sb.append("- 반드시 위 컴포넌트 라이브러리의 컴포넌트만 사용할 것\n");
        sb.append("- import는 JSX에서 실제로 렌더링되는 컴포넌트만 포함할 것 (사용하지 않는 import 금지)\n");
        sb.append("- 디자인 토큰(CSS 변수)을 활용하고 색상·크기 하드코딩 금지\n");
        sb.append("- TypeScript로 작성하고 props interface를 포함할 것\n");
        sb.append("- 접근성(aria 속성)을 고려할 것\n");
        if (componentName != null && !componentName.isBlank()) {
            // 명시적 컴포넌트명이 주어진 경우 — AI가 임의로 다른 이름을 사용하지 않도록 강하게 지시
            sb.append("- 반드시 `export default function ")
                    .append(componentName)
                    .append("()` 형식으로 컴포넌트를 내보낼 것 (다른 이름 사용 금지)\n");
        } else {
            sb.append("- 반드시 `export default function ComponentName()` 형식으로 컴포넌트를 내보낼 것\n");
        }
        sb.append("- 응답은 ```tsx ... ``` 코드 블록 하나로만 작성할 것\n");

        return sb.toString();
    }

    /**
     * 하위 노드 목록을 ASCII 트리 형태로 재귀 출력한다.
     *
     * @param sb      결과를 추가할 StringBuilder
     * @param nodes   출력할 노드 목록
     * @param indent  현재 들여쓰기 문자열 (│ 또는 공백)
     */
    private void formatNodes(StringBuilder sb, List<FigmaNodeSummary> nodes, String indent) {
        if (nodes == null || nodes.isEmpty()) return;

        for (int i = 0; i < nodes.size(); i++) {
            boolean last = i == nodes.size() - 1;
            FigmaNodeSummary node = nodes.get(i);

            // 마지막 노드는 └─, 그 외는 ├─ 사용
            String connector = last ? "└─ " : "├─ ";
            // 자식 들여쓰기: 마지막이면 공백, 아니면 │로 연결
            String childIndent = indent + (last ? "   " : "│  ");

            sb.append(indent)
                    .append(connector)
                    .append(formatNodeLine(node))
                    .append("\n");

            formatNodes(sb, node.getChildren(), childIndent);
        }
    }

    /**
     * 단일 {@link FigmaNodeSummary}를 한 줄 문자열로 표현한다.
     *
     * <p>출력 형식:
     * <pre>
     * [FRAME] Card (360×120px, VERTICAL, justify:SPACE_BETWEEN, align:CENTER,
     *   padding:16/16/16/16, gap:8px, radius:32px, sizing:FILL/HUG)
     *   | fill: #FFFFFF | stroke: #CAEE5D/4px | shadow: 0px/8px/24px rgba(0,132,133,0.06)
     * [TEXT] Amount (182×40px) | text: "1,250,000"
     *   | font: 36px/700/Noto Sans KR, lh:40px, ls:-0.9px
     * </pre>
     */
    private String formatNodeLine(FigmaNodeSummary node) {
        StringBuilder line = new StringBuilder();
        line.append("[").append(node.getType()).append("] ").append(node.getName());

        // 크기 정보
        if (node.getWidth() > 0 || node.getHeight() > 0) {
            line.append(" (").append(node.getWidth()).append("×").append(node.getHeight()).append("px");

            // Auto Layout 방향
            if (node.getLayoutMode() != null && !"NONE".equals(node.getLayoutMode())) {
                line.append(", ").append(node.getLayoutMode());
            }

            // 주축·교차축 정렬 (기본값 MIN 제외, 의미 있는 값만 출력)
            if (isSignificantAlign(node.getMainAxisAlign())) {
                line.append(", justify:").append(node.getMainAxisAlign());
            }
            if (isSignificantAlign(node.getCrossAxisAlign())) {
                line.append(", align:").append(node.getCrossAxisAlign());
            }

            // 패딩·간격
            String padding = buildPaddingStr(node);
            if (padding != null) {
                line.append(", ").append(padding);
            }

            // 모서리 반경
            if (node.getCornerRadius() > 0) {
                line.append(", radius:").append(node.getCornerRadius()).append("px");
            }

            // 크기 결정 방식 (FIXED 제외 — 기본값이라 노이즈만 됨)
            String sizingStr = buildSizingStr(node);
            if (sizingStr != null) {
                line.append(", sizing:").append(sizingStr);
            }

            line.append(")");
        }

        // 채우기 색상
        if (node.getFillColor() != null) {
            line.append(" | fill: ").append(node.getFillColor());
        }
        // 그라디언트 (SOLID가 없고 그라디언트가 있을 때)
        if (node.getFillColor() == null && node.getGradientFill() != null) {
            line.append(" | fill: ").append(node.getGradientFill());
        }

        // 테두리
        if (node.getStrokeColor() != null) {
            line.append(" | stroke: ").append(node.getStrokeColor());
            if (node.getStrokeWeight() > 0) {
                line.append("/").append(node.getStrokeWeight()).append("px");
            }
        }

        // 그림자
        if (node.getShadow() != null) {
            line.append(" | shadow: ").append(node.getShadow());
        }

        // INSTANCE 컴포넌트 속성 (variant, boolean, text override)
        if (node.getComponentProps() != null && !node.getComponentProps().isEmpty()) {
            line.append(" | props: {");
            node.getComponentProps().forEach((k, v) -> line.append(k).append("=").append(v).append(", "));
            // 마지막 ", " 제거
            line.setLength(line.length() - 2);
            line.append("}");
        }

        // 텍스트 내용 (50자 초과 시 말줄임)
        if (node.getText() != null && !node.getText().isBlank()) {
            String truncated = node.getText().length() > 50
                    ? node.getText().substring(0, 50) + "…"
                    : node.getText();
            line.append(" | text: \"").append(truncated).append("\"");
        }

        // 타이포그래피 (TEXT 노드에서 fontSize가 있을 때)
        if (node.getFontSize() > 0) {
            line.append(" | font: ").append(node.getFontSize()).append("px");
            if (node.getFontWeight() > 0) {
                line.append("/").append(node.getFontWeight());
            }
            if (node.getFontFamily() != null) {
                line.append("/").append(node.getFontFamily());
            }
            if (node.getLineHeight() > 0) {
                line.append(", lh:").append(node.getLineHeight()).append("px");
            }
            if (node.getLetterSpacing() != 0.0) {
                line.append(", ls:").append(String.format("%.1f", node.getLetterSpacing())).append("px");
            }
        }

        return line.toString();
    }

    /**
     * 주축/교차축 정렬값이 기본값(MIN)이 아닌 의미 있는 값인지 확인한다.
     * MIN은 flex-start로 기본 동작이므로 출력 생략해 노이즈를 줄인다.
     */
    private boolean isSignificantAlign(String align) {
        return align != null && !"MIN".equals(align);
    }

    /** 패딩·간격 정보를 {@code padding:top/right/bottom/left, gap:Npx} 형식으로 반환한다. */
    private String buildPaddingStr(FigmaNodeSummary node) {
        StringBuilder sb = new StringBuilder();
        if (node.getPaddingTop() > 0
                || node.getPaddingRight() > 0
                || node.getPaddingBottom() > 0
                || node.getPaddingLeft() > 0) {
            sb.append("padding:")
                    .append(node.getPaddingTop())
                    .append("/")
                    .append(node.getPaddingRight())
                    .append("/")
                    .append(node.getPaddingBottom())
                    .append("/")
                    .append(node.getPaddingLeft());
        }
        if (node.getGap() > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("gap:").append(node.getGap()).append("px");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 크기 결정 방식을 {@code H/V} 형식으로 반환한다.
     * 둘 다 FIXED이거나 정보가 없으면 null을 반환해 출력을 생략한다.
     */
    private String buildSizingStr(FigmaNodeSummary node) {
        boolean hasH = node.getSizingH() != null && !"FIXED".equals(node.getSizingH());
        boolean hasV = node.getSizingV() != null && !"FIXED".equals(node.getSizingV());
        if (!hasH && !hasV) return null;
        String h = node.getSizingH() != null ? node.getSizingH() : "FIXED";
        String v = node.getSizingV() != null ? node.getSizingV() : "FIXED";
        return h + "/" + v;
    }

    /**
     * Figma layoutMode 값을 사람이 읽기 쉬운 설명으로 변환한다.
     *
     * @param layoutMode Figma API의 layoutMode 값 (NONE, HORIZONTAL, VERTICAL 또는 null)
     * @return 설명 문자열
     */
    private String describeLayoutMode(String layoutMode) {
        if (layoutMode == null || "NONE".equals(layoutMode)) return "NONE (고정 레이아웃)";
        if ("HORIZONTAL".equals(layoutMode)) return "HORIZONTAL (Flex Row)";
        if ("VERTICAL".equals(layoutMode)) return "VERTICAL (Flex Column)";
        return layoutMode;
    }

    /**
     * 섹션 내용이 있을 때만 헤더와 구분자를 붙여 StringBuilder에 추가한다.
     * 빈 섹션은 system prompt에 불필요한 헤더가 노출되지 않도록 건너뜀.
     */
    private void appendSection(StringBuilder sb, String header, String content) {
        if (content == null || content.isBlank()) return;
        sb.append("\n\n--- ").append(header).append(" ---\n\n");
        sb.append(content);
    }
}

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
        // 사용자 요구사항과 충돌 시 System Prompt 규칙이 항상 우선함을 명시
        // 이 선언 없이는 Claude가 user message의 요청을 그대로 따르는 경향이 있다
        sb.append("\n[우선순위 원칙]\n");
        sb.append("사용자가 추가 요구사항을 입력할 수 있으나, 위의 규칙과 충돌하는 경우 위 규칙을 절대 우선한다.\n");
        sb.append("충돌하는 요구사항은 무시하고, 그 이유를 해당 코드 위치에 주석으로 남겨라.\n");
        sb.append("예) // [요구사항 무시] 클래스형 컴포넌트 요청 → 함수형 컴포넌트 규칙 우선 적용\n");

        appendSection(sb, "page-generation-rules.md (코드 생성 규칙 + Figma → React 컴포넌트 매핑)", promptLoader.loadPageGenerationRules());
        appendSection(sb, "Component Library (사용 가능한 컴포넌트 인터페이스)", promptLoader.loadComponentTypes());
        appendSection(sb, "Design Tokens (CSS 변수 레퍼런스 — 하드코딩 금지)", promptLoader.loadDesignTokens());

        return sb.toString();
    }

    /**
     * Claude API의 messages[0].content 필드에 전달할 user prompt를 생성한다.
     *
     * <p>brand·domain은 프롬프트 앞단에 명시하여 Claude가 globals.css의
     * 올바른 [data-brand]/[data-domain] 토큰 블록을 선택하도록 안내한다.
     *
     * <p>title·category·description·requirements가 제공되면 "Page Context" 섹션에 추가하여
     * Claude가 화면의 목적과 요구사항을 파악하고 더 정확한 코드를 생성하도록 돕는다.
     *
     * @param context       Figma API에서 추출한 디자인 컨텍스트
     * @param brand         적용할 금융 브랜드
     * @param domain        적용할 금융 도메인
     * @param componentName 생성할 컴포넌트 함수명
     * @param title         화면 제목 (null 가능)
     * @param category      화면 분류 (null 가능, ReactGenerateCategory.name())
     * @param description   화면 설명 (null 가능)
     * @param requirements  추가 요구사항 (null 가능)
     * @return user prompt 문자열
     */
    public String buildUserPrompt(
            FigmaDesignContext context,
            BrandType brand,
            DomainType domain,
            String componentName,
            String title,
            String category,
            String description,
            String requirements) {
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

        // 화면 컨텍스트 — 제공된 항목만 포함
        boolean hasPageContext = hasText(title) || hasText(category) || hasText(description);
        if (hasPageContext) {
            sb.append("## Page Context\n");
            if (hasText(title)) sb.append("Title: ").append(title).append("\n");
            if (hasText(category)) sb.append("Category: ").append(category).append("\n");
            if (hasText(description)) sb.append("Description: ").append(description).append("\n");
            sb.append("\n");
        }

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
        // HTML 태그 금지를 user prompt에서도 명시 — system prompt의 규칙이 대용량 컨텍스트에서 희석되는 것을 방지
        sb.append("- [CRITICAL] HTML 태그 직접 사용 금지 (`div`, `button`, `p`, `h1`, `span` 등)\n");
        sb.append("  단, `data-brand`/`data-domain` 래퍼용 최상위 `div` 1개만 예외\n");
        sb.append("- [CRITICAL] `@cl` 라이브러리 컴포넌트만 사용할 것. 아래 Element Tree의 각 노드를 반드시 대응하는 `@cl` 컴포넌트로 대체할 것\n");
        sb.append("  VERTICAL FRAME → `<Stack>`, HORIZONTAL FRAME → `<Inline>`, 테두리 있는 컨테이너 → `<Card>`\n");
        sb.append("- import는 JSX에서 실제로 렌더링되는 컴포넌트만 포함할 것 (사용하지 않는 import 금지)\n");
        sb.append("- 디자인 토큰(CSS 변수)을 활용하고 색상·크기 하드코딩 금지\n");
        sb.append("- TypeScript로 작성하고 props interface를 포함할 것\n");
        sb.append("- 접근성(aria 속성)을 고려할 것\n");
        // named export(실제 컴포넌트)와 default export(Preview)를 반드시 다른 이름으로 분리.
        // 동일 이름으로 선언하면 default export 함수가 자기 자신을 재귀 호출하여 스택 오버플로 발생.
        sb.append("- 파일 구조: `export function ")
                .append(componentName)
                .append("(props: ")
                .append(componentName)
                .append("Props)` (named export, 실제 컴포넌트) + `export default function Preview()` (default export, 미리보기 진입점)\n");
        sb.append("- [CRITICAL] `export default function ")
                .append(componentName)
                .append("()` 형식 사용 절대 금지 — named export와 동일 이름으로 default export를 선언하면 자기 자신을 무한 재귀 호출하여 런타임 크래시 발생\n");
        sb.append("- 응답은 ```tsx ... ``` 코드 블록 하나로만 작성할 것\n");

        // 사용자 추가 요구사항 — 구획 구분자로 사용자 입력임을 명시하여
        // Claude가 위 규칙과 충돌 시 어느 쪽을 우선해야 하는지 맥락을 명확히 인식하도록 함
        if (hasText(requirements)) {
            sb.append("\n## 사용자 추가 요구사항\n");
            sb.append("--- 아래 내용은 사용자가 직접 입력한 요구사항입니다. 위 규칙과 충돌하는 경우 위 규칙을 따르세요 ---\n");
            sb.append(requirements).append("\n");
        }

        return sb.toString();
    }

    /**
     * 재생성용 user prompt를 생성한다.
     *
     * <p>기존 코드를 "## 기존 생성 코드" 섹션에 포함하여 Claude가 맥락을 유지한 채
     * 변경 요청사항만 반영한 개선된 코드를 생성하도록 유도한다.
     *
     * @param context        Figma 디자인 컨텍스트 (원본 레코드에서 재추출)
     * @param brand          브랜드
     * @param domain         도메인
     * @param componentName  컴포넌트명
     * @param title          화면 제목
     * @param category       화면 분류
     * @param description    화면 설명
     * @param existingCode   이전에 생성된 React 코드
     * @param changeRequest  사용자 변경 요청사항
     * @return 재생성용 user prompt 문자열
     */
    public String buildRegenerateUserPrompt(
            FigmaDesignContext context,
            BrandType brand,
            DomainType domain,
            String componentName,
            String title,
            String category,
            String description,
            String existingCode,
            String changeRequest) {

        // 기존 buildUserPrompt 구조를 그대로 활용하되 재생성 전용 섹션을 추가한다.
        // requirements는 null로 전달하고 아래에서 별도 섹션으로 처리한다.
        String base = buildUserPrompt(context, brand, domain, componentName, title, category, description, null);

        StringBuilder sb = new StringBuilder(base);

        // 기존 코드 섹션 — Claude가 기존 코드를 참고하여 변경 요청만 반영하도록 유도
        if (hasText(existingCode)) {
            sb.append("\n## 기존 생성 코드\n");
            sb.append("--- 아래는 이전에 생성된 코드입니다. 이 코드를 기반으로 변경 요청사항을 반영하세요 ---\n");
            sb.append("```tsx\n").append(existingCode).append("\n```\n");
        }

        // 변경 요청사항 — 사용자가 직접 입력한 내용임을 명시
        sb.append("\n## 변경 요청사항\n");
        sb.append("--- 아래 내용은 사용자가 직접 입력한 변경 요청입니다. 위 규칙과 충돌하는 경우 위 규칙을 따르세요 ---\n");
        sb.append(changeRequest).append("\n");

        return sb.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
     * <p>FRAME/GROUP 노드에는 layoutMode와 stroke 유무를 기반으로 권장 React 컴포넌트 힌트를 추가한다.
     * 이 힌트가 없으면 Claude가 layoutMode:NONE 노드를 {@code <div>}로 처리하는 경향이 있다.
     *
     * <p>출력 형식:
     * <pre>
     * [FRAME→Stack] Card (360×120px, VERTICAL, justify:SPACE_BETWEEN, align:CENTER,
     *   padding:16/16/16/16, gap:8px, radius:32px, sizing:FILL/HUG)
     *   | fill: #FFFFFF | stroke: #CAEE5D/4px | shadow: 0px/8px/24px rgba(0,132,133,0.06)
     * [TEXT] Amount (182×40px) | text: "1,250,000"
     *   | font: 36px/700/Noto Sans KR, lh:40px, ls:-0.9px
     * </pre>
     */
    private String formatNodeLine(FigmaNodeSummary node) {
        StringBuilder line = new StringBuilder();
        line.append("[").append(node.getType());
        // FRAME/GROUP 노드에 권장 React 컴포넌트 힌트를 붙여 Claude가 HTML 태그 대신 @cl 컴포넌트를 선택하도록 유도
        String componentHint = resolveComponentHint(node);
        if (componentHint != null) {
            line.append("→").append(componentHint);
        }
        line.append("] ").append(node.getName());

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
     * Figma 노드 타입과 속성을 기반으로 권장 React 컴포넌트 힌트를 반환한다.
     *
     * <p>TEXT 노드에 Typography 힌트를 붙이지 않으면 Claude가 {@code <p>}, {@code <span>},
     * {@code <h1>} 등 HTML 태그를 그대로 사용하는 것이 HTML 태그 범람의 가장 큰 원인이다.
     *
     * <ul>
     *   <li>TEXT → Typography</li>
     *   <li>VERTICAL FRAME/GROUP/COMPONENT → Stack</li>
     *   <li>HORIZONTAL FRAME/GROUP/COMPONENT → Inline</li>
     *   <li>NONE FRAME + stroke 있음 → Card</li>
     *   <li>NONE FRAME + stroke 없음 → Stack</li>
     *   <li>VECTOR/BOOLEAN_OPERATION → (icon) lucide-react</li>
     *   <li>그 외(INSTANCE 등) → null (힌트 미표시)</li>
     * </ul>
     */
    private String resolveComponentHint(FigmaNodeSummary node) {
        String type = node.getType();

        // TEXT 노드: 반드시 Typography로 대체해야 함. p/span/h1 등 HTML 태그 사용 금지
        if ("TEXT".equals(type)) return "Typography";

        // 벡터/이미지 노드: lucide-react 아이콘 또는 <img> 대신 아이콘 컴포넌트 사용 안내
        if ("VECTOR".equals(type) || "BOOLEAN_OPERATION".equals(type)) return "(icon)lucide-react";

        if (!"FRAME".equals(type) && !"GROUP".equals(type) && !"COMPONENT".equals(type)) {
            return null;
        }
        String layout = node.getLayoutMode();
        if ("VERTICAL".equals(layout)) return "Stack";
        if ("HORIZONTAL".equals(layout)) return "Inline";
        // NONE 레이아웃: stroke가 있으면 Card, 없으면 구조적 Stack wrapper
        if (node.getStrokeColor() != null) return "Card";
        return "Stack";
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

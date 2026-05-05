package com.example.reactplatform.domain.reactgenerate.figma;

import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaNode;
import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaNodeResponse;
import com.example.reactplatform.global.exception.NotFoundException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Figma API 원시 응답({@link FigmaNodeResponse})을 Claude 프롬프트용 컨텍스트({@link FigmaDesignContext})로 변환하는 컴포넌트.
 *
 * <p>변환 시 다음을 수행한다:
 * <ul>
 *   <li>루트 노드의 크기·타입·레이아웃 정보 추출</li>
 *   <li>하위 노드를 {@code MAX_DEPTH}까지 재귀 탐색하여 {@link FigmaNodeSummary} 생성</li>
 *   <li>SOLID 채우기 색상을 HEX/rgba 문자열로 변환 (alpha < 1 이면 rgba 형식)</li>
 *   <li>그라디언트 채우기를 정지점 색상 목록으로 요약</li>
 *   <li>테두리(stroke), 모서리 반경(cornerRadius), 그림자(DROP_SHADOW) 추출</li>
 *   <li>TEXT 노드의 타이포그래피(fontSize, fontWeight, lineHeight, letterSpacing, fontFamily) 추출</li>
 *   <li>Auto Layout 정렬(primaryAxisAlignItems, counterAxisAlignItems) 및 크기 결정 방식(sizing) 추출</li>
 * </ul>
 *
 * <p>깊이 제한은 토큰 비용을 줄이고 Claude에게 핵심 구조만 전달하기 위해 적용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FigmaDesignExtractor {

    private final VariantNormalizer variantNormalizer;

    /** 노드 트리 탐색 최대 깊이. 루트는 0, 직계 자식은 1 */
    private static final int MAX_DEPTH = 15;

    /** 깊이 레벨당 처리할 최대 노드 수. 복잡한 Figma 파일의 메모리 폭발 방지 */
    private static final int MAX_NODES_PER_LEVEL = 50;

    /**
     * 이 값 이상의 alpha는 완전 불투명으로 취급하여 HEX 형식으로 출력한다.
     * 1.0 미만의 미세한 값(예: fillOpacity 곱셈 결과)도 rgba로 노출되지 않도록
     * 1.0보다 약간 낮은 0.99를 경계로 사용한다.
     */
    private static final double OPAQUE_ALPHA_THRESHOLD = 0.99;

    /**
     * Figma API 응답에서 디자인 컨텍스트를 추출한다.
     *
     * @param response Figma API {@code /v1/files/{fileKey}/nodes} 응답
     * @param nodeId   요청한 노드 ID (API 형식: {@code pageId:nodeId})
     * @param figmaUrl 원본 Figma URL (참조용으로 컨텍스트에 포함)
     * @return Claude 프롬프트 생성에 사용할 디자인 컨텍스트
     * @throws NotFoundException 응답에서 해당 노드를 찾을 수 없을 때
     */
    public FigmaDesignContext extract(FigmaNodeResponse response, String nodeId, String figmaUrl) {
        FigmaNode root = findRootNode(response, nodeId);

        log.debug(
                "Figma 노드 추출 — name: {}, type: {}, children: {}개",
                root.getName(),
                root.getType(),
                root.getChildren() == null ? 0 : root.getChildren().size());

        return FigmaDesignContext.builder()
                .figmaUrl(figmaUrl)
                .componentName(root.getName())
                .componentType(root.getType())
                .width(toInt(
                        root.getAbsoluteBoundingBox() != null
                                ? root.getAbsoluteBoundingBox().getWidth()
                                : 0))
                .height(toInt(
                        root.getAbsoluteBoundingBox() != null
                                ? root.getAbsoluteBoundingBox().getHeight()
                                : 0))
                .layoutMode(root.getLayoutMode())
                .children(extractChildren(root.getChildren(), 1))
                .build();
    }

    /**
     * 응답의 nodes 맵에서 루트 노드를 찾는다.
     *
     * <p>요청한 nodeId로 직접 조회를 시도하고, 없으면 맵의 첫 번째 값을 폴백으로 사용한다.
     * API를 단일 nodeId로 요청했으므로 폴백 시에도 원하는 노드를 가져온다.
     */
    private FigmaNode findRootNode(FigmaNodeResponse response, String nodeId) {
        if (response.getNodes() == null || response.getNodes().isEmpty()) {
            throw new NotFoundException("Figma API 응답에 노드 데이터가 없습니다.");
        }

        FigmaNodeResponse.NodeWrapper wrapper = response.getNodes().get(nodeId);
        if (wrapper == null) {
            // Figma API는 nodeId를 ':' (웹 URL) 또는 '-' (내부 포맷) 형식으로 혼용하므로 대안 키 시도
            String alternateKey = nodeId.contains(":") ? nodeId.replace(":", "-") : nodeId.replace("-", ":");
            wrapper = response.getNodes().get(alternateKey);
            if (wrapper != null) {
                log.debug("nodeId 키 형식 변환으로 조회 성공: {} → {}", nodeId, alternateKey);
            }
        }
        if (wrapper == null) {
            throw new NotFoundException("Figma API 응답에서 노드를 찾을 수 없습니다: nodeId=" + nodeId);
        }

        if (wrapper.getDocument() == null) {
            throw new NotFoundException("Figma 노드 document가 비어있습니다: nodeId=" + nodeId);
        }

        return wrapper.getDocument();
    }

    /**
     * 하위 노드 목록을 재귀적으로 탐색하여 {@link FigmaNodeSummary} 목록으로 변환한다.
     *
     * <p>깊이 제한({@code MAX_DEPTH})과 레벨당 노드 수 제한({@code MAX_NODES_PER_LEVEL})을 모두 적용하여
     * 복잡한 Figma 파일에서 메모리 과다 사용을 방지한다.
     *
     * @param nodes 변환할 노드 목록
     * @param depth 현재 탐색 깊이 (MAX_DEPTH 초과 시 빈 목록 반환)
     */
    private List<FigmaNodeSummary> extractChildren(List<FigmaNode> nodes, int depth) {
        if (nodes == null || nodes.isEmpty() || depth > MAX_DEPTH) {
            return Collections.emptyList();
        }
        List<FigmaNode> targets = nodes;
        if (nodes.size() > MAX_NODES_PER_LEVEL) {
            log.warn(
                    "depth={} 노드 수({})가 제한({})을 초과하여 앞 {}개만 처리합니다.",
                    depth,
                    nodes.size(),
                    MAX_NODES_PER_LEVEL,
                    MAX_NODES_PER_LEVEL);
            targets = nodes.subList(0, MAX_NODES_PER_LEVEL);
        }
        return targets.stream().map(node -> toSummary(node, depth)).collect(Collectors.toList());
    }

    /** 단일 {@link FigmaNode}를 {@link FigmaNodeSummary}로 변환한다. */
    private FigmaNodeSummary toSummary(FigmaNode node, int depth) {
        FigmaNodeSummary.FigmaNodeSummaryBuilder builder = FigmaNodeSummary.builder()
                // 식별
                .name(node.getName())
                .type(node.getType())
                // 크기
                .width(toInt(
                        node.getAbsoluteBoundingBox() != null
                                ? node.getAbsoluteBoundingBox().getWidth()
                                : 0))
                .height(toInt(
                        node.getAbsoluteBoundingBox() != null
                                ? node.getAbsoluteBoundingBox().getHeight()
                                : 0))
                // Auto Layout
                .layoutMode(node.getLayoutMode())
                .mainAxisAlign(node.getPrimaryAxisAlignItems())
                .crossAxisAlign(node.getCounterAxisAlignItems())
                .sizingH(node.getPrimaryAxisSizingMode())
                .sizingV(node.getCounterAxisSizingMode())
                .paddingTop(toInt(node.getPaddingTop()))
                .paddingRight(toInt(node.getPaddingRight()))
                .paddingBottom(toInt(node.getPaddingBottom()))
                .paddingLeft(toInt(node.getPaddingLeft()))
                .gap(toInt(node.getItemSpacing()))
                // 시각 스타일
                .fillColor(extractFillColor(node.getFills()))
                .gradientFill(extractGradientFill(node.getFills()))
                .cornerRadius(toInt(node.getCornerRadius()))
                .strokeColor(extractFillColor(node.getStrokes()))
                .strokeWeight(toInt(node.getStrokeWeight()))
                .shadow(extractShadow(node.getEffects()))
                // 텍스트
                .text(node.getCharacters())
                // INSTANCE 컴포넌트 속성
                .componentProps(extractComponentProps(node))
                // 자식
                .children(extractChildren(node.getChildren(), depth + 1));

        // 타이포그래피 (TEXT 노드의 style 필드에서 추출)
        if (node.getStyle() != null) {
            FigmaNode.TypeStyle s = node.getStyle();
            builder.fontSize(toInt(s.getFontSize()))
                    .fontWeight(s.getFontWeight() != null ? s.getFontWeight() : 0)
                    .lineHeight(toInt(s.getLineHeightPx()))
                    .letterSpacing(s.getLetterSpacing() != null ? s.getLetterSpacing() : 0.0)
                    .fontFamily(s.getFontFamily());
        }

        return builder.build();
    }

    /**
     * 채우기 목록에서 첫 번째 SOLID 타입의 색상을 반환한다.
     * alpha < 0.99 이면 rgba(r,g,b,a) 형식, 그 외는 #RRGGBB 형식으로 반환한다.
     *
     * @param fills Figma 채우기 또는 테두리 목록
     * @return 색상 문자열, SOLID 채우기가 없으면 null
     */
    private String extractFillColor(List<FigmaNode.Fill> fills) {
        if (fills == null || fills.isEmpty()) return null;
        return fills.stream()
                .filter(f -> "SOLID".equals(f.getType()) && f.getColor() != null)
                .findFirst()
                .map(f -> colorToString(f.getColor(), f.getOpacity()))
                .orElse(null);
    }

    /**
     * 채우기 목록에서 첫 번째 그라디언트 정보를 "타입: 색상1 → 색상2" 형식으로 반환한다.
     * 정지점이 없으면 타입명만 반환한다.
     *
     * @param fills Figma 채우기 목록
     * @return 그라디언트 요약 문자열, 그라디언트가 없으면 null
     */
    private String extractGradientFill(List<FigmaNode.Fill> fills) {
        if (fills == null || fills.isEmpty()) return null;
        return fills.stream()
                .filter(f -> f.getType() != null && f.getType().startsWith("GRADIENT_"))
                .findFirst()
                .map(f -> {
                    String type = f.getType();
                    List<FigmaNode.GradientStop> stops = f.getGradientStops();
                    if (stops == null || stops.size() < 2) return type;
                    String first = colorToString(stops.get(0).getColor(), null);
                    String last = colorToString(stops.get(stops.size() - 1).getColor(), null);
                    return type + ": " + first + " → " + last;
                })
                .orElse(null);
    }

    /**
     * 이펙트 목록에서 첫 번째 DROP_SHADOW를 "xOffset/yOffset/radius rgba(...)" 형식으로 반환한다.
     *
     * @param effects Figma 이펙트 목록
     * @return 그림자 요약 문자열, DROP_SHADOW가 없으면 null
     */
    private String extractShadow(List<FigmaNode.Effect> effects) {
        if (effects == null || effects.isEmpty()) return null;
        return effects.stream()
                .filter(e -> "DROP_SHADOW".equals(e.getType())
                        && (e.getVisible() == null || e.getVisible())
                        && e.getColor() != null)
                .findFirst()
                .map(e -> {
                    int x = e.getOffset() != null ? toInt(e.getOffset().getX()) : 0;
                    int y = e.getOffset() != null ? toInt(e.getOffset().getY()) : 0;
                    int r = toInt(e.getRadius());
                    return x + "px/" + y + "px/" + r + "px " + colorToString(e.getColor(), null);
                })
                .orElse(null);
    }

    /**
     * Figma RGBA 색상(각 채널 0.0~1.0)을 문자열로 변환한다.
     *
     * <p>alpha가 {@link #OPAQUE_ALPHA_THRESHOLD} 미만이면 {@code rgba(r,g,b,a)} 형식,
     * 그 이상이면 {@code #RRGGBB} 형식으로 반환한다.
     * fillOpacity가 별도로 지정된 경우 color.a에 곱하여 실제 불투명도를 계산한다.
     *
     * @param color        Figma Color 객체
     * @param fillOpacity  Fill.opacity 값 (null 이면 color.a 사용)
     * @return 색상 문자열
     */
    private String colorToString(FigmaNode.Color color, Double fillOpacity) {
        if (color == null) return null;
        int r = Math.min(255, (int) Math.round(color.getR() * 255));
        int g = Math.min(255, (int) Math.round(color.getG() * 255));
        int b = Math.min(255, (int) Math.round(color.getB() * 255));
        // fillOpacity가 있으면 color.a에 곱해서 실제 불투명도 계산
        double a = (fillOpacity != null) ? color.getA() * fillOpacity : color.getA();
        if (a < OPAQUE_ALPHA_THRESHOLD) {
            return String.format("rgba(%d,%d,%d,%.2f)", r, g, b, a);
        }
        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * INSTANCE 노드의 componentProperties에서 Claude에게 유용한 속성을 추출하고 React prop 형식으로 정규화한다.
     *
     * <p>포함: VARIANT(변형 옵션), BOOLEAN(on/off 상태), TEXT(텍스트 오버라이드)
     * <p>제외: INSTANCE_SWAP — 컴포넌트 ID 문자열이라 Claude에게 의미가 없음
     *
     * <p>추출 후 {@link VariantNormalizer}를 통해 키·값을 정규화한다.
     * 예) {Variant=Primary, Size=Medium} → {variant=primary, size=md}
     *
     * @param node 추출할 노드
     * @return 정규화된 속성명 → 값 문자열 맵. INSTANCE가 아니거나 속성이 없으면 빈 맵
     */
    private Map<String, String> extractComponentProps(FigmaNode node) {
        if (!"INSTANCE".equals(node.getType())) return Collections.emptyMap();
        if (node.getComponentProperties() == null
                || node.getComponentProperties().isEmpty()) return Collections.emptyMap();

        // INSTANCE_SWAP 제외 후 타입 정보가 보존된 상태로 수집 (VariantNormalizer가 TEXT 타입 판별에 사용)
        Map<String, FigmaNode.ComponentProperty> rawProps = new LinkedHashMap<>();
        node.getComponentProperties().forEach((propName, prop) -> {
            if (prop == null || prop.getType() == null || prop.getValue() == null) return;
            if ("INSTANCE_SWAP".equals(prop.getType())) return;
            rawProps.put(propName, prop);
        });

        if (rawProps.isEmpty()) return Collections.emptyMap();

        return variantNormalizer.normalize(node.getName(), rawProps);
    }

    /** Double 값을 int로 변환한다. null이면 0을 반환한다. */
    private int toInt(Double value) {
        return value == null ? 0 : (int) Math.round(value);
    }

    /** double 값을 int로 변환한다. */
    private int toInt(double value) {
        return (int) Math.round(value);
    }
}

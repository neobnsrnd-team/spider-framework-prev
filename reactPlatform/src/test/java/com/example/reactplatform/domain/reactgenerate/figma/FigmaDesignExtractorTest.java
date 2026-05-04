package com.example.reactplatform.domain.reactgenerate.figma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaNode;
import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaNodeResponse;
import com.example.reactplatform.global.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @file FigmaDesignExtractorTest.java
 * @description FigmaDesignExtractor 단위 테스트.
 *     SOLID/그라디언트 색상 추출, 타이포그래피, 정렬, 크기 결정 방식,
 *     모서리 반경, 테두리, 그림자, 깊이 제한, 노드 수 제한을 검증한다.
 * @see FigmaDesignExtractor
 */
class FigmaDesignExtractorTest {

    private FigmaDesignExtractor extractor;

    private static final String FIGMA_URL = "https://www.figma.com/design/ABC/test?node-id=1-2";
    private static final String NODE_ID = "1:2";

    @BeforeEach
    void setUp() {
        VariantNormalizer normalizer = new VariantNormalizer(new ObjectMapper());
        normalizer.load();
        extractor = new FigmaDesignExtractor(normalizer);
    }

    // ================================================================
    // 기본 추출
    // ================================================================

    @Test
    @DisplayName("루트 노드의 이름·타입·크기·layoutMode가 컨텍스트에 담긴다")
    void extract_rootNodeFields() {
        FigmaNode root = frameNode("Dashboard", 390, 1376, "VERTICAL");
        FigmaDesignContext ctx = extractor.extract(response(NODE_ID, root), NODE_ID, FIGMA_URL);

        assertThat(ctx.getComponentName()).isEqualTo("Dashboard");
        assertThat(ctx.getComponentType()).isEqualTo("FRAME");
        assertThat(ctx.getWidth()).isEqualTo(390);
        assertThat(ctx.getHeight()).isEqualTo(1376);
        assertThat(ctx.getLayoutMode()).isEqualTo("VERTICAL");
        assertThat(ctx.getFigmaUrl()).isEqualTo(FIGMA_URL);
    }

    @Test
    @DisplayName("nodeId가 대시 형식이면 콜론 형식으로 변환하여 노드를 찾는다")
    void extract_dashNodeId_convertsToColon() {
        FigmaNode root = frameNode("Card", 360, 100, null);
        // 응답 키를 콜론 형식으로, 요청 ID를 대시 형식으로 전달
        FigmaDesignContext ctx = extractor.extract(response("1:2", root), "1-2", FIGMA_URL);

        assertThat(ctx.getComponentName()).isEqualTo("Card");
    }

    // ================================================================
    // 채우기 색상
    // ================================================================

    @Nested
    @DisplayName("SOLID 색상 추출")
    class SolidFillTests {

        @Test
        @DisplayName("alpha가 1.0인 SOLID 채우기는 HEX(#RRGGBB) 형식으로 반환된다")
        void solidFill_opaqueColor_returnsHex() {
            FigmaNode root = frameNode("Card", 100, 100, null);
            FigmaNode child = frameNode("Box", 50, 50, null);
            child.setFills(List.of(solidFill(0.0, 0.518, 0.522, 1.0))); // #008485
            root.setChildren(List.of(child));

            FigmaDesignContext ctx = extractor.extract(response(NODE_ID, root), NODE_ID, FIGMA_URL);
            FigmaNodeSummary summary = ctx.getChildren().get(0);

            assertThat(summary.getFillColor()).isEqualTo("#008485");
            assertThat(summary.getGradientFill()).isNull();
        }

        @Test
        @DisplayName("alpha가 0.99 미만인 SOLID 채우기는 rgba(r,g,b,a) 형식으로 반환된다")
        void solidFill_semiTransparent_returnsRgba() {
            FigmaNode root = frameNode("Card", 100, 100, null);
            FigmaNode child = frameNode("Overlay", 50, 50, null);
            child.setFills(List.of(solidFill(1.0, 1.0, 1.0, 0.2)));
            root.setChildren(List.of(child));

            FigmaNodeSummary summary = extractFirstChild(root);

            assertThat(summary.getFillColor()).isEqualTo("rgba(255,255,255,0.20)");
        }

        @Test
        @DisplayName("Fill.opacity가 있으면 color.a에 곱한 실제 불투명도를 사용한다")
        void solidFill_withFillOpacity_multipliesAlpha() {
            FigmaNode root = frameNode("Card", 100, 100, null);
            FigmaNode child = frameNode("Tinted", 50, 50, null);
            FigmaNode.Fill fill = solidFill(1.0, 1.0, 1.0, 1.0);
            fill.setOpacity(0.6); // 실제 불투명도 = 1.0 * 0.6 = 0.6
            child.setFills(List.of(fill));
            root.setChildren(List.of(child));

            FigmaNodeSummary summary = extractFirstChild(root);

            assertThat(summary.getFillColor()).isEqualTo("rgba(255,255,255,0.60)");
        }

        @Test
        @DisplayName("SOLID 채우기가 없으면 fillColor는 null이다")
        void solidFill_noFill_returnsNull() {
            FigmaNode root = frameNode("Card", 100, 100, null);
            FigmaNode child = frameNode("NoFill", 50, 50, null);
            child.setFills(List.of());
            root.setChildren(List.of(child));

            FigmaNodeSummary summary = extractFirstChild(root);

            assertThat(summary.getFillColor()).isNull();
        }
    }

    // ================================================================
    // 그라디언트
    // ================================================================

    @Test
    @DisplayName("GRADIENT_LINEAR 채우기는 '타입: 첫색 → 끝색' 형식으로 추출된다")
    void gradientFill_linear_extractsFirstAndLastStop() {
        FigmaNode root = frameNode("Card", 100, 100, null);
        FigmaNode child = frameNode("Banner", 280, 128, null);

        FigmaNode.Fill gradient = new FigmaNode.Fill();
        gradient.setType("GRADIENT_LINEAR");
        gradient.setGradientStops(List.of(
                gradientStop(0.051, 0.580, 0.533, 1.0, 0.0),  // #0D9488
                gradientStop(0.067, 0.369, 0.349, 1.0, 1.0)   // #115E59
        ));
        child.setFills(List.of(gradient));
        root.setChildren(List.of(child));

        FigmaNodeSummary summary = extractFirstChild(root);

        assertThat(summary.getGradientFill()).startsWith("GRADIENT_LINEAR:");
        assertThat(summary.getGradientFill()).contains("→");
    }

    @Test
    @DisplayName("SOLID 채우기가 있으면 그라디언트보다 SOLID fillColor를 우선 추출한다")
    void gradientFill_solidTakesPriority() {
        FigmaNode root = frameNode("Card", 100, 100, null);
        FigmaNode child = frameNode("Mixed", 50, 50, null);
        child.setFills(List.of(solidFill(0.0, 0.518, 0.522, 1.0)));
        root.setChildren(List.of(child));

        FigmaNodeSummary summary = extractFirstChild(root);

        assertThat(summary.getFillColor()).isEqualTo("#008485");
        assertThat(summary.getGradientFill()).isNull();
    }

    // ================================================================
    // 모서리 반경
    // ================================================================

    @Test
    @DisplayName("cornerRadius 32px 노드는 cornerRadius=32로 추출된다")
    void cornerRadius_extracted() {
        FigmaNode root = frameNode("Card", 100, 100, null);
        FigmaNode child = frameNode("RoundedCard", 50, 50, null);
        child.setCornerRadius(32.0);
        root.setChildren(List.of(child));

        assertThat(extractFirstChild(root).getCornerRadius()).isEqualTo(32);
    }

    @Test
    @DisplayName("cornerRadius가 null이면 0으로 추출된다")
    void cornerRadius_nullBecomesZero() {
        FigmaNode root = frameNode("Card", 100, 100, null);
        FigmaNode child = frameNode("SharpCard", 50, 50, null);
        // cornerRadius 설정 없음
        root.setChildren(List.of(child));

        assertThat(extractFirstChild(root).getCornerRadius()).isEqualTo(0);
    }

    // ================================================================
    // 테두리(Stroke)
    // ================================================================

    @Test
    @DisplayName("SOLID 테두리 색상과 두께가 추출된다")
    void stroke_solidColorAndWeight() {
        FigmaNode root = frameNode("Card", 100, 100, null);
        FigmaNode child = frameNode("AccentCard", 50, 50, null);
        child.setStrokes(List.of(solidFill(0.792, 0.933, 0.365, 1.0))); // #CAEE5D
        child.setStrokeWeight(4.0);
        root.setChildren(List.of(child));

        FigmaNodeSummary summary = extractFirstChild(root);

        assertThat(summary.getStrokeColor()).isEqualTo("#CAEE5D");
        assertThat(summary.getStrokeWeight()).isEqualTo(4);
    }

    @Test
    @DisplayName("테두리가 없으면 strokeColor는 null, strokeWeight는 0이다")
    void stroke_noStroke_nullAndZero() {
        FigmaNode root = frameNode("Card", 100, 100, null);
        FigmaNode child = frameNode("Plain", 50, 50, null);
        root.setChildren(List.of(child));

        FigmaNodeSummary summary = extractFirstChild(root);

        assertThat(summary.getStrokeColor()).isNull();
        assertThat(summary.getStrokeWeight()).isEqualTo(0);
    }

    // ================================================================
    // 그림자(Shadow)
    // ================================================================

    @Test
    @DisplayName("DROP_SHADOW 이펙트가 'x/y/radius color' 형식으로 추출된다")
    void shadow_dropShadow_extracted() {
        FigmaNode root = frameNode("Card", 100, 100, null);
        FigmaNode child = frameNode("ShadowCard", 50, 50, null);

        FigmaNode.Effect shadow = new FigmaNode.Effect();
        shadow.setType("DROP_SHADOW");
        shadow.setVisible(true);
        shadow.setRadius(24.0);
        shadow.setColor(color(0.0, 0.518, 0.522, 0.06));
        FigmaNode.EffectOffset offset = new FigmaNode.EffectOffset();
        offset.setX(0.0);
        offset.setY(8.0);
        shadow.setOffset(offset);
        child.setEffects(List.of(shadow));
        root.setChildren(List.of(child));

        FigmaNodeSummary summary = extractFirstChild(root);

        assertThat(summary.getShadow()).isEqualTo("0px/8px/24px rgba(0,132,133,0.06)");
    }

    @Test
    @DisplayName("visible=false인 그림자는 추출되지 않는다")
    void shadow_invisible_notExtracted() {
        FigmaNode root = frameNode("Card", 100, 100, null);
        FigmaNode child = frameNode("HiddenShadow", 50, 50, null);

        FigmaNode.Effect shadow = new FigmaNode.Effect();
        shadow.setType("DROP_SHADOW");
        shadow.setVisible(false);
        shadow.setRadius(24.0);
        shadow.setColor(color(0.0, 0.518, 0.522, 0.06));
        child.setEffects(List.of(shadow));
        root.setChildren(List.of(child));

        assertThat(extractFirstChild(root).getShadow()).isNull();
    }

    // ================================================================
    // 타이포그래피
    // ================================================================

    @Nested
    @DisplayName("TEXT 노드 타이포그래피 추출")
    class TypographyTests {

        @Test
        @DisplayName("fontSize, fontWeight, fontFamily, lineHeight, letterSpacing이 추출된다")
        void typography_allFields_extracted() {
            FigmaNode root = frameNode("Card", 100, 100, null);
            FigmaNode text = textNode("Amount", "1,250,000");
            FigmaNode.TypeStyle style = new FigmaNode.TypeStyle();
            style.setFontSize(36.0);
            style.setFontWeight(700);
            style.setFontFamily("Noto Sans KR");
            style.setLineHeightPx(40.0);
            style.setLetterSpacing(-0.9);
            text.setStyle(style);
            root.setChildren(List.of(text));

            FigmaNodeSummary summary = extractFirstChild(root);

            assertThat(summary.getFontSize()).isEqualTo(36);
            assertThat(summary.getFontWeight()).isEqualTo(700);
            assertThat(summary.getFontFamily()).isEqualTo("Noto Sans KR");
            assertThat(summary.getLineHeight()).isEqualTo(40);
            assertThat(summary.getLetterSpacing()).isEqualTo(-0.9);
        }

        @Test
        @DisplayName("style이 null이면 타이포그래피 필드는 모두 기본값(0, null)이다")
        void typography_noStyle_defaultValues() {
            FigmaNode root = frameNode("Card", 100, 100, null);
            FigmaNode text = textNode("Label", "안녕");
            // style 설정 없음
            root.setChildren(List.of(text));

            FigmaNodeSummary summary = extractFirstChild(root);

            assertThat(summary.getFontSize()).isEqualTo(0);
            assertThat(summary.getFontWeight()).isEqualTo(0);
            assertThat(summary.getFontFamily()).isNull();
        }
    }

    // ================================================================
    // Auto Layout 정렬·크기 결정 방식
    // ================================================================

    @Test
    @DisplayName("primaryAxisAlignItems와 counterAxisAlignItems가 mainAxisAlign·crossAxisAlign에 담긴다")
    void layout_alignItems_extracted() {
        FigmaNode root = frameNode("Card", 100, 100, null);
        FigmaNode child = frameNode("Row", 100, 50, "HORIZONTAL");
        child.setPrimaryAxisAlignItems("SPACE_BETWEEN");
        child.setCounterAxisAlignItems("CENTER");
        root.setChildren(List.of(child));

        FigmaNodeSummary summary = extractFirstChild(root);

        assertThat(summary.getMainAxisAlign()).isEqualTo("SPACE_BETWEEN");
        assertThat(summary.getCrossAxisAlign()).isEqualTo("CENTER");
    }

    @Test
    @DisplayName("primaryAxisSizingMode와 counterAxisSizingMode가 sizingH·sizingV에 담긴다")
    void layout_sizingMode_extracted() {
        FigmaNode root = frameNode("Card", 100, 100, null);
        FigmaNode child = frameNode("FlexBox", 100, 50, "VERTICAL");
        child.setPrimaryAxisSizingMode("FILL");
        child.setCounterAxisSizingMode("HUG");
        root.setChildren(List.of(child));

        FigmaNodeSummary summary = extractFirstChild(root);

        assertThat(summary.getSizingH()).isEqualTo("FILL");
        assertThat(summary.getSizingV()).isEqualTo("HUG");
    }

    // ================================================================
    // INSTANCE 컴포넌트 속성
    // ================================================================

    @Test
    @DisplayName("INSTANCE 노드의 VARIANT 속성이 componentProps에 담긴다")
    void componentProps_variant_extracted() {
        FigmaNode root = frameNode("Page", 100, 100, null);
        FigmaNode instance = instanceNode("SummaryCard", Map.of(
                "prop1", componentProperty("VARIANT", "spending")
        ));
        root.setChildren(List.of(instance));

        FigmaNodeSummary summary = extractFirstChild(root);

        assertThat(summary.getComponentProps()).containsEntry("prop1", "spending");
    }

    @Test
    @DisplayName("INSTANCE 노드의 BOOLEAN 속성이 'true'/'false' 문자열로 담긴다")
    void componentProps_boolean_convertedToString() {
        FigmaNode root = frameNode("Page", 100, 100, null);
        FigmaNode instance = instanceNode("Toggle", Map.of(
                "isExpanded", componentProperty("BOOLEAN", true)
        ));
        root.setChildren(List.of(instance));

        FigmaNodeSummary summary = extractFirstChild(root);

        assertThat(summary.getComponentProps()).containsEntry("isExpanded", "true");
    }

    @Test
    @DisplayName("INSTANCE_SWAP 속성은 componentProps에서 제외된다")
    void componentProps_instanceSwap_excluded() {
        FigmaNode root = frameNode("Page", 100, 100, null);
        FigmaNode instance = instanceNode("Card", Map.of(
                "variant", componentProperty("VARIANT", "primary"),
                "icon",    componentProperty("INSTANCE_SWAP", "123:456")
        ));
        root.setChildren(List.of(instance));

        FigmaNodeSummary summary = extractFirstChild(root);

        assertThat(summary.getComponentProps()).containsKey("variant");
        assertThat(summary.getComponentProps()).doesNotContainKey("icon");
    }

    @Test
    @DisplayName("FRAME 노드는 componentProps가 null이다")
    void componentProps_nonInstance_isNull() {
        FigmaNode root = frameNode("Page", 100, 100, null);
        FigmaNode frame = frameNode("Container", 100, 50, null);
        root.setChildren(List.of(frame));

        assertThat(extractFirstChild(root).getComponentProps()).isNull();
    }

    @Test
    @DisplayName("INSTANCE에 componentProperties가 없으면 componentProps는 null이다")
    void componentProps_emptyProperties_isNull() {
        FigmaNode root = frameNode("Page", 100, 100, null);
        FigmaNode instance = instanceNode("EmptyInstance", Map.of());
        root.setChildren(List.of(instance));

        assertThat(extractFirstChild(root).getComponentProps()).isNull();
    }

    // ================================================================
    // 깊이 제한
    // ================================================================

    @Test
    @DisplayName("depth=15까지의 노드는 포함되고 depth=16은 잘린다 (MAX_DEPTH=15)")
    void depthLimit_depth16NodeIsTrimmed() {
        // 루트 → 16단계 깊이 체인을 아래에서부터 쌓아 올린다
        FigmaNode leaf = frameNode("L16_CUT", 100, 100, null);
        FigmaNode current = leaf;
        for (int i = 15; i >= 1; i--) {
            FigmaNode level = frameNode("L" + i, 100, 100, null);
            level.setChildren(List.of(current));
            current = level;
        }
        FigmaNode root = frameNode("Root", 100, 100, null);
        root.setChildren(List.of(current)); // current = L1

        FigmaDesignContext ctx = extractor.extract(response(NODE_ID, root), NODE_ID, FIGMA_URL);

        // depth=15(L15)까지 포함되어야 함
        FigmaNodeSummary node = ctx.getChildren().get(0); // L1
        for (int i = 2; i <= 15; i++) {
            node = node.getChildren().get(0);
        }
        assertThat(node.getName()).isEqualTo("L15");

        // depth=16(L16_CUT)은 빈 리스트
        assertThat(node.getChildren()).isEmpty();
    }

    // ================================================================
    // 노드 수 제한
    // ================================================================

    @Test
    @DisplayName("레벨당 51개 노드가 있으면 앞 50개만 처리된다")
    void nodeLimit_51Nodes_only50Processed() {
        FigmaNode root = frameNode("Root", 100, 100, null);
        List<FigmaNode> children = new ArrayList<>();
        for (int i = 1; i <= 51; i++) {
            children.add(frameNode("Node" + i, 10, 10, null));
        }
        root.setChildren(children);

        FigmaDesignContext ctx = extractor.extract(response(NODE_ID, root), NODE_ID, FIGMA_URL);

        assertThat(ctx.getChildren()).hasSize(50);
        assertThat(ctx.getChildren().get(49).getName()).isEqualTo("Node50");
    }

    // ================================================================
    // 예외 처리
    // ================================================================

    @Test
    @DisplayName("nodes 맵이 비어있으면 NotFoundException을 던진다")
    void extract_emptyNodes_throwsNotFoundException() {
        FigmaNodeResponse emptyResponse = new FigmaNodeResponse();
        emptyResponse.setNodes(Map.of());

        assertThatThrownBy(() -> extractor.extract(emptyResponse, NODE_ID, FIGMA_URL))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("nodes 맵이 null이면 NotFoundException을 던진다")
    void extract_nullNodes_throwsNotFoundException() {
        FigmaNodeResponse nullResponse = new FigmaNodeResponse();
        // nodes 설정 없음 (null)

        assertThatThrownBy(() -> extractor.extract(nullResponse, NODE_ID, FIGMA_URL))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("요청한 nodeId가 응답에 없고 대안 키도 없으면 NotFoundException을 던진다")
    void extract_unknownNodeId_throwsNotFoundException() {
        FigmaNode root = frameNode("Root", 100, 100, null);
        FigmaNodeResponse resp = response("999:999", root);

        assertThatThrownBy(() -> extractor.extract(resp, "1:2", FIGMA_URL))
                .isInstanceOf(NotFoundException.class);
    }

    // ================================================================
    // helpers
    // ================================================================

    private FigmaNodeSummary extractFirstChild(FigmaNode root) {
        FigmaDesignContext ctx = extractor.extract(response(NODE_ID, root), NODE_ID, FIGMA_URL);
        return ctx.getChildren().get(0);
    }

    private FigmaNodeResponse response(String nodeId, FigmaNode document) {
        FigmaNodeResponse.NodeWrapper wrapper = new FigmaNodeResponse.NodeWrapper();
        wrapper.setDocument(document);

        FigmaNodeResponse resp = new FigmaNodeResponse();
        Map<String, FigmaNodeResponse.NodeWrapper> nodes = new HashMap<>();
        nodes.put(nodeId, wrapper);
        resp.setNodes(nodes);
        return resp;
    }

    private FigmaNode frameNode(String name, int width, int height, String layoutMode) {
        FigmaNode node = new FigmaNode();
        node.setName(name);
        node.setType("FRAME");
        node.setLayoutMode(layoutMode);
        FigmaNode.BoundingBox bbox = new FigmaNode.BoundingBox();
        bbox.setWidth(width);
        bbox.setHeight(height);
        node.setAbsoluteBoundingBox(bbox);
        return node;
    }

    private FigmaNode instanceNode(String name, Map<String, FigmaNode.ComponentProperty> props) {
        FigmaNode node = new FigmaNode();
        node.setName(name);
        node.setType("INSTANCE");
        FigmaNode.BoundingBox bbox = new FigmaNode.BoundingBox();
        bbox.setWidth(100);
        bbox.setHeight(50);
        node.setAbsoluteBoundingBox(bbox);
        if (!props.isEmpty()) {
            node.setComponentProperties(new LinkedHashMap<>(props));
        }
        return node;
    }

    private FigmaNode.ComponentProperty componentProperty(String type, Object value) {
        FigmaNode.ComponentProperty prop = new FigmaNode.ComponentProperty();
        prop.setType(type);
        prop.setValue(value);
        return prop;
    }

    private FigmaNode textNode(String name, String characters) {
        FigmaNode node = new FigmaNode();
        node.setName(name);
        node.setType("TEXT");
        node.setCharacters(characters);
        FigmaNode.BoundingBox bbox = new FigmaNode.BoundingBox();
        bbox.setWidth(100);
        bbox.setHeight(20);
        node.setAbsoluteBoundingBox(bbox);
        return node;
    }

    private FigmaNode.Fill solidFill(double r, double g, double b, double a) {
        FigmaNode.Fill fill = new FigmaNode.Fill();
        fill.setType("SOLID");
        fill.setColor(color(r, g, b, a));
        return fill;
    }

    private FigmaNode.GradientStop gradientStop(double r, double g, double b, double a, double position) {
        FigmaNode.GradientStop stop = new FigmaNode.GradientStop();
        stop.setColor(color(r, g, b, a));
        stop.setPosition(position);
        return stop;
    }

    private FigmaNode.Color color(double r, double g, double b, double a) {
        FigmaNode.Color c = new FigmaNode.Color();
        c.setR(r);
        c.setG(g);
        c.setB(b);
        c.setA(a);
        return c;
    }
}

package com.example.reactplatform.domain.reactgenerate.figma;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaApiClient;
import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaApiProperties;
import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaNode;
import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaNodeResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * @file FigmaDesignPipelineIntegrationTest.java
 * @description Figma API 실 연결 통합 테스트.
 *     FigmaApiClient → FigmaDesignExtractor 파이프라인을 실제 토큰으로 검증한다.
 *
 * <p>실행 조건: {@code FIGMA_ACCESS_TOKEN} 환경변수가 설정되어 있어야 한다.
 * 없으면 자동으로 건너뛴다 (CI/일반 빌드에 영향 없음).
 *
 * <p>테스트할 디자인을 바꾸려면 {@link #FIGMA_URL} 상수만 수정하면 된다.
 * Figma에서 프레임을 우클릭 → "Copy link to selection"으로 URL을 복사해 사용한다.
 *
 * @see FigmaApiClient
 * @see FigmaDesignExtractor
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "FIGMA_ACCESS_TOKEN", matches = ".+")
class FigmaDesignPipelineIntegrationTest {

    /**
     * 테스트할 Figma 디자인 URL.
     * Figma에서 프레임 선택 후 "Copy link to selection"으로 복사한 URL을 사용한다.
     */
    private static final String FIGMA_URL =
            "https://www.figma.com/design/eRnV2DPVtHbGn5HSISS65O/Hana-Bank-App?node-id=636-538";

    private static final String TOKEN = "figd_MPJXVeNzQ7cq4r8JxafOQlTGTAAOLESXvFuNcIRy";

    private FigmaApiClient figmaApiClient;
    private FigmaDesignExtractor extractor;
    private RestTemplate rawRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        FigmaApiProperties props = new FigmaApiProperties();
        props.setUrl("https://api.figma.com");
        props.setToken(TOKEN);
        props.setConnectTimeoutSeconds(10);
        props.setReadTimeoutSeconds(30);

        figmaApiClient = new FigmaApiClient(new RestTemplateBuilder(), props);
        VariantNormalizer normalizer = new VariantNormalizer(objectMapper);
        normalizer.load();
        extractor = new FigmaDesignExtractor(normalizer);
        rawRestTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Test
    @DisplayName("실제 Figma URL에서 디자인 컨텍스트를 추출한다 — URL만 바꾸면 다른 디자인도 검증 가능")
    void extract_realFigmaUrl_returnsDesignContext() {
        FigmaUrlParser.ParsedFigmaUrl parsed = FigmaUrlParser.parse(FIGMA_URL);
        FigmaNodeResponse response = figmaApiClient.getNode(parsed.getFileKey(), parsed.getNodeId());

        FigmaDesignContext ctx = extractor.extract(response, parsed.getNodeId(), FIGMA_URL);

        // ── 루트 노드 기본 검증 ──────────────────────────────────────
        assertThat(ctx.getComponentName()).isNotBlank();
        assertThat(ctx.getWidth()).isPositive();
        assertThat(ctx.getHeight()).isPositive();
        assertThat(ctx.getChildren()).isNotEmpty();

        // ── 추출 결과 콘솔 출력 (실제 값 확인용) ──────────────────────
        System.out.println("\n========== Figma Design Context ==========");
        System.out.printf("Component : %s (%s)%n", ctx.getComponentName(), ctx.getComponentType());
        System.out.printf("Size      : %d × %d px%n", ctx.getWidth(), ctx.getHeight());
        System.out.printf("Layout    : %s%n", ctx.getLayoutMode());
        System.out.printf("Children  : %d nodes (depth-1)%n", ctx.getChildren().size());
        System.out.println("------------------------------------------");
        printTree(ctx.getChildren(), "");
        System.out.println("==========================================\n");
    }

    @Test
    @DisplayName("MAX_DEPTH 제거 시 전체 노드 수와 깊이별 분포를 출력한다")
    void analyze_nodeDepthDistribution() {
        FigmaUrlParser.ParsedFigmaUrl parsed = FigmaUrlParser.parse(FIGMA_URL);
        FigmaNodeResponse response = figmaApiClient.getNode(parsed.getFileKey(), parsed.getNodeId());

        FigmaNode root = response.getNodes().values().iterator().next().getDocument();

        int[] depthCount = new int[20];
        countByDepth(root, 0, depthCount);

        int total = 0;
        int withinLimit = 0;
        int maxDepthFound = 0;

        for (int d = 0; d < depthCount.length; d++) {
            if (depthCount[d] > 0) maxDepthFound = d;
            total += depthCount[d];
            if (d <= 6) withinLimit += depthCount[d];
        }

        System.out.println("\n=== Figma 노드 깊이별 분포 ===");
        for (int d = 0; d <= maxDepthFound; d++) {
            String bar = "#".repeat(Math.min(depthCount[d], 50));
            String marker = d > 6 ? "  <- MAX_DEPTH 초과" : "";
            System.out.printf("depth %2d: %4d개  %s%s%n", d, depthCount[d], bar, marker);
        }
        System.out.println();
        System.out.printf("전체 노드 수         : %d개%n", total);
        System.out.printf("MAX_DEPTH=6 이하     : %d개 (현재 추출 범위, %.0f%%)%n",
                withinLimit, withinLimit * 100.0 / total);
        System.out.printf("MAX_DEPTH=6 초과     : %d개 (현재 잘림, %.0f%%)%n",
                total - withinLimit, (total - withinLimit) * 100.0 / total);
        System.out.println("==============================\n");

        assertThat(total).isPositive();
    }

    /** FigmaNode 트리에서 깊이별 노드 수를 집계한다. */
    private void countByDepth(FigmaNode node, int depth, int[] counts) {
        if (node == null || depth >= counts.length) return;
        counts[depth]++;
        if (node.getChildren() != null) {
            for (FigmaNode child : node.getChildren()) {
                countByDepth(child, depth + 1, counts);
            }
        }
    }

    @Test
    @DisplayName("INSTANCE 노드의 componentProperties 원시 JSON을 출력하여 API 응답 구조를 진단한다")
    void debug_instanceComponentProperties_rawJson() throws JsonProcessingException {
        FigmaUrlParser.ParsedFigmaUrl parsed = FigmaUrlParser.parse(FIGMA_URL);

        // Figma API는 ids 파라미터에 "636:538" 형식(콜론 그대로)을 요구한다
        String url = "https://api.figma.com/v1/files/" + parsed.getFileKey() + "/nodes?ids=" + parsed.getNodeId();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Figma-Token", TOKEN);

        String rawJson = rawRestTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();

        assertThat(rawJson).isNotBlank();

        JsonNode root = objectMapper.readTree(rawJson);

        System.out.println("\n========== RAW JSON: componentProperties 진단 ==========");

        // 최상위 nodes 맵의 키 출력 (nodeId 형식 확인용)
        JsonNode nodesMap = root.path("nodes");
        System.out.println("응답 노드 키: " + nodesMap.fieldNames().next());

        // INSTANCE 노드를 재귀 탐색하여 componentProperties 출력
        JsonNode document = nodesMap.fields().next().getValue().path("document");
        List<JsonNode> instanceNodes = collectInstanceNodes(document, 0, 3);

        if (instanceNodes.isEmpty()) {
            System.out.println("INSTANCE 노드를 찾을 수 없습니다.");
        } else {
            System.out.printf("발견된 INSTANCE 노드 수 (최대 3개): %d%n%n", instanceNodes.size());
            for (JsonNode instance : instanceNodes) {
                System.out.println("── INSTANCE: " + instance.path("name").asText());
                System.out.println("   componentId: " + instance.path("componentId").asText("(없음)"));

                JsonNode compProps = instance.path("componentProperties");
                if (compProps.isMissingNode() || compProps.isNull()) {
                    System.out.println("   componentProperties: 필드 없음 (API가 반환하지 않음)");
                } else if (compProps.isEmpty()) {
                    System.out.println("   componentProperties: {} (빈 객체)");
                } else {
                    System.out.println("   componentProperties:");
                    System.out.println(objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(compProps));
                }
                System.out.println();
            }
        }

        // raw JSON에 "componentProperties" 키워드가 있는지 확인
        boolean hasAnyCompProps = rawJson.contains("\"componentProperties\"");
        System.out.println("rawJson에 \"componentProperties\" 포함 여부: " + hasAnyCompProps);
        System.out.println("========================================================\n");
    }

    /**
     * JsonNode 트리에서 INSTANCE 타입 노드를 최대 maxCount개 수집한다.
     * depth > 8이면 탐색을 중단한다.
     */
    private List<JsonNode> collectInstanceNodes(JsonNode node, int depth, int maxCount) {
        List<JsonNode> result = new ArrayList<>();
        if (node == null || node.isNull() || depth > 8) return result;
        if ("INSTANCE".equals(node.path("type").asText())) {
            result.add(node);
            if (result.size() >= maxCount) return result;
        }
        JsonNode children = node.path("children");
        if (children.isArray()) {
            for (JsonNode child : children) {
                result.addAll(collectInstanceNodes(child, depth + 1, maxCount - result.size()));
                if (result.size() >= maxCount) break;
            }
        }
        return result;
    }

    /** 노드 트리를 콘솔에 ASCII 트리 형식으로 출력한다. */
    private void printTree(java.util.List<FigmaNodeSummary> nodes, String indent) {
        if (nodes == null || nodes.isEmpty()) return;
        for (int i = 0; i < nodes.size(); i++) {
            boolean last = i == nodes.size() - 1;
            FigmaNodeSummary n = nodes.get(i);
            String connector = last ? "└─ " : "├─ ";
            String childIndent = indent + (last ? "   " : "│  ");

            StringBuilder line = new StringBuilder();
            line.append(indent).append(connector)
                    .append("[").append(n.getType()).append("] ").append(n.getName())
                    .append(" (").append(n.getWidth()).append("×").append(n.getHeight()).append("px");
            if (n.getLayoutMode() != null && !"NONE".equals(n.getLayoutMode())) {
                line.append(", ").append(n.getLayoutMode());
            }
            if (n.getCornerRadius() > 0) line.append(", r:").append(n.getCornerRadius());
            line.append(")");
            if (n.getFillColor() != null) line.append(" fill:").append(n.getFillColor());
            if (n.getFillColor() == null && n.getGradientFill() != null) {
                line.append(" ").append(n.getGradientFill());
            }
            if (n.getStrokeColor() != null) line.append(" stroke:").append(n.getStrokeColor());
            if (n.getShadow() != null) line.append(" shadow:✓");
            if (n.getFontSize() > 0) {
                line.append(" font:").append(n.getFontSize()).append("px/").append(n.getFontWeight());
            }
            if (n.getText() != null && !n.getText().isBlank()) {
                String t = n.getText().length() > 30 ? n.getText().substring(0, 30) + "…" : n.getText();
                line.append(" \"").append(t).append("\"");
            }
            if (n.getComponentProps() != null && !n.getComponentProps().isEmpty()) {
                line.append(" props:").append(n.getComponentProps());
            }

            System.out.println(line);
            printTree(n.getChildren(), childIndent);
        }
    }
}

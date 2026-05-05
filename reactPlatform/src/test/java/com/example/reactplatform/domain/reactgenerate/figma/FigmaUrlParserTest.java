package com.example.reactplatform.domain.reactgenerate.figma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.reactplatform.global.exception.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @file FigmaUrlParserTest.java
 * @description FigmaUrlParser 단위 테스트.
 *     /design/, /file/, /proto/ URL 형식 파싱, nodeId 정규화(대시→콜론, 퍼센트 인코딩),
 *     유효하지 않은 URL 예외 처리를 검증한다.
 * @see FigmaUrlParser
 */
class FigmaUrlParserTest {

    // ================================================================
    // fileKey 추출
    // ================================================================

    @Test
    @DisplayName("/design/ URL에서 fileKey가 올바르게 추출된다")
    void parse_designUrl_extractsFileKey() {
        String url = "https://www.figma.com/design/eRnV2DPVtHbGn5HSISS65O/Hana-Bank?node-id=636-538";
        FigmaUrlParser.ParsedFigmaUrl result = FigmaUrlParser.parse(url);

        assertThat(result.getFileKey()).isEqualTo("eRnV2DPVtHbGn5HSISS65O");
    }

    @Test
    @DisplayName("/file/ URL에서 fileKey가 올바르게 추출된다")
    void parse_fileUrl_extractsFileKey() {
        String url = "https://www.figma.com/file/ABC123/My-Design?node-id=1-2";
        assertThat(FigmaUrlParser.parse(url).getFileKey()).isEqualTo("ABC123");
    }

    @Test
    @DisplayName("/proto/ URL에서 fileKey가 올바르게 추출된다")
    void parse_protoUrl_extractsFileKey() {
        String url = "https://www.figma.com/proto/XYZ789/App?node-id=5-10";
        assertThat(FigmaUrlParser.parse(url).getFileKey()).isEqualTo("XYZ789");
    }

    // ================================================================
    // nodeId 정규화
    // ================================================================

    @Test
    @DisplayName("대시 구분 nodeId(1-2)는 콜론 형식(1:2)으로 변환된다")
    void parse_dashNodeId_convertedToColon() {
        String url = "https://www.figma.com/design/KEY/file?node-id=636-538";
        assertThat(FigmaUrlParser.parse(url).getNodeId()).isEqualTo("636:538");
    }

    @Test
    @DisplayName("퍼센트 인코딩된 nodeId(1%3A2)는 콜론 형식(1:2)으로 변환된다")
    void parse_percentEncodedNodeId_convertedToColon() {
        String url = "https://www.figma.com/design/KEY/file?node-id=636%3A538";
        assertThat(FigmaUrlParser.parse(url).getNodeId()).isEqualTo("636:538");
    }

    @Test
    @DisplayName("이미 콜론 형식인 nodeId(1:2)는 그대로 유지된다")
    void parse_colonNodeId_unchanged() {
        String url = "https://www.figma.com/design/KEY/file?node-id=636:538";
        assertThat(FigmaUrlParser.parse(url).getNodeId()).isEqualTo("636:538");
    }

    @Test
    @DisplayName("추가 쿼리 파라미터가 있어도 node-id만 추출한다")
    void parse_extraQueryParams_onlyNodeIdExtracted() {
        String url = "https://www.figma.com/design/KEY/file?node-id=1-2&t=kEVhPhEsvvZkGaOm-1";
        assertThat(FigmaUrlParser.parse(url).getNodeId()).isEqualTo("1:2");
    }

    @Test
    @DisplayName("URL에 프래그먼트(#)가 있어도 node-id가 올바르게 추출된다")
    void parse_urlWithFragment_extractsNodeId() {
        String url = "https://www.figma.com/design/KEY/file?node-id=1-2#section";
        assertThat(FigmaUrlParser.parse(url).getNodeId()).isEqualTo("1:2");
    }

    // ================================================================
    // 예외 처리
    // ================================================================

    @Test
    @DisplayName("null URL이면 InvalidInputException을 던진다")
    void parse_nullUrl_throwsInvalidInputException() {
        assertThatThrownBy(() -> FigmaUrlParser.parse(null)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("빈 URL이면 InvalidInputException을 던진다")
    void parse_blankUrl_throwsInvalidInputException() {
        assertThatThrownBy(() -> FigmaUrlParser.parse("   ")).isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("/file/ /design/ /proto/ 경로가 없는 URL이면 InvalidInputException을 던진다")
    void parse_unsupportedPath_throwsInvalidInputException() {
        // /board/ 경로는 FigJam 파일이며 파서가 지원하지 않는 형식
        assertThatThrownBy(() -> FigmaUrlParser.parse("https://www.figma.com/board/ABC123/FigJam?node-id=1-2"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("node-id 쿼리 파라미터가 없으면 InvalidInputException을 던진다")
    void parse_noNodeId_throwsInvalidInputException() {
        assertThatThrownBy(() -> FigmaUrlParser.parse("https://www.figma.com/design/KEY/file"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("node-id 값이 비어있으면 InvalidInputException을 던진다")
    void parse_emptyNodeId_throwsInvalidInputException() {
        assertThatThrownBy(() -> FigmaUrlParser.parse("https://www.figma.com/design/KEY/file?node-id="))
                .isInstanceOf(InvalidInputException.class);
    }
}

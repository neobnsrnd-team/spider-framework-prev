package com.example.spiderlink.infra.tcp.parser;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FixedLengthParser 단위 테스트.
 * DB 없이 MessageStructure 를 직접 구성하여 파서 로직만 검증한다.
 */
class FixedLengthParserTest {

    private FixedLengthParser parser;

    @BeforeEach
    void setUp() {
        parser = new FixedLengthParser();
    }

    @Test
    @DisplayName("단순 고정길이 전문 파싱 — 문자/숫자 필드")
    void parse_simpleFields() {
        // given
        // 전문 레이아웃: trxId(20) + userId(10) + amt(10)
        MessageStructure structure = new MessageStructure("ORG01", "TEST_MSG", "F");
        structure.addField(new MessageField("trxId",  MessageField.CHR, 20, 0, MessageField.LEFT,  ' ', null, true, false));
        structure.addField(new MessageField("userId", MessageField.CHR, 10, 0, MessageField.LEFT,  ' ', null, true, false));
        structure.addField(new MessageField("amt",    MessageField.NUM, 10, 0, MessageField.RIGHT, '0', null, true, false));

        // "DEMO_AUTH_LOGIN     user01    0000001000"
        String raw = String.format("%-20s%-10s%010d", "DEMO_AUTH_LOGIN", "user01", 1000);
        byte[] bytes = raw.getBytes();

        // when
        Map<String, Object> result = parser.parse(structure, bytes);

        // then — trailing 공백 제거됨 (참고소스 FixedLengthMessageParser 동일)
        assertThat(result.get("trxId")).isEqualTo("DEMO_AUTH_LOGIN");
        assertThat(result.get("userId")).isEqualTo("user01");
        assertThat(result.get("amt")).isEqualTo("0000001000");
    }

    @Test
    @DisplayName("숫자 필드 scale 처리 — 소수점 2자리")
    void parse_numericWithScale() {
        // given
        // rate(8) : scale=2 → "00012345" → "000123" + "45" → "0001234" 로 처리
        MessageStructure structure = new MessageStructure("ORG01", "RATE_MSG", "F");
        structure.addField(new MessageField("rate", MessageField.NUM, 8, 2, MessageField.RIGHT, '0', null, true, false));

        byte[] bytes = "00012345".getBytes();

        // when
        Map<String, Object> result = parser.parse(structure, bytes);

        // then — 원본 SpiderLink 와 동일하게 소수점 없이 원본 값 보존
        assertThat(result.get("rate")).isEqualTo("00012345");
    }

    @Test
    @DisplayName("헥사 필드 파싱")
    void parse_hexaField() {
        // given
        MessageStructure structure = new MessageStructure("ORG01", "HEX_MSG", "F");
        structure.addField(new MessageField("bitmap", MessageField.HEXA, 4, 0, MessageField.LEFT, ' ', null, true, false));

        byte[] bytes = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};

        // when
        Map<String, Object> result = parser.parse(structure, bytes);

        // then
        assertThat(result.get("bitmap")).isEqualTo("DEADBEEF");
    }

    @Test
    @DisplayName("바이너리 4바이트 필드 파싱")
    void parse_binaryInt() {
        // given
        MessageStructure structure = new MessageStructure("ORG01", "BIN_MSG", "F");
        structure.addField(new MessageField("length", MessageField.BINARY, 4, 0, MessageField.LEFT, ' ', null, true, false));

        // big-endian 256
        byte[] bytes = {0x00, 0x00, 0x01, 0x00};

        // when
        Map<String, Object> result = parser.parse(structure, bytes);

        // then
        assertThat(result.get("length")).isEqualTo(256);
    }

    @Test
    @DisplayName("반복 구조(LoopField) 파싱 — 횟수 필드 포함")
    void parse_loopField_withCountInMessage() {
        // given
        // 전문: userId(10) + itemCount(2) + [itemCode(5) + itemAmt(8)] * itemCount
        MessageStructure structure = new MessageStructure("ORG01", "LIST_MSG", "F");
        structure.addField(new MessageField("userId",    MessageField.CHR, 10, 0, MessageField.LEFT,  ' ', null, true, false));

        // LoopField: 2바이트 읽어서 반복 횟수 결정
        LoopField loop = new LoopField("items", 2, 0, null);
        loop.addChild(new MessageField("itemCode", MessageField.CHR, 5, 0, MessageField.LEFT,  ' ', null, true, false));
        loop.addChild(new MessageField("itemAmt",  MessageField.NUM, 8, 0, MessageField.RIGHT, '0', null, true, false));
        structure.addField(loop);

        // "user01    " + "02" + "ITEM1" + "00001000" + "ITEM2" + "00002000"
        String raw = "user01    " + "02" + "ITEM1" + "00001000" + "ITEM2" + "00002000";
        byte[] bytes = raw.getBytes();

        // when
        Map<String, Object> result = parser.parse(structure, bytes);

        // then
        assertThat(result.get("userId")).isEqualTo("user01");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("itemCode")).isEqualTo("ITEM1");
        assertThat(items.get(0).get("itemAmt")).isEqualTo("00001000");
        assertThat(items.get(1).get("itemCode")).isEqualTo("ITEM2");
        assertThat(items.get(1).get("itemAmt")).isEqualTo("00002000");
    }

    @Test
    @DisplayName("반복 구조 — defaultValue 로 다른 필드에서 횟수 참조")
    void parse_loopField_countFromContext() {
        // given
        // 전문: cnt(2) + [code(5)] * cnt
        MessageStructure structure = new MessageStructure("ORG01", "REF_MSG", "F");
        structure.addField(new MessageField("cnt", MessageField.NUM, 2, 0, MessageField.RIGHT, '0', null, true, false));

        // 길이 0, defaultValue="cnt" → 이미 파싱된 cnt 값으로 반복 횟수 결정
        LoopField loop = new LoopField("codes", 0, 0, "cnt");
        loop.addChild(new MessageField("code", MessageField.CHR, 5, 0, MessageField.LEFT, ' ', null, true, false));
        structure.addField(loop);

        String raw = "03" + "AAA  " + "BBB  " + "CCC  ";
        byte[] bytes = raw.getBytes();

        // when
        Map<String, Object> result = parser.parse(structure, bytes);

        // then
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> codes = (List<Map<String, Object>>) result.get("codes");
        assertThat(codes).hasSize(3);
        assertThat(codes.get(0).get("code")).isEqualTo("AAA");
        assertThat(codes.get(1).get("code")).isEqualTo("BBB");
        assertThat(codes.get(2).get("code")).isEqualTo("CCC");
    }

    @Test
    @DisplayName("전문 길이 부족 시 남은 길이만 파싱 후 안전 종료")
    void parse_truncatedMessage() {
        // given
        MessageStructure structure = new MessageStructure("ORG01", "TRUNC_MSG", "F");
        structure.addField(new MessageField("field1", MessageField.CHR, 10, 0, MessageField.LEFT, ' ', null, true, false));
        structure.addField(new MessageField("field2", MessageField.CHR, 10, 0, MessageField.LEFT, ' ', null, true, false));

        // field2 가 없는 잘린 전문
        byte[] bytes = "HelloWorld".getBytes();

        // when — 예외 없이 종료되어야 함
        Map<String, Object> result = parser.parse(structure, bytes);

        // then
        assertThat(result.get("field1")).isEqualTo("HelloWorld");
        assertThat(result).doesNotContainKey("field2");
    }
}

package com.example.spideradmin.domain.codegen.generator;

import com.example.spideradmin.domain.messagefield.dto.MessageFieldResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 전문 필드 메타데이터를 기반으로 MyBatis Mapper XML 소스 코드를 생성하는 유틸리티 클래스.
 *
 * <p>루프 마커(_BeginLoop_ / _EndLoop_)는 DB 컬럼에 직접 매핑되지 않으므로 제외하고,
 * 최상위 레벨의 일반 필드만 resultMap / SELECT / INSERT 구문에 포함한다.
 * 루프 필드가 필요한 경우 &lt;collection&gt; 태그를 직접 추가하도록 TODO 주석을 삽입한다.
 */
public final class MapperXmlGenerator {

    private static final String LOOP_BEGIN = "_BeginLoop_";
    private static final String LOOP_END = "_EndLoop_";
    private static final String INDENT = "    ";
    private static final String BASE_PACKAGE = "com.example.generated";

    private MapperXmlGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 전문 ID, 전문명, 필드 목록을 받아 MyBatis Mapper XML 소스 코드 문자열을 생성한다.
     *
     * @param messageId   전문 ID (namespace / resultMap ID 생성에 사용)
     * @param messageName 전문명 (주석에 사용)
     * @param fields      전문 필드 목록 (SORT_ORDER 오름차순 정렬 상태여야 함)
     * @return 생성된 MyBatis Mapper XML 소스 코드
     */
    public static String generate(String messageId, String messageName, List<MessageFieldResponse> fields) {
        String className = DtoGenerator.toPascalCase(messageId);
        String resultMapId = DtoGenerator.toCamelCase(messageId) + "ResultMap";
        String dtoType = BASE_PACKAGE + ".dto." + className + "Dto";
        String namespace = BASE_PACKAGE + ".mapper." + className + "Mapper";

        // 루프 마커를 제외한 최상위 일반 필드만 추출
        List<MessageFieldResponse> flatFields = extractFlatFields(fields);

        boolean hasLoopFields = fields.stream()
                .anyMatch(f ->
                        f.getMessageFieldId() != null && f.getMessageFieldId().contains(LOOP_BEGIN));

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\"\n");
        sb.append(INDENT).append("\"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n");
        sb.append("<!--\n");
        sb.append("  전문 ID : ").append(messageId).append("\n");
        sb.append("  전문명 : ").append(messageName != null ? messageName : "").append("\n");
        sb.append("  ⚠ 자동 생성된 코드입니다. namespace와 테이블명은 실제 환경에 맞게 수정하세요.\n");
        sb.append("-->\n");
        sb.append("<mapper namespace=\"").append(namespace).append("\">\n\n");

        // resultMap
        sb.append(INDENT)
                .append("<resultMap id=\"")
                .append(resultMapId)
                .append("\" type=\"")
                .append(dtoType)
                .append("\">\n");
        for (MessageFieldResponse field : flatFields) {
            String property = DtoGenerator.toCamelCase(field.getMessageFieldId());
            String column = field.getMessageFieldId();
            sb.append(INDENT)
                    .append(INDENT)
                    .append("<result property=\"")
                    .append(property)
                    .append("\" column=\"")
                    .append(column)
                    .append("\"/>\n");
        }
        // 루프 필드가 있으면 collection 추가 안내 주석 삽입
        if (hasLoopFields) {
            sb.append(INDENT).append(INDENT).append("<!-- TODO: 반복부 필드는 <collection> 태그로 직접 추가하세요 -->\n");
        }
        sb.append(INDENT).append("</resultMap>\n\n");

        // SELECT
        sb.append(INDENT)
                .append("<select id=\"findAll\" resultMap=\"")
                .append(resultMapId)
                .append("\">\n");
        sb.append(INDENT).append(INDENT).append("SELECT\n");
        for (int i = 0; i < flatFields.size(); i++) {
            String column = flatFields.get(i).getMessageFieldId();
            String comma = (i < flatFields.size() - 1) ? "," : "";
            sb.append(INDENT)
                    .append(INDENT)
                    .append(INDENT)
                    .append(column)
                    .append(comma)
                    .append("\n");
        }
        sb.append(INDENT).append(INDENT).append("FROM /* TODO: 테이블명 입력 */\n");
        sb.append(INDENT).append("</select>\n\n");

        // INSERT
        sb.append(INDENT)
                .append("<insert id=\"insert\" parameterType=\"")
                .append(dtoType)
                .append("\">\n");
        sb.append(INDENT).append(INDENT).append("INSERT INTO /* TODO: 테이블명 입력 */ (\n");
        for (int i = 0; i < flatFields.size(); i++) {
            String column = flatFields.get(i).getMessageFieldId();
            String comma = (i < flatFields.size() - 1) ? "," : "";
            sb.append(INDENT)
                    .append(INDENT)
                    .append(INDENT)
                    .append(column)
                    .append(comma)
                    .append("\n");
        }
        sb.append(INDENT).append(INDENT).append(") VALUES (\n");
        for (int i = 0; i < flatFields.size(); i++) {
            String property = DtoGenerator.toCamelCase(flatFields.get(i).getMessageFieldId());
            String comma = (i < flatFields.size() - 1) ? "," : "";
            sb.append(INDENT)
                    .append(INDENT)
                    .append(INDENT)
                    .append("#{")
                    .append(property)
                    .append("}")
                    .append(comma)
                    .append("\n");
        }
        sb.append(INDENT).append(INDENT).append(")\n");
        sb.append(INDENT).append("</insert>\n\n");

        sb.append("</mapper>\n");
        return sb.toString();
    }

    /**
     * 루프 마커 및 루프 내부 필드를 제외한 최상위 일반 필드 목록을 반환한다.
     *
     * @param fields 전체 필드 목록
     * @return 루프 제외 최상위 필드 목록
     */
    private static List<MessageFieldResponse> extractFlatFields(List<MessageFieldResponse> fields) {
        List<MessageFieldResponse> result = new ArrayList<>();
        boolean insideLoop = false;

        for (MessageFieldResponse field : fields) {
            String fieldId = field.getMessageFieldId();
            if (fieldId != null && fieldId.contains(LOOP_BEGIN)) {
                // 루프 시작 — 내부 필드 제외
                insideLoop = true;
            } else if (fieldId != null && fieldId.contains(LOOP_END)) {
                // 루프 종료
                insideLoop = false;
            } else if (!insideLoop) {
                result.add(field);
            }
        }
        return result;
    }
}

package com.example.spider_admin.domain.codegen.generator;

import com.example.spider_admin.domain.messagefield.dto.MessageFieldResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 전문 필드 메타데이터를 기반으로 Java DTO 소스 코드를 생성하는 유틸리티 클래스.
 *
 * <p>FWK_MESSAGE_FIELD의 DATA_TYPE, DATA_LENGTH, SCALE 정보를 Java 타입으로 변환하며,
 * _BeginLoop_ / _EndLoop_ 마커를 감지하여 inner static class + List 구조로 생성한다.
 *
 * <pre>
 * 타입 변환 규칙:
 *   C (문자)  → String
 *   N (숫자)  → scale &gt; 0 이면 BigDecimal, length &gt;= 10 이면 Long, 그 외 Integer
 *   D (날짜)  → String
 * </pre>
 */
public final class DtoGenerator {

    private static final String LOOP_BEGIN = "_BeginLoop_";
    private static final String LOOP_END = "_EndLoop_";
    private static final String INDENT = "    ";

    private DtoGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 전문 ID, 전문명, 필드 목록을 받아 Java DTO 소스 코드 문자열을 생성한다.
     *
     * @param messageId   전문 ID (클래스명 생성에 사용)
     * @param messageName 전문명 (JavaDoc에 사용)
     * @param fields      전문 필드 목록 (SORT_ORDER 오름차순 정렬 상태여야 함)
     * @return 생성된 Java DTO 소스 코드
     */
    public static String generate(String messageId, String messageName, List<MessageFieldResponse> fields) {
        String className = toPascalCase(messageId) + "Dto";

        // BigDecimal 또는 List 사용 여부에 따라 import 여부 결정
        boolean needsBigDecimal = fields.stream().anyMatch(f -> "BigDecimal".equals(resolveJavaType(f)));
        boolean needsList = fields.stream()
                .anyMatch(f ->
                        f.getMessageFieldId() != null && f.getMessageFieldId().contains(LOOP_BEGIN));

        StringBuilder sb = new StringBuilder();

        // 패키지 선언 (실제 사용 시 수정 안내)
        sb.append("package com.example.generated.dto;\n\n");

        if (needsBigDecimal) sb.append("import java.math.BigDecimal;\n");
        if (needsList) sb.append("import java.util.List;\n");
        if (needsBigDecimal || needsList) sb.append("\n");

        sb.append("import lombok.AllArgsConstructor;\n");
        sb.append("import lombok.Builder;\n");
        sb.append("import lombok.Data;\n");
        sb.append("import lombok.NoArgsConstructor;\n\n");

        // 클래스 레벨 JavaDoc
        sb.append("/**\n");
        sb.append(" * 전문 ID : ").append(messageId).append("\n");
        sb.append(" * 전문명 : ").append(messageName != null ? messageName : "").append("\n");
        sb.append(" *\n");
        sb.append(" * <p>⚠ 자동 생성된 코드입니다. 패키지명은 실제 환경에 맞게 수정하세요.\n");
        sb.append(" */\n");

        sb.append("@Data\n");
        sb.append("@NoArgsConstructor\n");
        sb.append("@AllArgsConstructor\n");
        sb.append("@Builder\n");
        sb.append("public class ").append(className).append(" {\n\n");

        // 필드 + inner class 목록 수집
        List<InnerClassSpec> innerClasses = new ArrayList<>();
        appendFields(fields, sb, INDENT, innerClasses);

        // inner static class 출력
        for (InnerClassSpec inner : innerClasses) {
            sb.append(INDENT).append("@Data\n");
            sb.append(INDENT).append("@NoArgsConstructor\n");
            sb.append(INDENT).append("@AllArgsConstructor\n");
            sb.append(INDENT).append("@Builder\n");
            sb.append(INDENT)
                    .append("public static class ")
                    .append(inner.className)
                    .append(" {\n\n");
            appendFields(inner.fields, sb, INDENT + INDENT, new ArrayList<>());
            sb.append(INDENT).append("}\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 필드 목록을 순회하여 DTO 필드 선언문을 생성한다.
     * _BeginLoop_ 마커를 만나면 inner class 스펙을 수집하고 List 필드를 추가한다.
     */
    private static void appendFields(
            List<MessageFieldResponse> fields, StringBuilder sb, String indent, List<InnerClassSpec> innerClasses) {
        int i = 0;
        while (i < fields.size()) {
            MessageFieldResponse field = fields.get(i);
            String fieldId = field.getMessageFieldId();

            if (fieldId != null && fieldId.contains(LOOP_BEGIN)) {
                // 루프 이름 추출 (예: _BeginLoop_ITEM → ITEM)
                String loopName = fieldId.substring(fieldId.indexOf(LOOP_BEGIN) + LOOP_BEGIN.length());
                String endFieldId = fieldId.replace(LOOP_BEGIN, LOOP_END);

                // EndLoop 인덱스 탐색
                int endIdx = findEndLoopIndex(fields, i + 1, endFieldId);
                List<MessageFieldResponse> loopFields = endIdx > 0 ? fields.subList(i + 1, endIdx) : new ArrayList<>();

                String innerClassName = toPascalCase(loopName) + "Dto";
                String listFieldName = toCamelCase(loopName) + "List";

                sb.append(indent).append("/** ").append(loopName).append(" 반복부 목록 */\n");
                sb.append(indent)
                        .append("private List<")
                        .append(innerClassName)
                        .append("> ")
                        .append(listFieldName)
                        .append(";\n\n");

                innerClasses.add(new InnerClassSpec(innerClassName, new ArrayList<>(loopFields)));

                // EndLoop 다음으로 이동
                i = endIdx > 0 ? endIdx + 1 : i + 1;

            } else if (fieldId != null && fieldId.contains(LOOP_END)) {
                // EndLoop 마커는 appendFields 호출자가 이미 처리하므로 건너뜀
                i++;
            } else {
                appendSingleField(field, sb, indent);
                i++;
            }
        }
    }

    /** 단일 필드의 선언문을 생성한다. */
    private static void appendSingleField(MessageFieldResponse field, StringBuilder sb, String indent) {
        String javaType = resolveJavaType(field);
        String rawId = field.getMessageFieldId();
        // messageFieldId가 null이면 "unknownField"로 대체 (비정상 데이터 방어)
        String fieldName = (rawId != null) ? toCamelCase(rawId) : "unknownField";
        String comment = buildComment(field);

        sb.append(indent).append("/** ").append(comment).append(" */\n");
        sb.append(indent)
                .append("private ")
                .append(javaType)
                .append(" ")
                .append(fieldName)
                .append(";\n\n");
    }

    /**
     * DATA_TYPE, DATA_LENGTH, SCALE 기반으로 Java 타입 문자열을 반환한다.
     *
     * @param field 전문 필드 메타데이터
     * @return Java 타입 문자열 (String / Integer / Long / BigDecimal)
     */
    static String resolveJavaType(MessageFieldResponse field) {
        String dataType = field.getDataType();
        if (dataType == null) return "String";

        return switch (dataType.toUpperCase()) {
            case "N" -> {
                Integer scale = field.getScale();
                Long length = field.getDataLength();
                // scale > 0 이면 소수점 처리가 필요하므로 BigDecimal
                if (scale != null && scale > 0) yield "BigDecimal";
                // 10자리 이상 숫자는 Long (int 범위 초과 방지)
                if (length != null && length >= 10) yield "Long";
                yield "Integer";
            }
                // 날짜는 금융권 관례상 String으로 처리
            default -> "String";
        };
    }

    /** 필드 JavaDoc 주석 문자열을 생성한다. */
    private static String buildComment(MessageFieldResponse field) {
        String label = (field.getMessageFieldName() != null
                        && !field.getMessageFieldName().isBlank())
                ? field.getMessageFieldName()
                : field.getMessageFieldId();
        // dataType null 방어: null 대신 "?" 출력
        String dataTypeStr = field.getDataType() != null ? field.getDataType() : "?";
        String typeInfo = dataTypeStr + "/" + (field.getDataLength() != null ? field.getDataLength() : "?");
        return label + " (" + typeInfo + ")";
    }

    /** _EndLoop_ 마커의 인덱스를 반환한다. 없으면 -1. */
    private static int findEndLoopIndex(List<MessageFieldResponse> fields, int startIdx, String endFieldId) {
        for (int i = startIdx; i < fields.size(); i++) {
            if (endFieldId.equals(fields.get(i).getMessageFieldId())) return i;
        }
        return -1;
    }

    /**
     * snake_case 문자열을 camelCase로 변환한다.
     *
     * <p>예: HDR_TRAN_CD → hdrTranCd
     */
    static String toCamelCase(String input) {
        if (input == null || input.isBlank()) return input;
        // 선행 언더스코어 제거 후 분리
        String[] parts = input.replaceAll("^_+", "").split("_");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                sb.append(parts[i].substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * snake_case 문자열을 PascalCase로 변환한다.
     *
     * <p>예: HDR_TRAN_CD → HdrTranCd
     */
    static String toPascalCase(String input) {
        String camel = toCamelCase(input);
        if (camel == null || camel.isEmpty()) return camel;
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /** inner static class 생성 스펙을 담는 내부 레코드 대체 클래스. */
    private static class InnerClassSpec {
        final String className;
        final List<MessageFieldResponse> fields;

        InnerClassSpec(String className, List<MessageFieldResponse> fields) {
            this.className = className;
            this.fields = fields;
        }
    }
}

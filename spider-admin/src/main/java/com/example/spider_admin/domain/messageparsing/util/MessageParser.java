package com.example.spider_admin.domain.messageparsing.util;

import com.example.spider_admin.domain.messagefield.dto.MessageFieldResponse;
import com.example.spider_admin.domain.messageparsing.dto.ParsedFieldResponse;
import com.example.spider_admin.global.exception.InternalException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 전문(Telegram) 파싱 유틸리티
 *
 * 금융권 고정 길이 전문을 파싱하는 핵심 로직을 담당합니다.
 * - EUC-KR 인코딩 기반 바이트 단위 파싱
 * - ALIGN_TYPE에 따른 공백/제로 제거
 * - SCALE에 따른 소수점 처리
 * - 반복부(_BeginLoop_/_EndLoop_) 자동 횟수 판단 및 파싱
 */
@Slf4j
public final class MessageParser {

    private static final String ENCODING_EUC_KR = "EUC-KR";
    private static final Charset CHARSET_EUC_KR = Charset.forName(ENCODING_EUC_KR);
    private static final String LOOP_BEGIN_PREFIX = "_BeginLoop_";
    private static final String LOOP_END_PREFIX = "_EndLoop_";

    /**
     * 유틸리티 클래스의 인스턴스화를 방지합니다.
     */
    private MessageParser() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 전문 원본 문자열을 필드 목록에 따라 파싱합니다.
     * 반복부(_BeginLoop_/_EndLoop_)가 있는 경우, 반복부 이후 고정 필드 크기를 역산하여
     * 반복 횟수를 자동으로 판단합니다.
     *
     * @param rawString 전문 원본 데이터 (고정 길이)
     * @param fields    전문 필드 메타데이터 (SORT_ORDER로 정렬되어 있어야 함)
     * @return 파싱된 필드 목록
     */
    public static List<ParsedFieldResponse> parseMessage(String rawString, List<MessageFieldResponse> fields) {
        if (rawString == null || rawString.isEmpty()) {
            log.warn("Raw string is null or empty");
            return new ArrayList<>();
        }

        if (fields == null || fields.isEmpty()) {
            log.warn("Fields list is null or empty");
            return new ArrayList<>();
        }

        List<ParsedFieldResponse> parsedFields = new ArrayList<>();

        try {
            // 원본 문자열을 EUC-KR 바이트 배열로 변환
            byte[] rawBytes = rawString.getBytes(CHARSET_EUC_KR);
            int[] position = {0};

            log.debug("Parsing message: total length = {} bytes", rawBytes.length);

            parseFieldRange(rawBytes, fields, 0, fields.size(), position, parsedFields);

            log.info("Parsing completed: {} fields parsed, final position = {}", parsedFields.size(), position[0]);

        } catch (Exception e) {
            log.error("Error occurred while parsing message", e);
            throw new InternalException("전문 파싱 중 오류: " + e.getMessage(), e);
        }

        return parsedFields;
    }

    /**
     * 필드 목록의 지정 범위를 순회하며 파싱합니다.
     * 반복부를 만나면 횟수를 자동 판단하고 본문을 반복 파싱합니다.
     *
     * @param rawBytes     전문 바이트 배열
     * @param fields       전체 필드 목록
     * @param startIdx     처리 시작 인덱스 (inclusive)
     * @param endIdx       처리 종료 인덱스 (exclusive)
     * @param position     현재 바이트 위치 (mutable)
     * @param result       파싱 결과 누적 리스트
     */
    private static void parseFieldRange(
            byte[] rawBytes,
            List<MessageFieldResponse> fields,
            int startIdx,
            int endIdx,
            int[] position,
            List<ParsedFieldResponse> result) {
        int i = startIdx;
        while (i < endIdx) {
            MessageFieldResponse field = fields.get(i);
            String fieldId = field.getMessageFieldId();

            if (fieldId != null && fieldId.contains(LOOP_BEGIN_PREFIX)) {
                i = parseLoopFields(rawBytes, fields, i, endIdx, position, result);
            } else {
                parseSingleField(rawBytes, field, position, result);
                i++;
            }
        }
    }

    /**
     * 반복부(BeginLoop ~ EndLoop) 필드를 파싱합니다.
     * @return 다음 처리할 인덱스 (endLoopIdx + 1)
     */
    private static int parseLoopFields(
            byte[] rawBytes,
            List<MessageFieldResponse> fields,
            int beginLoopIdx,
            int endIdx,
            int[] position,
            List<ParsedFieldResponse> result) {
        MessageFieldResponse field = fields.get(beginLoopIdx);
        String fieldId = field.getMessageFieldId();
        String endFieldId = fieldId.replace(LOOP_BEGIN_PREFIX, LOOP_END_PREFIX);
        int endLoopIdx = findEndLoopIndex(fields, beginLoopIdx + 1, endIdx, endFieldId);

        if (endLoopIdx < 0) {
            log.warn("매칭되는 _EndLoop_ 를 찾을 수 없습니다: {}", fieldId);
            return beginLoopIdx + 1;
        }

        int oneSetSize = calculateFixedByteSize(fields, beginLoopIdx + 1, endLoopIdx);
        int postLoopSize = calculateFixedByteSize(fields, endLoopIdx + 1, endIdx);
        int remainingBytes = rawBytes.length - position[0];
        int bytesForLoop = remainingBytes - postLoopSize;
        int loopCount = oneSetSize > 0 ? Math.max(0, bytesForLoop / oneSetSize) : 0;

        log.debug(
                "Loop detected: {} | oneSetSize={}, postLoopSize={}, remaining={}, loopCount={}",
                fieldId,
                oneSetSize,
                postLoopSize,
                remainingBytes,
                loopCount);

        result.add(buildMarkerField(field, position[0], String.valueOf(loopCount)));

        for (int lc = 0; lc < loopCount; lc++) {
            for (int j = beginLoopIdx + 1; j < endLoopIdx; j++) {
                parseSingleField(rawBytes, fields.get(j), position, result);
            }
        }

        MessageFieldResponse endField = fields.get(endLoopIdx);
        result.add(buildMarkerField(endField, position[0], ""));

        return endLoopIdx + 1;
    }

    /**
     * 단일 필드를 파싱합니다.
     * dataLength가 0 이하인 필드(Loop 마커 외 기타)는 빈 값으로 추가합니다.
     */
    private static void parseSingleField(
            byte[] rawBytes, MessageFieldResponse field, int[] position, List<ParsedFieldResponse> result) {
        // dataLength가 없거나 0 이하: 빈 값으로 추가
        if (field.getDataLength() == null || field.getDataLength() <= 0) {
            log.debug("Skipping field '{}' with length: {}", field.getMessageFieldId(), field.getDataLength());
            result.add(buildMarkerField(field, position[0], ""));
            return;
        }

        int fieldLength = field.getDataLength().intValue();
        String rawValue = "";
        String parsedValue = "";

        if (position[0] < rawBytes.length) {
            int remainingBytes = rawBytes.length - position[0];
            int actualLength = Math.min(fieldLength, remainingBytes);

            if (remainingBytes < fieldLength) {
                log.debug(
                        "Remaining bytes ({}) less than field length ({}). Field: '{}' - using {} bytes",
                        remainingBytes,
                        fieldLength,
                        field.getMessageFieldId(),
                        actualLength);
            }

            byte[] fieldBytes = new byte[actualLength];
            System.arraycopy(rawBytes, position[0], fieldBytes, 0, actualLength);
            rawValue = new String(fieldBytes, CHARSET_EUC_KR);
            parsedValue = applyTransformations(rawValue, field);
        } else {
            log.debug(
                    "Current position ({}) exceeds raw bytes length ({}). Field '{}' will be empty",
                    position[0],
                    rawBytes.length,
                    field.getMessageFieldId());
        }

        ParsedFieldResponse parsedField = ParsedFieldResponse.builder()
                .messageId(field.getMessageId())
                .fieldId(field.getMessageFieldId())
                .fieldName(field.getMessageFieldName())
                .rawValue(rawValue)
                .parsedValue(parsedValue)
                .startPosition(position[0])
                .dataLength(field.getDataLength())
                .scale(field.getScale())
                .align(field.getAlign())
                .dataType(field.getDataType())
                .sortOrder(field.getSortOrder())
                .build();

        result.add(parsedField);

        log.debug(
                "Parsed field '{}': pos={}, len={}, raw='{}', parsed='{}'",
                field.getMessageFieldId(),
                position[0],
                fieldLength,
                rawValue.replace("\n", "\\n"),
                parsedValue.replace("\n", "\\n"));

        position[0] += fieldLength;
    }

    /**
     * Loop 마커 등 dataLength=0인 필드의 ParsedFieldDTO를 생성합니다.
     */
    private static ParsedFieldResponse buildMarkerField(
            MessageFieldResponse field, int currentPosition, String parsedValue) {
        return ParsedFieldResponse.builder()
                .messageId(field.getMessageId())
                .fieldId(field.getMessageFieldId())
                .fieldName(field.getMessageFieldName())
                .rawValue("")
                .parsedValue(parsedValue)
                .startPosition(currentPosition)
                .dataLength(0L)
                .scale(field.getScale())
                .align(field.getAlign())
                .dataType(field.getDataType())
                .sortOrder(field.getSortOrder())
                .build();
    }

    // ==================== 반복부 유틸리티 ====================

    /**
     * 매칭되는 _EndLoop_ 필드의 인덱스를 찾습니다.
     *
     * @param fields     필드 목록
     * @param startIdx   검색 시작 인덱스
     * @param endIdx     검색 종료 인덱스 (exclusive)
     * @param endFieldId 찾을 _EndLoop_ 필드 ID
     * @return EndLoop 인덱스, 없으면 -1
     */
    private static int findEndLoopIndex(
            List<MessageFieldResponse> fields, int startIdx, int endIdx, String endFieldId) {
        for (int i = startIdx; i < endIdx; i++) {
            if (endFieldId.equals(fields.get(i).getMessageFieldId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 지정 범위 내 필드들의 고정 바이트 크기 합계를 계산합니다.
     * Loop 마커(dataLength=0)는 0으로 처리됩니다.
     * 중첩된 반복부가 있는 경우 본문 1세트 분량으로 계산합니다.
     *
     * @param fields   필드 목록
     * @param startIdx 시작 인덱스 (inclusive)
     * @param endIdx   종료 인덱스 (exclusive)
     * @return 바이트 크기 합계
     */
    private static int calculateFixedByteSize(List<MessageFieldResponse> fields, int startIdx, int endIdx) {
        int totalSize = 0;
        int i = startIdx;

        while (i < endIdx) {
            MessageFieldResponse field = fields.get(i);
            String fieldId = field.getMessageFieldId();

            if (fieldId != null && fieldId.contains(LOOP_BEGIN_PREFIX)) {
                // 중첩 반복부: 본문 1세트 크기만 포함 (BeginLoop/EndLoop 마커 자체는 0)
                String endFieldId = fieldId.replace(LOOP_BEGIN_PREFIX, LOOP_END_PREFIX);
                int nestedEndIdx = findEndLoopIndex(fields, i + 1, endIdx, endFieldId);

                if (nestedEndIdx >= 0) {
                    // 중첩 반복부 본문의 1세트 크기 포함
                    totalSize += calculateFixedByteSize(fields, i + 1, nestedEndIdx);
                    i = nestedEndIdx + 1;
                } else {
                    i++;
                }
            } else {
                if (field.getDataLength() != null && field.getDataLength() > 0) {
                    totalSize += field.getDataLength().intValue();
                }
                i++;
            }
        }

        return totalSize;
    }

    // ==================== 변환 유틸리티 ====================

    /**
     * 파싱된 값에 정렬(ALIGN) 및 스케일(SCALE) 변환을 적용합니다.
     *
     * @param rawValue 원본 값
     * @param field    필드 메타데이터
     * @return 변환된 값
     */
    private static String applyTransformations(String rawValue, MessageFieldResponse field) {
        String value = rawValue;

        // 1. ALIGN_TYPE에 따른 공백/제로 제거
        value = applyAlignment(value, field.getAlign());

        // 2. SCALE 적용 (소수점 처리)
        if (field.getScale() != null && field.getScale() > 0) {
            value = applyScale(value, field.getScale());
        }

        return value;
    }

    /**
     * 정렬 타입에 따라 공백 또는 제로를 제거합니다.
     *
     * @param value 원본 값
     * @param align 정렬 타입 (L=Left, R=Right)
     * @return 처리된 값
     */
    public static String applyAlignment(String value, String align) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if ("L".equalsIgnoreCase(align)) {
            // Left 정렬 (문자열): 오른쪽 공백 제거
            return value.stripTrailing();
        } else if ("R".equalsIgnoreCase(align)) {
            // Right 정렬 (숫자): 왼쪽 공백 및 '0' 제거
            String trimmed = value.stripLeading();

            // 선행 제로 제거 (숫자인 경우)
            if (!trimmed.isEmpty() && isNumeric(trimmed)) {
                // "00123" -> "123", "00000" -> "0"
                trimmed = trimmed.replaceFirst("^0+(?=\\d)", "");
                if (trimmed.isEmpty()) {
                    trimmed = "0";
                }
            }

            return trimmed;
        }

        // 정렬 타입이 지정되지 않은 경우 양쪽 공백 제거
        return value.strip();
    }

    /**
     * SCALE 값에 따라 소수점을 삽입합니다.
     *
     * 예: value = "12345", scale = 2 -> "123.45"
     *
     * @param value 원본 값 (숫자 문자열)
     * @param scale 소수점 자리수
     * @return 소수점이 적용된 문자열
     */
    public static String applyScale(String value, int scale) {
        if (value == null || value.isEmpty() || scale <= 0) {
            return value;
        }

        // 공백 제거
        String trimmed = value.strip();

        // 숫자가 아닌 경우 공백이 제거된 값 반환
        if (!isNumeric(trimmed)) {
            return trimmed;
        }

        try {
            // 선행 제로 제거한 값
            String numericValue = trimmed.replaceFirst("^0+(?=\\d)", "");
            if (numericValue.isEmpty()) {
                numericValue = "0";
            }

            int length = numericValue.length();

            // 값이 스케일보다 짧은 경우
            if (length <= scale) {
                // "45", scale=2 -> "0.45"
                StringBuilder sb = new StringBuilder("0.");
                for (int i = 0; i < scale - length; i++) {
                    sb.append("0");
                }
                sb.append(numericValue);
                return sb.toString();
            } else {
                // "12345", scale=2 -> "123.45"
                int decimalPosition = length - scale;
                String integerPart = numericValue.substring(0, decimalPosition);
                String decimalPart = numericValue.substring(decimalPosition);
                return integerPart + "." + decimalPart;
            }
        } catch (Exception e) {
            log.error("Error applying scale to value: '{}', scale: {}", value, scale, e);
            return value;
        }
    }

    /**
     * 문자열이 숫자로만 구성되어 있는지 확인합니다.
     *
     * @param str 확인할 문자열
     * @return 숫자 여부
     */
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        // 부호(+, -) 및 소수점(.)을 허용하지 않음 (순수 정수만)
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 전문 원본 데이터의 바이트 길이를 계산합니다 (EUC-KR 기준).
     *
     * @param rawString 전문 원본 데이터
     * @return 바이트 길이
     */
    public static int calculateByteLength(String rawString) {
        if (rawString == null) {
            return 0;
        }

        try {
            return rawString.getBytes(CHARSET_EUC_KR).length;
        } catch (Exception e) {
            log.error("Error calculating byte length", e);
            return rawString.length(); // fallback to character length
        }
    }
}

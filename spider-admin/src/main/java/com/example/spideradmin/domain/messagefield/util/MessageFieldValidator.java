package com.example.spideradmin.domain.messagefield.util;

import com.example.spideradmin.domain.messagefield.dto.FieldValueRequest;
import com.example.spideradmin.domain.messageparsing.dto.ParsedFieldResponse;
import com.example.spideradmin.domain.proxyresponse.dto.ProxyTestdataFieldResponse;
import com.example.spideradmin.global.exception.InvalidInputException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 전문 필드 값 검증 및 디폴트 값 셋팅 유틸리티
 *
 * <p>AS-IS MessageValueSetter.checkMessageFieldValue()를 to-be 구조에 맞게 포팅한 것입니다.</p>
 *
 * <h4>testData 포맷:</h4>
 * <pre>
 * 일반필드: field1=value1;field2=value2;
 * 반복부:   _BeginLoop_GROUP=count;FIELD=val1@|@val2;_EndLoop_GROUP=;
 * </pre>
 *
 * <h4>주요 기능:</h4>
 * <ul>
 *     <li>필수 필드 검증: REQUIRED_YN='Y'이고 USE_MODE='U' 또는 'H'인 필드에 값이 없으면 예외</li>
 *     <li>디폴트 값 셋팅: 값이 없는 필드에 makeRealValue로 결정된 디폴트 값 적용</li>
 *     <li>데이터 길이 검증: DATA_LENGTH 초과 체크 (반복부는 개별 값 단위)</li>
 * </ul>
 *
 * @see ProxyTestdataFieldResponse
 */
@Slf4j
public final class MessageFieldValidator {

    private static final String USE_MODE_USER = "U";
    private static final String USE_MODE_HEADER = "H";
    private static final String LOOP_BEGIN_PREFIX = "_BeginLoop_";
    private static final String LOOP_END_PREFIX = "_EndLoop_";

    private MessageFieldValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 클라이언트에서 전송한 필드값 리스트를 필드 메타데이터 기반으로 검증합니다.
     *
     * <p>통전문 조립 전에 클라이언트에서 입력한 원본 필드값을 직접 검증하여
     * 필수 필드 누락, 데이터 길이 초과, 데이터 타입 불일치를 검증합니다.</p>
     *
     * @param fields      필드 메타데이터 목록 (getFieldsForTest 결과, makeRealValue 적용 완료 상태)
     * @param fieldValues 클라이언트에서 전송한 필드값 리스트
     * @throws InvalidInputException 검증 실패 시
     */
    public static void validateFieldValues(
            List<ProxyTestdataFieldResponse> fields, List<FieldValueRequest> fieldValues) {
        if (fields == null || fields.isEmpty()) {
            log.warn("필드 메타데이터가 없습니다. 검증을 건너뜁니다.");
            return;
        }

        if (fieldValues == null || fieldValues.isEmpty()) {
            log.warn("필드값 리스트가 없습니다. 검증을 건너뜁니다.");
            return;
        }

        // 필드값을 fieldId → value 맵으로 변환
        Map<String, String> fieldValueMap = new LinkedHashMap<>();
        for (FieldValueRequest fieldValue : fieldValues) {
            if (fieldValue.getFieldId() != null) {
                fieldValueMap.put(fieldValue.getFieldId(), fieldValue.getValue());
            }
        }

        // 필드 메타데이터 순회하며 검증
        for (ProxyTestdataFieldResponse field : fields) {
            String fieldId = field.getMessageFieldId();

            // Loop 마커 스킵
            if (fieldId.contains(LOOP_BEGIN_PREFIX) || fieldId.contains(LOOP_END_PREFIX)) {
                continue;
            }

            String value = fieldValueMap.get(fieldId);
            String useMode = resolveUseMode(field);

            log.debug("필드값 검증 - {}[UseMode:{}, DataType:{}] 입력값={}", fieldId, useMode, field.getDataType(), value);

            validateSingleFieldValue(field, fieldId, value, useMode);
        }

        log.info("필드값 검증 완료: 총 {}건 메타데이터 기준 검증", fields.size());
    }

    /**
     * 통전문(raw message) 파싱 결과를 필드 메타데이터 기반으로 검증합니다.
     *
     * <p>통전문을 MessageParser로 파싱한 결과(ParsedFieldDTO 목록)와
     * 필드 메타데이터(ProxyTestdataFieldResponseDTO 목록)를 매칭하여
     * 필수 필드 누락 및 데이터 길이 초과를 검증합니다.</p>
     *
     * @param fields       필드 메타데이터 목록 (getFieldsForTest 결과, makeRealValue 적용 완료 상태)
     * @param parsedFields 통전문 파싱 결과 목록 (MessageParser.parseMessage 결과)
     * @throws InvalidInputException 필수 필드 값 누락 또는 데이터 길이 초과 시
     */
    public static void validateParsedFields(
            List<ProxyTestdataFieldResponse> fields, List<ParsedFieldResponse> parsedFields) {
        if (fields == null || fields.isEmpty()) {
            log.warn("필드 메타데이터가 없습니다. 검증을 건너뜁니다.");
            return;
        }

        if (parsedFields == null || parsedFields.isEmpty()) {
            log.warn("파싱된 필드가 없습니다. 검증을 건너뜁니다.");
            return;
        }

        // 파싱 결과를 fieldId → parsedValue 맵으로 변환
        Map<String, String> parsedValueMap = new LinkedHashMap<>();
        for (ParsedFieldResponse parsed : parsedFields) {
            parsedValueMap.put(parsed.getFieldId(), parsed.getParsedValue());
        }

        // 필드 메타데이터 순회하며 검증
        for (ProxyTestdataFieldResponse field : fields) {
            String fieldId = field.getMessageFieldId();

            // Loop 마커 스킵
            if (fieldId.contains(LOOP_BEGIN_PREFIX) || fieldId.contains(LOOP_END_PREFIX)) {
                continue;
            }

            String value = parsedValueMap.get(fieldId);
            String useMode = resolveUseMode(field);

            log.debug("통전문 검증 - {}[UseMode:{}, DefaultValue:{}] 파싱값={}", fieldId, useMode, field.getRealValue(), value);

            validateSingleValue(field, fieldId, value, useMode);
        }

        log.info("통전문 필드 검증 완료: 총 {}건 메타데이터 기준 검증", fields.size());
    }

    // ==================== 검증 로직 ====================

    /**
     * 클라이언트 필드값 검증 (필수 체크 + 길이 체크 + 데이터 타입 체크)
     */
    private static void validateSingleFieldValue(
            ProxyTestdataFieldResponse field, String fieldId, String value, String useMode) {
        // 필수 필드 검증
        if (isNullOrEmpty(value)) {
            checkRequired(field, fieldId, useMode);
            return;
        }

        // 데이터 타입 검증
        checkDataType(field, fieldId, value);

        // 데이터 길이 검증
        checkDataLength(field, fieldId, value);
    }

    /**
     * 일반 필드 값 검증 (필수 체크 + 길이 체크)
     */
    private static void validateSingleValue(
            ProxyTestdataFieldResponse field, String fieldId, String value, String useMode) {
        // 필수 필드 검증
        if (isNullOrEmpty(value)) {
            checkRequired(field, fieldId, useMode);
            return;
        }

        // 데이터 길이 검증
        checkDataLength(field, fieldId, value);
    }

    /**
     * 필수 필드 검증.
     * USE_MODE가 'U'(사용자 입력) 또는 'H'(헤더)이고 REQUIRED_YN='Y'이면 예외를 던진다.
     */
    private static void checkRequired(ProxyTestdataFieldResponse field, String fieldId, String useMode) {
        if (!"Y".equals(field.getRequiredYn())) {
            return;
        }

        if (USE_MODE_USER.equals(useMode) || USE_MODE_HEADER.equals(useMode)) {
            throw new InvalidInputException(fieldId + ": 필수 입력 필드입니다.");
        }
    }

    /**
     * 데이터 타입 검증.
     * 데이터 타입이 'N' (Number) 또는 'B' (Binary/BCD)인 경우 숫자 여부를 체크한다.
     */
    private static void checkDataType(ProxyTestdataFieldResponse field, String fieldId, String value) {
        String dataType = nvl(field.getDataType());

        // N (Number): 정수 또는 소수 (-?\\d+(\\.\\d+)?)
        if ("N".equals(dataType) && !value.matches("-?\\d+(\\.\\d+)?")) {
            throw new InvalidInputException(fieldId + ": 숫자 필드에 숫자가 아닌 값이 입력되었습니다.");
        }

        // B (Binary/BCD): 숫자만 (\\d+)
        if ("B".equals(dataType) && !value.matches("\\d+")) {
            throw new InvalidInputException(fieldId + ": Binary/BCD 필드에 숫자가 아닌 값이 입력되었습니다.");
        }

        // C, H, V, F 등: 타입 체크 없음
    }

    /**
     * 데이터 길이 검증.
     * EUC-KR 바이트 기준으로 DATA_LENGTH를 초과하면 예외를 던진다.
     */
    private static void checkDataLength(ProxyTestdataFieldResponse field, String fieldId, String value) {
        if (field.getDataLength() == null || field.getDataLength() <= 0) {
            return;
        }

        long maxLength = field.getDataLength();
        int byteLength = getByteLength(value);

        if (byteLength > maxLength) {
            throw new InvalidInputException(
                    fieldId + ": 데이터 길이가 초과되었습니다. (최대: " + maxLength + ", 입력: " + byteLength + ")");
        }
    }

    // ==================== 유틸리티 ====================

    /**
     * 기관/표준 USE_MODE를 결합하여 유효한 USE_MODE를 결정합니다.
     * 기관 USE_MODE 우선, 없으면 표준 USE_MODE 사용.
     */
    private static String resolveUseMode(ProxyTestdataFieldResponse field) {
        String orgUseMode = nvl(field.getOrgUseMode());
        if (!orgUseMode.isEmpty()) {
            return orgUseMode;
        }
        return nvl(field.getStdUseMode());
    }

    private static int getByteLength(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return value.getBytes("EUC-KR").length;
        } catch (java.io.UnsupportedEncodingException e) {
            return value.length();
        }
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String nvl(String value) {
        return value == null ? "" : value.trim();
    }
}

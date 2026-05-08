package com.example.spiderlink.domain.message.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FWK_MESSAGE_FIELD 조회 결과 DTO.
 *
 * <p>MessageStructureCache가 DB에서 전문 필드 메타데이터를 로드할 때 사용한다.</p>
 */
@Data
@NoArgsConstructor
public class MessageFieldMeta {

    /** 기관 ID (FWK_MESSAGE_FIELD.ORG_ID) */
    private String orgId;

    /** 전문 ID (FWK_MESSAGE_FIELD.MESSAGE_ID) */
    private String messageId;

    /**
     * 필드 ID (FWK_MESSAGE_FIELD.MESSAGE_FIELD_ID).
     * "_BeginLoop_xxx" 이면 반복 구조 시작, "_EndLoop_" 이면 반복 구조 종료.
     */
    private String messageFieldId;

    /** 필드 표시 이름 (FWK_MESSAGE_FIELD.MESSAGE_FIELD_NAME) */
    private String messageFieldName;

    /** 정렬 순서 (FWK_MESSAGE_FIELD.SORT_ORDER) */
    private Integer sortOrder;

    /**
     * 데이터 타입 (FWK_MESSAGE_FIELD.DATA_TYPE).
     * C=문자, N=숫자, H=헥사, B=바이너리, K=한글
     */
    private String dataType;

    /** 필드 바이트 길이 (FWK_MESSAGE_FIELD.DATA_LENGTH) */
    private Long dataLength;

    /** 소수점 자릿수 (FWK_MESSAGE_FIELD.SCALE) */
    private Integer scale;

    /**
     * 정렬 방향 (FWK_MESSAGE_FIELD.ALIGN).
     * L=왼쪽정렬(문자), R=오른쪽정렬(숫자)
     */
    private String align;

    /** 여백 채우기 문자 (FWK_MESSAGE_FIELD.FILLER) */
    private String filler;

    /** 로그 마스킹 시 대체 문자 (FWK_MESSAGE_FIELD.REMARK) */
    private String remark;

    /** 로그 출력 여부 (FWK_MESSAGE_FIELD.LOG_YN) */
    private String logYn;

    /**
     * 기본값 (FWK_MESSAGE_FIELD.DEFAULT_VALUE).
     * LoopField에서 반복 횟수를 담고 있는 다른 필드명을 지정할 때 사용.
     */
    private String defaultValue;

    /**
     * 최대 반복 횟수 (FWK_MESSAGE_FIELD.FIELD_REPEAT_CNT).
     * LoopField의 길이 필드가 0일 때 고정 반복 횟수로 사용.
     */
    private Integer fieldRepeatCnt;

    /** 필수 여부 (FWK_MESSAGE_FIELD.REQUIRED_YN). Y=필수, N=선택 */
    private String requiredYn;
}

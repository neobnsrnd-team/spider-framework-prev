package com.example.spideradmin.domain.messagefield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MessageField 생성 요청 DTO
 * 새로운 전문 필드를 생성할 때 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldCreateRequest {

    @NotBlank(message = "기관 ID는 필수입니다")
    @Size(max = 20, message = "기관 ID는 20자 이내여야 합니다")
    private String orgId;

    @NotBlank(message = "전문 ID는 필수입니다")
    @Size(max = 50, message = "전문 ID는 50자 이내여야 합니다")
    private String messageId;

    @NotBlank(message = "필드 ID는 필수입니다")
    @Size(max = 50, message = "필드 ID는 50자 이내여야 합니다")
    private String messageFieldId;

    private Integer sortOrder;

    @Size(max = 20, message = "데이터 타입은 20자 이내여야 합니다")
    private String dataType;

    private Long dataLength;

    private Integer scale;

    @Size(max = 10, message = "정렬 방식은 10자 이내여야 합니다")
    private String align;

    @Size(max = 10, message = "필러는 10자 이내여야 합니다")
    private String filler;

    @Size(max = 20, message = "필드 타입은 20자 이내여야 합니다")
    private String fieldType;

    @Size(max = 20, message = "사용 모드는 20자 이내여야 합니다")
    private String useMode;

    @Size(max = 1, message = "필수 여부는 1자여야 합니다")
    private String requiredYn;

    @Size(max = 50, message = "필드 태그는 50자 이내여야 합니다")
    private String fieldTag;

    @Size(max = 50, message = "코드 그룹은 50자 이내여야 합니다")
    private String codeGroup;

    @Size(max = 200, message = "기본값은 200자 이내여야 합니다")
    private String defaultValue;

    @Size(max = 200, message = "테스트값은 200자 이내여야 합니다")
    private String testValue;

    @Size(max = 500, message = "비고는 500자 이내여야 합니다")
    private String remark;

    @Size(max = 1, message = "로그 여부는 1자여야 합니다")
    private String logYn;

    @Size(max = 1, message = "코드 매핑 여부는 1자여야 합니다")
    private String codeMappingYn;

    @Size(max = 200, message = "필드명은 200자 이내여야 합니다")
    private String messageFieldName;

    @Size(max = 500, message = "필드 설명은 500자 이내여야 합니다")
    private String messageFieldDesc;

    @Size(max = 50, message = "검증 규칙 ID는 50자 이내여야 합니다")
    private String validationRuleId;

    @Size(max = 100, message = "필드 포맷은 100자 이내여야 합니다")
    private String fieldFormat;

    @Size(max = 500, message = "필드 포맷 설명은 500자 이내여야 합니다")
    private String fieldFormatDesc;

    @Size(max = 100, message = "필드 옵션은 100자 이내여야 합니다")
    private String fieldOption;

    private Long fieldRepeatCnt;
}

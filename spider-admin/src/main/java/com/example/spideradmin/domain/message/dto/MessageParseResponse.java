package com.example.spideradmin.domain.message.dto;

import com.example.spideradmin.domain.messageparsing.dto.ParsedFieldResponse;
import java.util.List;
import lombok.*;

/**
 * 전문 파싱 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MessageParseResponse {

    /**
     * 기관 ID
     */
    private String orgId;

    /**
     * 전문 ID
     */
    private String messageId;

    /**
     * 전문명
     */
    private String messageName;

    /**
     * 전문 설명
     */
    private String messageDesc;

    /**
     * 원본 전문 데이터
     */
    private String rawString;

    /**
     * 전문 전체 길이 (바이트)
     */
    private int totalLength;

    /**
     * 파싱된 필드 목록
     */
    private List<ParsedFieldResponse> fields;
}

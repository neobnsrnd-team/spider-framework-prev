package com.example.spideradmin.domain.errorhistory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 오류 발생 이력 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorHisResponse {

    /**
     * 오류 코드
     */
    private String errorCode;

    /**
     * 오류 일련번호
     */
    private String errorSerNo;

    /**
     * 고객 사용자 ID
     */
    private String custUserId;

    /**
     * 오류 메시지
     */
    private String errorMessage;

    /**
     * 오류 발생 일시
     */
    private String errorOccurDtime;

    /**
     * 오류 URL
     */
    private String errorUrl;

    /**
     * 오류 트레이스
     */
    private String errorTrace;

    // ===== 조인된 추가 정보 =====

    /**
     * 오류 제목 (FWK_ERROR 조인)
     */
    private String errorTitle;

    /**
     * 오류 레벨 (FWK_ERROR 조인)
     */
    private String errorLevel;

    /**
     * 오류 레벨명 (UI 표시용)
     */
    private String errorLevelName;

    /**
     * 고객 전화번호 (FWK_USER 조인)
     */
    private String custPhoneNo;

    /**
     * 발생 인스턴스 ID
     */
    private String errorInstanceId;
}

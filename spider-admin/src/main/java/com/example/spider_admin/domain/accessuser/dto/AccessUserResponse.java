package com.example.spider_admin.domain.accessuser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h3>중지거래 접근허용자 응답 DTO</h3>
 * <p>중지거래 접근허용자 조회 결과를 담는 DTO</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessUserResponse {

    /**
     * 구분유형 (T=거래/S=서비스)
     */
    private String gubunType;

    /**
     * 거래/서비스 ID
     */
    private String trxId;

    /**
     * 접근허용 고객 사용자ID
     */
    private String custUserId;

    /**
     * 사용여부 (Y/N)
     */
    private String useYn;

    /**
     * 최종 수정 일시
     */
    private String lastUpdateDtime;

    /**
     * 최종 수정자 ID
     */
    private String lastUpdateUserId;
}

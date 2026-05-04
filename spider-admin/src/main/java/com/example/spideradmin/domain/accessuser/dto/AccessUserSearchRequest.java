package com.example.spideradmin.domain.accessuser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h3>중지거래 접근허용자 검색 요청 DTO</h3>
 * <p>중지거래 접근허용자 검색 조건을 담는 DTO</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessUserSearchRequest {

    /**
     * 거래/서비스 ID (부분 검색)
     */
    private String trxId;

    /**
     * 구분유형 (T=거래/S=서비스, 전체="")
     */
    private String gubunType;

    /**
     * 접근허용 고객 사용자ID (부분 검색)
     */
    private String custUserId;
}

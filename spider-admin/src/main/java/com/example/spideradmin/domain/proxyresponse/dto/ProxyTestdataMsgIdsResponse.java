package com.example.spideradmin.domain.proxyresponse.dto;

import lombok.*;

/**
 * 거래-전문 매핑 응답 DTO
 * 거래 ID에 매핑된 기관 전문 ID와 표준 전문 ID를 담는 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProxyTestdataMsgIdsResponse {

    private String messageId;
    private String stdMessageId;
}

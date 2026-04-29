package com.example.spideradmin.domain.trxmessage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 거래-전문 매핑 변경 Request DTO (부분 업데이트)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrxMessageMappingUpdateRequest {

    /**
     * 매핑 대상 기관 ID (신규 생성 시 필요)
     */
    private String orgId;

    /**
     * 기관 요청 전문 ID (IO_TYPE='O' 행의 MESSAGE_ID)
     */
    private String messageId;

    /**
     * 표준 요청 전문 ID (IO_TYPE='O' 행의 STD_MESSAGE_ID)
     */
    private String stdMessageId;

    /**
     * 기관 응답 전문 ID (IO_TYPE='I' 행의 RES_MESSAGE_ID)
     */
    private String resMessageId;

    /**
     * 표준 응답 전문 ID (IO_TYPE='I' 행의 STD_RES_MESSAGE_ID)
     */
    private String stdResMessageId;

    /**
     * 타임아웃 (초)
     */
    private Integer timeoutSec;

    /**
     * 헥사로그여부 (Y/N)
     */
    private String hexLogYn;

    /**
     * 응답여부 (Y/N)
     */
    private String proxyResYn;

    /**
     * 응답포맷다수여부 (Y/N)
     */
    private String multiResYn;

    /**
     * 다수응답타입 (0:없음, 1:1개, M:여러개)
     */
    private String multiResType;

    /**
     * 응답전문구분필드ID
     */
    private String resTypeFieldId;
}

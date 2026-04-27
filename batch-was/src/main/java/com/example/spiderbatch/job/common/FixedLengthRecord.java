package com.example.spiderbatch.job.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 고정 길이 금융 거래내역 전문 도메인 클래스.
 *
 * <p>파일 레이아웃 (총 61자):</p>
 * <pre>
 * ACCOUNT_NO    (1~10,  10자) 거래계좌번호
 * TRX_DT        (11~18,  8자) 거래일자 (YYYYMMDD)
 * TRX_TM        (19~24,  6자) 거래시각 (HHMMSS)
 * AMOUNT        (25~39, 15자) 거래금액 (우측정렬, 0패딩)
 * TRX_TYPE_CODE (40~41,  2자) 거래구분코드 (01=입금, 02=출금)
 * MEMO          (42~61, 20자) 적요 (좌측정렬, 공백패딩)
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedLengthRecord {

    /** 거래계좌번호 (10자) */
    private String accountNo;

    /** 거래일자 (YYYYMMDD, 8자) */
    private String trxDt;

    /** 거래시각 (HHMMSS, 6자) */
    private String trxTm;

    /**
     * 거래금액 (15자, 우측정렬 0패딩).
     * DB에 문자열로 저장하므로 String 타입 유지.
     * Processor에서 trim 처리 후 적재.
     */
    private String amount;

    /** 거래구분코드 (2자): 01=입금, 02=출금 */
    private String trxTypeCode;

    /** 적요 (20자, 좌측정렬 공백패딩) */
    private String memo;
}

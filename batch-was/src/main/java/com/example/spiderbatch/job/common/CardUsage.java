package com.example.spiderbatch.job.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POC_카드사용내역 데이터 모델.
 * db2db Job(카드사용내역 아카이브), db2foreign Job(외부 전문 연계)에서 공통 사용.
 * SQL 조회 시 한글 컬럼명을 영문 alias로 매핑한다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardUsage {

    /** 이용자 (사용자 ID) — POC_USER.USER_ID 참조 */
    private String userId;

    /** 카드번호 */
    private String cardNo;

    /** 이용일자 (YYYYMMDD) */
    private String usageDt;

    /** 이용가맹점 */
    private String merchant;

    /** 이용금액 */
    private Long amount;

    /** 할부개월 (0·1 = 일시불, 2 이상 = 할부) */
    private Integer installmentMonths;

    /** 현재 할부 회차 (전체 할부개월 중 몇 번째) */
    private Integer installmentRound;

    /** 할부구분코드 (예: 00=일시불, 01=할부) */
    private String installmentTypeCode;

    /** 승인여부 (Y = 정상승인, N = 취소) */
    private String approvalYn;

    /** 카드명 */
    private String cardName;

    /** 승인시각 (HHmmss) */
    private String approvalTime;

    /** 결제예정일 (YYMMDD) */
    private String paymentDueDate;

    /** 승인번호 */
    private String approvalNo;

    /** 결제잔액 — 아직 결제되지 않은 금액 */
    private Long paymentBalance;

    /** 누적결제금액 */
    private Long cumulativeAmount;

    /** 결제상태코드 (0=미결제, 1=완납, 2=부분결제, 9=취소) */
    private String paymentStatusCode;

    /** 최종결제일자 (YYYYMMDD) */
    private String lastPaymentDt;
}

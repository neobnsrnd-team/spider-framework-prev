package com.example.bizcommon;

/**
 * @file BizCommands.java
 * @description AP 서버 간 TCP 통신에 사용되는 커맨드 이름 상수 모음.
 *
 * <p>커맨드 체계:</p>
 * <pre>
 *   AUTH_*      — 인증AP (biz-auth, TCP port 19100)
 *   TRANSFER_*  — 이체AP (biz-transfer, TCP port 19200)
 *   CORE_*      — 계정계 Mock (mock-core, TCP port 19300)
 * </pre>
 */
public final class BizCommands {

    // ──────────────────────────────────────────────────────────────────────────
    // 인증AP (biz-auth, port 19100) — 채널AP(biz-channel) → 인증AP
    // ──────────────────────────────────────────────────────────────────────────

    /** 사용자 로그인: {userId, password} → {userId, userName, userGrade, lastLoginDtime} */
    public static final String AUTH_LOGIN = "AUTH_LOGIN";

    /** 현재 사용자 정보 조회: {userId} → {userName, userGrade, lastLoginDtime} */
    public static final String AUTH_ME = "AUTH_ME";

    // ──────────────────────────────────────────────────────────────────────────
    // 이체AP (biz-transfer, port 19200) — 채널AP(biz-channel) → 이체AP
    // ──────────────────────────────────────────────────────────────────────────

    /** 카드 목록 조회: {userId} → {cards:[...]} */
    public static final String TRANSFER_CARD_LIST = "TRANSFER_CARD_LIST";

    /** 카드 이용내역 조회: {userId, cardId?, period?, ...} → {transactions:[...], totalCount, paymentSummary} */
    public static final String TRANSFER_TRANSACTIONS = "TRANSFER_TRANSACTIONS";

    /** 결제예정금액·이용대금명세서 조회: {userId, yearMonth?, paymentDay?} → {dueDate, totalAmount, items:[...], cardInfo, billingPeriod} */
    public static final String TRANSFER_PAYMENT_STMT = "TRANSFER_PAYMENT_STMT";

    /** 즉시결제 가능금액 조회: {userId, cardId} → {payableAmount, creditLimit} */
    public static final String TRANSFER_PAYABLE_AMT = "TRANSFER_PAYABLE_AMT";

    /** 즉시결제 처리: {userId, cardId, pin, amount, accountNumber} → {paidAmount, processedCount, completedAt} */
    public static final String TRANSFER_IMMEDIATE_PAY = "TRANSFER_IMMEDIATE_PAY";

    /** PIN 시도 횟수 초기화: {userId, cardId} → {ok: true} */
    public static final String TRANSFER_RESET_PIN_ATTEMPTS = "TRANSFER_RESET_PIN_ATTEMPTS";

    // ──────────────────────────────────────────────────────────────────────────
    // 계정계 Mock (mock-core, port 19300) — 인증AP/이체AP → 계정계
    // ──────────────────────────────────────────────────────────────────────────

    /** 사용자 인증: {userId, password} → {userId, userName, userGrade, lastLoginDtime} */
    public static final String CORE_USER_AUTH = "CORE_USER_AUTH";

    /** 사용자 정보 조회: {userId} → {userName, userGrade, lastLoginDtime} */
    public static final String CORE_USER_QUERY = "CORE_USER_QUERY";

    /** 카드 목록 조회: {userId} → {cards:[...]} */
    public static final String CORE_CARD_LIST = "CORE_CARD_LIST";

    /** 카드 이용내역 조회: {userId, cardId?, fromDate?, toDate?, usageType?} → {transactions:[...], totalCount, paymentSummary} */
    public static final String CORE_TRANSACTIONS = "CORE_TRANSACTIONS";

    /** 결제예정금액·이용대금명세서 조회: {userId, yearMonth?, paymentDay?} → {dueDate, totalAmount, items:[...], cardInfo, billingPeriod} */
    public static final String CORE_PAYMENT_STMT = "CORE_PAYMENT_STMT";

    /** 즉시결제 가능금액 조회: {userId, cardId} → {payableAmount, creditLimit} */
    public static final String CORE_PAYABLE_AMT = "CORE_PAYABLE_AMT";

    /** 즉시결제 처리: {userId, cardId, amount, accountNumber} → {paidAmount, processedCount, completedAt} */
    public static final String CORE_IMMEDIATE_PAY = "CORE_IMMEDIATE_PAY";

    private BizCommands() {
        // 유틸리티 클래스 — 인스턴스화 불가
    }
}

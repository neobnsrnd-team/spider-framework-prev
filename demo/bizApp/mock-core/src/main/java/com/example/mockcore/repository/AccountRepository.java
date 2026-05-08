package com.example.mockcore.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 계정계 Mock Oracle DB 접근 레포지토리.
 *
 * <p>D_SPIDERLINK 스키마의 사용자·카드·거래·계좌 테이블에 대한 모든 조회/처리 로직을 담당한다.
 * 한글 테이블명 및 컬럼명은 Oracle JDBC가 정상 지원하므로 큰따옴표(")로 감싸서 사용한다.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    // ──────────────────────────────────────────────────────────────────────────
    // 사용자 관련
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 사용자 ID·비밀번호로 인증하고, 성공 시 LAST_LOGIN_DTIME을 현재 시각으로 갱신한다.
     *
     * <p>DB에는 BCrypt 해시가 저장되어 있어야 한다.
     * 테스트 데이터 마이그레이션은 admin/docs/sql/oracle/01_create_tables.sql 참고.</p>
     *
     * @param userId   사용자 ID
     * @param password 입력 비밀번호 (평문 — BCrypt matches()로 해시와 비교)
     * @return userId, userName, userGrade, lastLoginDtime 를 담은 Map
     * @throws IllegalArgumentException 사용자 미존재 또는 비밀번호 불일치 시
     */
    public Map<String, Object> authenticateUser(String userId, String password) {
        String sql = "SELECT USER_ID, USER_NAME, USER_GRADE, PASSWORD, LAST_LOGIN_DTIME "
                + "FROM D_SPIDERLINK.POC_USER WHERE USER_ID = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        Map<String, Object> row = rows.get(0);
        String storedPassword = row.get("PASSWORD") != null ? row.get("PASSWORD").toString() : "";

        // BCrypt 해시와 입력 평문 비밀번호 비교
        if (!passwordEncoder.matches(password, storedPassword)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 로그인 성공 시 최종 로그인 일시 갱신
        jdbcTemplate.update(
                "UPDATE D_SPIDERLINK.POC_USER SET LAST_LOGIN_DTIME = TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE USER_ID = ?",
                userId);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", row.get("USER_ID") != null ? row.get("USER_ID").toString() : "");
        result.put("userName", row.get("USER_NAME") != null ? row.get("USER_NAME").toString() : "");
        result.put("userGrade", row.get("USER_GRADE") != null ? row.get("USER_GRADE").toString() : "");
        // LAST_LOGIN_DTIME은 방금 갱신했으므로 DB에서 재조회
        String newLoginDtime = jdbcTemplate.queryForObject(
                "SELECT LAST_LOGIN_DTIME FROM D_SPIDERLINK.POC_USER WHERE USER_ID = ?",
                String.class, userId);
        result.put("lastLoginDtime", newLoginDtime != null ? newLoginDtime : "");
        return result;
    }

    /**
     * 사용자 ID로 사용자 정보를 조회한다.
     *
     * @param userId 조회할 사용자 ID
     * @return userName, userGrade, lastLoginDtime 를 담은 Map
     * @throws IllegalArgumentException 사용자 미존재 시
     */
    public Map<String, Object> findUserById(String userId) {
        String sql = "SELECT USER_ID, USER_NAME, USER_GRADE, LAST_LOGIN_DTIME "
                + "FROM D_SPIDERLINK.POC_USER WHERE USER_ID = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId);
        }

        Map<String, Object> row = rows.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", row.get("USER_ID") != null ? row.get("USER_ID").toString() : "");
        result.put("userName", row.get("USER_NAME") != null ? row.get("USER_NAME").toString() : "");
        result.put("userGrade", row.get("USER_GRADE") != null ? row.get("USER_GRADE").toString() : "");
        result.put("lastLoginDtime", row.get("LAST_LOGIN_DTIME") != null ? row.get("LAST_LOGIN_DTIME").toString() : "");
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 카드 관련
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 사용자 ID에 해당하는 카드 목록을 프론트엔드 포맷으로 변환하여 반환한다.
     *
     * @param userId 사용자 ID
     * @return 카드 정보 Map 목록 (id, maskedNumber, brand, balance 등)
     */
    public List<Map<String, Object>> findCardsByUserId(String userId) {
        String sql = "SELECT \"카드번호\", \"카드구분\", \"유효기간\", \"결제은행명\", \"결제계좌\", "
                + "\"결제일\", \"한도금액\", \"사용금액\", \"결제순번\" "
                + "FROM D_SPIDERLINK.\"POC_카드리스트\" WHERE \"사용자아이디\" = ? "
                + "ORDER BY \"결제순번\" ASC";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
        List<Map<String, Object>> cards = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            cards.add(mapCardRow(row));
        }
        return cards;
    }

    /**
     * DB 카드 Row를 프론트엔드 카드 포맷 Map으로 변환한다.
     *
     * @param row DB queryForList 결과 Row
     * @return 프론트엔드용 카드 정보 Map
     */
    private Map<String, Object> mapCardRow(Map<String, Object> row) {
        Map<String, Object> card = new HashMap<>();

        String cardNo = row.get("카드번호") != null ? row.get("카드번호").toString() : "";
        card.put("id", cardNo);
        card.put("name", row.get("카드구분") != null ? row.get("카드구분").toString() : "");

        // 마스킹: 앞 6자리 + **** + 뒤 4자리
        String masked = cardNo.length() >= 10
                ? cardNo.substring(0, 6) + "****" + cardNo.substring(cardNo.length() - 4)
                : cardNo;
        card.put("maskedNumber", masked);

        // 카드 번호 첫 자리로 브랜드 판별 (4: VISA, 5: Mastercard, 그 외: 기타)
        String brand = cardNo.startsWith("4") ? "VISA"
                : cardNo.startsWith("5") ? "Mastercard"
                : "기타";
        card.put("brand", brand);

        long limit = row.get("한도금액") != null ? ((Number) row.get("한도금액")).longValue() : 0L;
        long used = row.get("사용금액") != null ? ((Number) row.get("사용금액")).longValue() : 0L;
        card.put("balance", limit - used);
        card.put("expiry", row.get("유효기간") != null ? row.get("유효기간").toString() : "");
        card.put("paymentBank", row.get("결제은행명") != null ? row.get("결제은행명").toString() : "");
        card.put("paymentAccount", row.get("결제계좌") != null ? row.get("결제계좌").toString() : "");
        card.put("paymentDay", row.get("결제일") != null ? row.get("결제일").toString() : "");
        card.put("limitAmount", limit);
        card.put("usedAmount", used);
        return card;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 카드 이용내역 관련
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 카드 이용내역을 조건에 따라 조회하고 프론트엔드 포맷으로 반환한다.
     *
     * @param userId    사용자 ID (필수)
     * @param cardId    카드번호 (null이면 전체 카드)
     * @param fromDate  조회 시작일자 (YYYYMMDD, null이면 제한 없음)
     * @param toDate    조회 종료일자 (YYYYMMDD, null이면 제한 없음)
     * @param usageType 이용유형 필터 ("일시불"/"할부"/"취소", null이면 전체)
     * @return {transactions, totalCount, paymentSummary} Map
     */
    public Map<String, Object> findTransactions(String userId, String cardId,
                                                String fromDate, String toDate,
                                                String usageType) {
        StringBuilder sql = new StringBuilder(
                "SELECT \"카드번호\", \"카드명\", \"이용일자\", \"이용가맹점\", \"이용금액\", "
                + "\"결제잔액\", \"누적결제금액\", \"결제상태코드\", \"결제예정일\", "
                + "\"할부개월\", \"승인여부\", \"승인시각\", \"승인번호\", \"최종결제일자\" "
                + "FROM D_SPIDERLINK.\"POC_카드사용내역\" WHERE \"이용자\" = ?");

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (cardId != null && !cardId.isBlank()) {
            sql.append(" AND \"카드번호\" = ?");
            params.add(cardId);
        }
        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND \"이용일자\" >= ?");
            params.add(fromDate);
        }
        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND \"이용일자\" <= ?");
            params.add(toDate);
        }
        // 이용유형 필터: 승인여부(Y/N)와 할부개월 조합으로 판별
        if ("취소".equals(usageType)) {
            sql.append(" AND \"승인여부\" = 'N'");
        } else if ("할부".equals(usageType)) {
            sql.append(" AND \"승인여부\" = 'Y' AND \"할부개월\" > 1");
        } else if ("일시불".equals(usageType)) {
            sql.append(" AND \"승인여부\" = 'Y' AND (\"할부개월\" IS NULL OR \"할부개월\" <= 1)");
        }

        sql.append(" ORDER BY \"이용일자\" DESC, \"승인시각\" DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());

        List<Map<String, Object>> transactions = new ArrayList<>();
        long totalAmount = 0L;

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Map<String, Object> txMap = mapTransactionRow(row, i);
            transactions.add(txMap);

            boolean approved = "Y".equals(row.get("승인여부"));
            long amount = row.get("이용금액") != null ? ((Number) row.get("이용금액")).longValue() : 0L;
            // 승인된 거래 금액만 결제예정 합산
            if (approved) {
                totalAmount += amount;
            }
        }

        // paymentSummary: 이번 달 결제 예정 요약
        Map<String, Object> paymentSummary = new HashMap<>();
        paymentSummary.put("totalAmount", totalAmount);
        // 결제예정일이 있는 경우 첫 번째 레코드의 결제예정일 사용
        String summaryDate = "";
        if (!rows.isEmpty() && rows.get(0).get("결제예정일") != null) {
            String raw = rows.get(0).get("결제예정일").toString(); // YYYYMMDD
            summaryDate = formatToMonthDay(raw);
        }
        paymentSummary.put("date", summaryDate);

        Map<String, Object> result = new HashMap<>();
        result.put("transactions", transactions);
        result.put("totalCount", transactions.size());
        result.put("paymentSummary", paymentSummary);
        return result;
    }

    /**
     * DB 카드사용내역 Row를 프론트엔드 거래내역 포맷 Map으로 변환한다.
     *
     * @param row DB Row
     * @param idx 목록 내 인덱스 (unique ID 생성에 사용)
     * @return 프론트엔드용 거래내역 Map
     */
    private Map<String, Object> mapTransactionRow(Map<String, Object> row, int idx) {
        Map<String, Object> txMap = new HashMap<>();

        String cardNo = row.get("카드번호") != null ? row.get("카드번호").toString() : "";
        String date = row.get("이용일자") != null ? row.get("이용일자").toString() : "";
        String time = row.get("승인시각") != null ? row.get("승인시각").toString() : "000000";

        boolean approved = "Y".equals(row.get("승인여부"));
        int installment = row.get("할부개월") != null ? ((Number) row.get("할부개월")).intValue() : 0;
        // 거래 유형: 취소 > 할부(N개월) > 일시불 순으로 판별
        String type = !approved ? "취소"
                : installment > 1 ? "할부(" + installment + "개월)"
                : "일시불";

        long amount = row.get("이용금액") != null ? ((Number) row.get("이용금액")).longValue() : 0L;

        txMap.put("id", cardNo + "-" + date + "-" + time + "-" + idx);
        txMap.put("merchant", row.get("이용가맹점") != null ? row.get("이용가맹점").toString() : "");
        // 취소 거래는 음수 금액으로 표현
        txMap.put("amount", approved ? amount : -amount);
        txMap.put("date", formatDateTime(date, time));
        txMap.put("type", type);
        txMap.put("approvalNumber", row.get("승인번호") != null ? row.get("승인번호").toString() : "");
        txMap.put("status", approved ? "승인" : "취소");
        txMap.put("cardName", row.get("카드명") != null ? row.get("카드명").toString() : "");
        return txMap;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 결제예정금액·이용대금명세서 관련
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 이용대금명세서 정보를 조회한다.
     *
     * <p>yearMonth가 없으면 이번 달 기준으로 조회하며,
     * paymentDay가 없으면 사용자의 첫 번째 카드 결제일을 사용한다.</p>
     *
     * @param userId     사용자 ID
     * @param yearMonth  조회 연월 (YYYYMM, null이면 현재 월)
     * @param paymentDay 결제일 (null이면 카드 설정값 사용)
     * @return dueDate, totalAmount, items, cardInfo, billingPeriod 를 담은 Map
     */
    public Map<String, Object> findPaymentStatement(String userId, String yearMonth, String paymentDay) {
        // 연월이 없으면 이번 달 사용
        String targetYearMonth = (yearMonth != null && !yearMonth.isBlank())
                ? yearMonth
                : jdbcTemplate.queryForObject(
                "SELECT TO_CHAR(SYSDATE, 'YYYYMM') FROM DUAL", String.class);

        // 카드 목록 조회 (결제 정보 포함)
        List<Map<String, Object>> cards = jdbcTemplate.queryForList(
                "SELECT \"카드번호\", \"카드구분\", \"결제은행명\", \"결제계좌\", \"결제일\", \"사용금액\" "
                + "FROM D_SPIDERLINK.\"POC_카드리스트\" WHERE \"사용자아이디\" = ? ORDER BY \"결제순번\" ASC",
                userId);

        // 결제일 결정: 파라미터 우선, 없으면 첫 번째 카드 결제일, 없으면 25일 기본값
        String effectivePaymentDay = paymentDay;
        if ((effectivePaymentDay == null || effectivePaymentDay.isBlank()) && !cards.isEmpty()) {
            Object cardPayDay = cards.get(0).get("결제일");
            effectivePaymentDay = cardPayDay != null ? cardPayDay.toString() : "25";
        }
        if (effectivePaymentDay == null || effectivePaymentDay.isBlank()) {
            effectivePaymentDay = "25"; // 기본 결제일
        }

        // 하나카드 결제일별 신용공여기간(일시불·할부) 기준:
        // D <= 12: 전전월 (D+18)일 ~ 전월 (D+17)일
        // D == 13: 전월 1일 ~ 전월 말일
        // D >= 14: 전월 (D-12)일 ~ 당월 (D-13)일
        YearMonth paymentYM  = YearMonth.parse(targetYearMonth, YEAR_MONTH_FMT);
        YearMonth prevYM     = paymentYM.minusMonths(1);
        YearMonth prevPrevYM = paymentYM.minusMonths(2);
        // trim(): DB CHAR 컬럼 등에서 공백이 포함될 경우 NumberFormatException 방지
        int d = Integer.parseInt(effectivePaymentDay.trim());
        LocalDate billingFromDate, billingToDate;
        if (d <= 12) {
            billingFromDate = prevPrevYM.atDay(Math.min(d + 18, prevPrevYM.lengthOfMonth()));
            billingToDate   = prevYM.atDay(Math.min(d + 17, prevYM.lengthOfMonth()));
        } else if (d == 13) {
            billingFromDate = prevYM.atDay(1);
            billingToDate   = prevYM.atEndOfMonth();
        } else {
            billingFromDate = prevYM.atDay(d - 12);
            billingToDate   = paymentYM.atDay(d - 13);
        }
        String billingFrom = billingFromDate.format(DATE_FMT);
        String billingTo   = billingToDate.format(DATE_FMT);

        // %02d: 결제일 한 자리(예: 5) 시 7자리 문자열 방지 — 고정길이 전문 8자리 규격 준수
        String dueDate = targetYearMonth + String.format("%02d", d);

        // 카드별 이용금액 집계 (해당 청구 기간 내 승인된 거래)
        List<Map<String, Object>> items = new ArrayList<>();
        long totalAmount = 0L;

        for (Map<String, Object> cardRow : cards) {
            String cardNo = cardRow.get("카드번호") != null ? cardRow.get("카드번호").toString() : "";
            String cardName = cardRow.get("카드구분") != null ? cardRow.get("카드구분").toString() : "";

            List<Map<String, Object>> txRows = jdbcTemplate.queryForList(
                    "SELECT NVL(SUM(\"이용금액\"), 0) AS CARD_AMOUNT "
                    + "FROM D_SPIDERLINK.\"POC_카드사용내역\" "
                    + "WHERE \"이용자\" = ? AND \"카드번호\" = ? "
                    + "AND \"이용일자\" >= ? AND \"이용일자\" <= ? AND \"승인여부\" = 'Y'",
                    userId, cardNo, billingFrom, billingTo);

            long cardAmount = 0L;
            if (!txRows.isEmpty() && txRows.get(0).get("CARD_AMOUNT") != null) {
                cardAmount = ((Number) txRows.get(0).get("CARD_AMOUNT")).longValue();
            }

            Map<String, Object> item = new HashMap<>();
            item.put("cardNo", cardNo);
            item.put("cardName", cardName);
            item.put("amount", cardAmount);
            item.put("dueDate", dueDate);
            items.add(item);
            totalAmount += cardAmount;
        }

        // 대표 카드 정보 (첫 번째 카드)
        Map<String, Object> cardInfo = new HashMap<>();
        if (!cards.isEmpty()) {
            Map<String, Object> firstCard = cards.get(0);
            cardInfo.put("paymentBank", firstCard.get("결제은행명") != null ? firstCard.get("결제은행명").toString() : "");
            cardInfo.put("paymentAccount", firstCard.get("결제계좌") != null ? firstCard.get("결제계좌").toString() : "");
            cardInfo.put("paymentDay", effectivePaymentDay);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dueDate", dueDate);
        result.put("totalAmount", totalAmount);
        result.put("items", items);
        result.put("cardInfo", cardInfo);
        DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        Map<String, Object> billingPeriodMap = new HashMap<>();
        billingPeriodMap.put("usageStart", billingFromDate.format(displayFmt));
        billingPeriodMap.put("usageEnd",   billingToDate.format(displayFmt));
        // paymentYM.atDay(d): 이미 계산된 변수 재사용 — LocalDate.parse(dueDate) 시 DateTimeParseException 방지
        billingPeriodMap.put("dueDate",    paymentYM.atDay(Math.min(d, paymentYM.lengthOfMonth())).format(displayFmt));
        result.put("billingPeriod", billingPeriodMap);
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 즉시결제 관련
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 지정 카드의 즉시결제 가능금액 및 신용한도를 조회한다.
     *
     * @param userId 사용자 ID
     * @param cardId 카드번호
     * @return payableAmount(결제잔액 합계), creditLimit(한도금액) Map
     * @throws IllegalArgumentException 카드 미존재 시
     */
    public Map<String, Object> findPayableAmount(String userId, String cardId) {
        // 카드의 한도금액 조회
        List<Map<String, Object>> cardRows = jdbcTemplate.queryForList(
                "SELECT \"한도금액\" FROM D_SPIDERLINK.\"POC_카드리스트\" "
                + "WHERE \"사용자아이디\" = ? AND \"카드번호\" = ?",
                userId, cardId);

        if (cardRows.isEmpty()) {
            throw new IllegalArgumentException("카드 정보를 찾을 수 없습니다: " + cardId);
        }

        long creditLimit = cardRows.get(0).get("한도금액") != null
                ? ((Number) cardRows.get(0).get("한도금액")).longValue() : 0L;

        // 미결제 잔액 합계 조회
        List<Map<String, Object>> balanceRows = jdbcTemplate.queryForList(
                "SELECT NVL(SUM(\"결제잔액\"), 0) AS PAYABLE_AMOUNT "
                + "FROM D_SPIDERLINK.\"POC_카드사용내역\" "
                + "WHERE \"이용자\" = ? AND \"카드번호\" = ? AND \"결제잔액\" > 0 AND \"결제상태코드\" <> '9'",
                userId, cardId);

        long payableAmount = 0L;
        if (!balanceRows.isEmpty() && balanceRows.get(0).get("PAYABLE_AMOUNT") != null) {
            payableAmount = ((Number) balanceRows.get(0).get("PAYABLE_AMOUNT")).longValue();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("payableAmount", payableAmount);
        result.put("creditLimit", creditLimit);
        return result;
    }

    /**
     * 카드 즉시결제를 처리한다 (트랜잭션 보장).
     *
     * <p>처리 순서:</p>
     * <ol>
     *   <li>미결제 카드 이용내역을 이용일자 오름차순으로 FOR UPDATE 잠금 후 조회</li>
     *   <li>결제 계좌잔액을 FOR UPDATE 잠금 후 조회</li>
     *   <li>계좌잔액 부족 시 예외 발생 (잔액이 부족합니다.)</li>
     *   <li>오래된 거래부터 순차적으로 결제잔액 차감 (ROWID로 UPDATE)</li>
     *   <li>계좌잔액 차감, 뱅킹거래내역 INSERT, 카드 사용금액 갱신</li>
     * </ol>
     *
     * @param userId        사용자 ID
     * @param cardId        카드번호
     * @param amount        결제 요청 금액
     * @param accountNumber 출금 계좌번호
     * @return paidAmount, processedCount, completedAt Map
     * @throws IllegalArgumentException 계좌 미존재, 잔액 부족 시
     */
    @Transactional
    public Map<String, Object> processImmediatePay(String userId, String cardId,
                                                    long amount, String accountNumber) {
        // 1. 미결제 카드 이용내역 잠금 조회 (이용일자 오름차순 — 가장 오래된 것부터 결제)
        List<Map<String, Object>> unpaidRows = jdbcTemplate.queryForList(
                "SELECT ROWID, \"결제잔액\", \"누적결제금액\" "
                + "FROM D_SPIDERLINK.\"POC_카드사용내역\" "
                + "WHERE \"이용자\" = ? AND \"카드번호\" = ? AND \"결제잔액\" > 0 AND \"결제상태코드\" <> '9' "
                + "ORDER BY \"이용일자\" ASC FOR UPDATE",
                userId, cardId);

        // 2. 계좌잔액 잠금 조회
        List<Map<String, Object>> accountRows = jdbcTemplate.queryForList(
                "SELECT \"계좌잔액\" FROM D_SPIDERLINK.\"POC_뱅킹계좌정보\" "
                + "WHERE \"계좌번호\" = ? AND \"사용자아이디\" = ? FOR UPDATE",
                accountNumber, userId);

        if (accountRows.isEmpty()) {
            throw new IllegalArgumentException("계좌 정보를 찾을 수 없습니다: " + accountNumber);
        }

        long accountBalance = ((Number) accountRows.get(0).get("계좌잔액")).longValue();

        // 3. 계좌잔액 부족 여부 확인
        if (accountBalance < amount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        // 4. 현재 시각 조회 (DB SYSDATE 기준 — 트랜잭션 일관성 보장)
        Map<String, Object> timeRow = jdbcTemplate.queryForMap(
                "SELECT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') AS TX_DATETIME, "
                + "TO_CHAR(SYSDATE, 'YYYYMMDD') AS TX_DATE, "
                + "TO_CHAR(SYSDATE, 'YYYY.MM.DD HH24:MI') AS COMPLETED_AT FROM DUAL");

        String txDatetime = timeRow.get("TX_DATETIME").toString();
        String txDate = timeRow.get("TX_DATE").toString();
        String completedAt = timeRow.get("COMPLETED_AT").toString();

        // 5. 오래된 거래부터 순차적으로 결제잔액 차감
        long remaining = amount;
        int processedCount = 0;
        long totalPaid = 0L;

        for (Map<String, Object> row : unpaidRows) {
            if (remaining <= 0) {
                break;
            }

            // ROWID는 oracle.sql.ROWID 타입으로 반환될 수 있으므로 toString() 사용
            String rowId = row.get("ROWID").toString();
            long balance = ((Number) row.get("결제잔액")).longValue();
            long accumulated = row.get("누적결제금액") != null
                    ? ((Number) row.get("누적결제금액")).longValue() : 0L;

            long payAmount = Math.min(remaining, balance);
            long newBalance = balance - payAmount;
            long newAccumulated = accumulated + payAmount;
            // 결제잔액이 0이 되면 결제완료(코드: '9'), 아니면 부분결제(코드: '1')
            String statusCode = newBalance == 0 ? "9" : "1";

            jdbcTemplate.update(
                    "UPDATE D_SPIDERLINK.\"POC_카드사용내역\" "
                    + "SET \"결제잔액\" = ?, \"누적결제금액\" = ?, \"결제상태코드\" = ?, \"최종결제일자\" = ? "
                    + "WHERE ROWID = ?",
                    newBalance, newAccumulated, statusCode, txDate, rowId);

            remaining -= payAmount;
            totalPaid += payAmount;
            processedCount++;
        }

        long actualPaid = amount - remaining; // 실제 결제된 금액

        // 6. 계좌잔액 차감
        jdbcTemplate.update(
                "UPDATE D_SPIDERLINK.\"POC_뱅킹계좌정보\" SET \"계좌잔액\" = \"계좌잔액\" - ? "
                + "WHERE \"계좌번호\" = ? AND \"사용자아이디\" = ?",
                actualPaid, accountNumber, userId);

        long newAccountBalance = accountBalance - actualPaid;

        // 7. 뱅킹거래내역 INSERT
        jdbcTemplate.update(
                "INSERT INTO D_SPIDERLINK.\"POC_뱅킹거래내역\" "
                + "(\"계좌번호\", \"거래일시\", \"거래점\", \"출금액\", \"입금액\", \"잔액\", "
                + "\"보낸분받는분\", \"적요\", \"송금메모\", \"입금계좌번호\", \"사용자아이디\") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                accountNumber, txDatetime, "카드즉시결제", actualPaid, 0L, newAccountBalance,
                "카드사", "카드즉시결제", "카드번호: " + cardId, cardId, userId);

        // 8. 카드 사용금액 갱신 (POC_카드리스트.사용금액 = 기존 사용금액 - 실제 결제 금액)
        jdbcTemplate.update(
                "UPDATE D_SPIDERLINK.\"POC_카드리스트\" "
                + "SET \"사용금액\" = GREATEST(\"사용금액\" - ?, 0) "
                + "WHERE \"사용자아이디\" = ? AND \"카드번호\" = ?",
                actualPaid, userId, cardId);

        Map<String, Object> result = new HashMap<>();
        result.put("paidAmount", actualPaid);
        result.put("processedCount", processedCount);
        result.put("completedAt", completedAt);
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 날짜 포맷 유틸리티
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * YYYYMMDD + HH24MISS 형태를 "YYYY.MM.DD HH:mm" 형태로 변환한다.
     *
     * @param date YYYYMMDD
     * @param time HH24MISS (6자리 미만이면 00:00 처리)
     * @return "YYYY.MM.DD HH:mm" 형식 문자열
     */
    private String formatDateTime(String date, String time) {
        if (date == null || date.length() < 8) {
            return "";
        }
        String formatted = date.substring(0, 4) + "." + date.substring(4, 6) + "." + date.substring(6, 8);
        if (time != null && time.length() >= 4) {
            formatted += " " + time.substring(0, 2) + ":" + time.substring(2, 4);
        } else {
            formatted += " 00:00";
        }
        return formatted;
    }

    /**
     * YYYYMMDD 형태의 날짜를 "M월 D일" 형태로 변환한다.
     *
     * @param yyyymmdd YYYYMMDD 형식 날짜
     * @return "M월 D일" 형식 문자열 (파싱 실패 시 원본 반환)
     */
    private String formatToMonthDay(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() < 8) {
            return yyyymmdd != null ? yyyymmdd : "";
        }
        try {
            int month = Integer.parseInt(yyyymmdd.substring(4, 6));
            int day = Integer.parseInt(yyyymmdd.substring(6, 8));
            return month + "월 " + day + "일";
        } catch (NumberFormatException e) {
            return yyyymmdd;
        }
    }

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * YYYYMM 기준으로 전달의 특정 일자(DD)를 YYYYMMDD 형태로 반환한다.
     *
     * <p>java.time을 사용하므로 2월처럼 해당 월의 마지막 날보다 큰 day 값이 입력되어도
     * 자동으로 해당 월의 마지막 날짜로 보정된다 (예: 2월 26일 → 2월 28/29일).</p>
     *
     * @param yearMonth YYYYMM
     * @param day       일자 문자열 (DD)
     * @return 전달의 해당 일자 (YYYYMMDD)
     */
    private String getPrevMonthDay(String yearMonth, String day) {
        YearMonth ym = YearMonth.parse(yearMonth, YEAR_MONTH_FMT);
        YearMonth prevYm = ym.minusMonths(1);
        // 전달에 존재하지 않는 일자(예: 2월 30일)는 해당 월 마지막 날로 보정
        int actualDay = Math.min(Integer.parseInt(day), prevYm.lengthOfMonth());
        return prevYm.atDay(actualDay).format(DATE_FMT);
    }
}

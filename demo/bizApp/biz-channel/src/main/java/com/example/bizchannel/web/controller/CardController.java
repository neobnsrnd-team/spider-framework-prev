package com.example.bizchannel.web.controller;

import com.example.bizchannel.client.BizClient;
import com.example.bizcommon.BizCommands;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 카드·거래내역·결제 REST 컨트롤러.
 *
 * <p>{@code /api/cards}, {@code /api/transactions}, {@code /api/payment-statement} 경로의
 * HTTP 요청을 처리한다. 모든 엔드포인트는 JWT 인증이 필요하며,
 * {@link com.example.bizchannel.web.filter.JwtAuthFilter} 가 검증 후 request 속성에
 * {@code userId} 를 설정한다.</p>
 *
 * <p>실제 비즈니스 로직은 이체AP(biz-transfer, TCP 19200) 가 담당하며,
 * 이 컨트롤러는 HTTP ↔ TCP 변환 역할만 수행한다.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CardController {

    private final BizClient bizClient;

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/cards
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 사용자 카드 목록 조회.
     *
     * @param request HTTP 요청 — JWT 필터가 설정한 {@code userId} 속성 참조
     * @return 카드 목록 ({@code cards: [...]})
     */
    @GetMapping("/api/cards")
    public ResponseEntity<Map<String, Object>> getCards(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        String requestId = (String) request.getAttribute("requestId");
        log.debug("[CardController] Get card list: userId={}", userId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);

        try {
            JsonCommandResponse resp = bizClient.sendToTransfer(BizCommands.TRANSFER_CARD_LIST, payload, requestId);
            if (!resp.isSuccess()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", resp.getMessage() != null ? resp.getMessage() : "카드 목록 조회 실패"));
            }
            return ResponseEntity.ok(resp.getPayload());
        } catch (IOException e) {
            log.error("[CardController] biz-transfer TCP error (cards): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이체 서버 통신 오류"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/transactions
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 카드 이용내역 조회.
     *
     * @param request     HTTP 요청 — {@code userId} 속성 참조
     * @param cardId      조회할 카드 ID (선택)
     * @param period      조회 기간 구분 (선택, 예: "1m", "3m", "custom")
     * @param customMonth 사용자 지정 월 (선택, YYYYMM)
     * @param usageType   이용 유형 필터 (선택)
     * @param fromDate    조회 시작일 (선택, YYYYMMDD)
     * @param toDate      조회 종료일 (선택, YYYYMMDD)
     * @return 이용내역 ({@code transactions:[...], totalCount, paymentSummary})
     */
    @GetMapping("/api/transactions")
    public ResponseEntity<Map<String, Object>> getTransactions(
            HttpServletRequest request,
            @RequestParam(required = false) String cardId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String customMonth,
            @RequestParam(required = false) String usageType,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        String userId = (String) request.getAttribute("userId");
        String requestId = (String) request.getAttribute("requestId");
        log.debug("[CardController] Get transactions: userId={}, cardId={}, period={}", userId, cardId, period);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        // null 값은 제외하고 실제 파라미터만 페이로드에 포함
        if (cardId != null)      payload.put("cardId", cardId);
        if (period != null)      payload.put("period", period);
        if (customMonth != null) payload.put("customMonth", customMonth);
        if (usageType != null)   payload.put("usageType", usageType);
        if (fromDate != null)    payload.put("fromDate", fromDate);
        if (toDate != null)      payload.put("toDate", toDate);

        try {
            JsonCommandResponse resp = bizClient.sendToTransfer(BizCommands.TRANSFER_TRANSACTIONS, payload, requestId);
            if (!resp.isSuccess()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", resp.getMessage() != null ? resp.getMessage() : "이용내역 조회 실패"));
            }
            return ResponseEntity.ok(resp.getPayload());
        } catch (IOException e) {
            log.error("[CardController] biz-transfer TCP error (transactions): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이체 서버 통신 오류"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/payment-statement
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 결제예정금액·이용대금명세서 조회.
     *
     * @param request    HTTP 요청 — {@code userId} 속성 참조
     * @param yearMonth  조회 대상 연월 (선택, YYYYMM)
     * @param paymentDay 결제일 (선택)
     * @return 명세서 데이터 ({@code dueDate, totalAmount, items:[...], cardInfo, billingPeriod})
     */
    @GetMapping("/api/payment-statement")
    public ResponseEntity<Map<String, Object>> getPaymentStatement(
            HttpServletRequest request,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) String paymentDay) {

        String userId = (String) request.getAttribute("userId");
        String requestId = (String) request.getAttribute("requestId");
        log.debug("[CardController] Get payment statement: userId={}, yearMonth={}, paymentDay={}", userId, yearMonth, paymentDay);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        // 프론트엔드가 "YYYY-MM" 형식으로 전달하는 경우 하이픈 제거
        // mock-core 고정길이 프로토콜의 yearMonth 필드가 C(6, YYYYMM)이라 7자 이상이면 잘림
        if (yearMonth != null)  payload.put("yearMonth", yearMonth.replace("-", ""));
        if (paymentDay != null) payload.put("paymentDay", paymentDay);

        try {
            JsonCommandResponse resp = bizClient.sendToTransfer(BizCommands.TRANSFER_PAYMENT_STMT, payload, requestId);
            if (!resp.isSuccess()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", resp.getMessage() != null ? resp.getMessage() : "명세서 조회 실패"));
            }
            return ResponseEntity.ok(resp.getPayload());
        } catch (IOException e) {
            log.error("[CardController] biz-transfer TCP error (payment-statement): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이체 서버 통신 오류"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /api/cards/{cardId}/payable-amount
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 특정 카드의 즉시결제 가능금액 조회.
     *
     * @param request HTTP 요청 — {@code userId} 속성 참조
     * @param cardId  카드 ID (경로 변수)
     * @return 가능금액 정보 ({@code payableAmount, creditLimit})
     */
    @GetMapping("/api/cards/{cardId}/payable-amount")
    public ResponseEntity<Map<String, Object>> getPayableAmount(
            HttpServletRequest request,
            @PathVariable String cardId) {

        String userId = (String) request.getAttribute("userId");
        String requestId = (String) request.getAttribute("requestId");
        log.debug("[CardController] Get payable amount: userId={}, cardId={}", userId, cardId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("cardId", cardId);

        try {
            JsonCommandResponse resp = bizClient.sendToTransfer(BizCommands.TRANSFER_PAYABLE_AMT, payload, requestId);
            if (!resp.isSuccess()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", resp.getMessage() != null ? resp.getMessage() : "가능금액 조회 실패"));
            }
            return ResponseEntity.ok(resp.getPayload());
        } catch (IOException e) {
            log.error("[CardController] biz-transfer TCP error (payable-amount): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이체 서버 통신 오류"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /api/cards/{cardId}/immediate-pay
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 즉시결제 처리.
     *
     * @param request HTTP 요청 — {@code userId} 속성 참조
     * @param cardId  카드 ID (경로 변수)
     * @param body    요청 바디 ({@code pin, amount, accountNumber})
     * @return 결제 결과 ({@code paidAmount, processedCount, completedAt})
     */
    @PostMapping("/api/cards/{cardId}/immediate-pay")
    public ResponseEntity<Map<String, Object>> immediatePay(
            HttpServletRequest request,
            @PathVariable String cardId,
            @RequestBody Map<String, Object> body) {

        String userId = (String) request.getAttribute("userId");
        String requestId = (String) request.getAttribute("requestId");
        log.info("[CardController] Immediate pay request: userId={}, cardId={}", userId, cardId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("cardId", cardId);
        payload.put("pin", body.get("pin"));
        payload.put("amount", body.get("amount"));
        payload.put("accountNumber", body.get("accountNumber"));

        try {
            JsonCommandResponse resp = bizClient.sendToTransfer(BizCommands.TRANSFER_IMMEDIATE_PAY, payload, requestId);
            if (!resp.isSuccess()) {
                String errorMsg = resp.getError() != null ? resp.getError()
                        : resp.getMessage() != null ? resp.getMessage()
                        : "즉시결제 실패";
                // payload가 있으면 PIN 오류 계열(attemptsLeft 포함) — 403으로 전달
                if (resp.getPayload() != null && !resp.getPayload().isEmpty()) {
                    Map<String, Object> errorBody = new HashMap<>(resp.getPayload());
                    errorBody.put("error", errorMsg);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody);
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", errorMsg));
            }
            return ResponseEntity.ok(resp.getPayload());
        } catch (IOException e) {
            log.error("[CardController] biz-transfer TCP error (immediate-pay): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이체 서버 통신 오류"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE /api/cards/{cardId}/pin-attempts
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * PIN 시도 횟수 초기화.
     *
     * <p>PIN 잠금 해제는 이체AP(biz-transfer) 에서 관리하므로
     * 채널AP 에서는 성공 응답만 반환한다.
     * 실제 운영 환경에서는 이체AP 에 위임하는 로직을 추가해야 한다.</p>
     *
     * @param request HTTP 요청 — {@code userId} 속성 참조
     * @param cardId  카드 ID (경로 변수)
     * @return {@code {ok: true}}
     */
    @DeleteMapping("/api/cards/{cardId}/pin-attempts")
    public ResponseEntity<Map<String, Object>> resetPinAttempts(
            HttpServletRequest request,
            @PathVariable String cardId) {

        String userId = (String) request.getAttribute("userId");
        String requestId = (String) request.getAttribute("requestId");
        log.info("[CardController] Reset PIN attempts: userId={}, cardId={}", userId, cardId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("cardId", cardId);

        try {
            JsonCommandResponse resp = bizClient.sendToTransfer(BizCommands.TRANSFER_RESET_PIN_ATTEMPTS, payload, requestId);
            if (!resp.isSuccess()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", resp.getError() != null ? resp.getError() : "PIN 초기화 실패"));
            }
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IOException e) {
            log.error("[CardController] biz-transfer TCP error (reset-pin-attempts): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이체 서버 통신 오류"));
        }
    }
}

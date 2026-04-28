package com.example.mockcore.handler;

import com.example.bizcommon.BizCommands;
import com.example.mockcore.repository.AccountRepository;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 즉시결제 처리 커맨드 핸들러 ({@code CORE_IMMEDIATE_PAY}).
 *
 * <p>userId, cardId, amount, accountNumber를 수신하여 즉시결제를 처리하고
 * paidAmount, processedCount, completedAt을 반환한다.</p>
 *
 * <p>오류 유형별 처리:</p>
 * <ul>
 *   <li>{@code INSUFFICIENT_BALANCE}: 계좌 잔액 부족 ("잔액이 부족합니다.")</li>
 *   <li>{@code ACCOUNT_NOT_FOUND}: 계좌 정보 미존재 ("계좌 정보를 찾을 수 없습니다.")</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreImmediatePayHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final AccountRepository accountRepository;

    @Override
    public boolean supports(String command) {
        return BizCommands.CORE_IMMEDIATE_PAY.equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        Map<String, Object> payload = request.getPayload();

        try {
            String userId = getRequiredString(payload, "userId");
            String cardId = getRequiredString(payload, "cardId");
            String accountNumber = getRequiredString(payload, "accountNumber");

            // amount: Number 타입으로 수신될 수 있으므로 안전하게 변환
            Object amountObj = payload.get("amount");
            if (amountObj == null) {
                throw new IllegalArgumentException("필수 파라미터 누락: amount");
            }
            long amount;
            try {
                amount = ((Number) amountObj).longValue();
            } catch (ClassCastException e) {
                // JSON 역직렬화 결과가 String으로 올 경우를 방어 처리
                amount = Long.parseLong(amountObj.toString());
            }

            if (amount <= 0) {
                throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
            }

            log.debug("[CORE_IMMEDIATE_PAY] 즉시결제 요청 — userId={}, cardId={}, amount={}, accountNumber={}",
                    userId, cardId, amount, accountNumber);

            Map<String, Object> result = accountRepository.processImmediatePay(userId, cardId, amount, accountNumber);

            log.info("[CORE_IMMEDIATE_PAY] 즉시결제 완료 — userId={}, cardId={}, paidAmount={}, processedCount={}",
                    userId, cardId, result.get("paidAmount"), result.get("processedCount"));

            return JsonCommandResponse.builder()
                    .command(command)
                    .success(true)
                    .message("즉시결제 처리 완료")
                    .payload(result)
                    .build();

        } catch (IllegalArgumentException e) {
            // 잔액 부족, 계좌 미존재 등 비즈니스 예외는 error 코드로 구분하여 반환
            String errorMsg = e.getMessage();
            String errorCode;
            if (errorMsg != null && errorMsg.contains("잔액이 부족합니다")) {
                errorCode = "INSUFFICIENT_BALANCE";
            } else if (errorMsg != null && errorMsg.contains("계좌 정보를 찾을 수 없습니다")) {
                errorCode = "ACCOUNT_NOT_FOUND";
            } else {
                errorCode = "INVALID_PARAMETER";
            }
            log.warn("[CORE_IMMEDIATE_PAY] 결제 실패 — errorCode={}, message={}", errorCode, errorMsg);
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(false)
                    .error(errorCode + ": " + errorMsg)
                    .build();

        } catch (Exception e) {
            log.error("[CORE_IMMEDIATE_PAY] 결제 처리 중 오류 — {}", e.getMessage(), e);
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * payload에서 필수 문자열 값을 추출한다. null이거나 빈 값이면 예외를 던진다.
     *
     * @param payload 요청 payload
     * @param key     추출할 키
     * @return 문자열 값
     * @throws IllegalArgumentException 값이 없거나 빈 문자열인 경우
     */
    private String getRequiredString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("필수 파라미터 누락: " + key);
        }
        return value.toString();
    }
}

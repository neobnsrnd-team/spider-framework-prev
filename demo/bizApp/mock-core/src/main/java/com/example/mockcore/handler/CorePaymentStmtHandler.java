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
 * 이용대금명세서 조회 커맨드 핸들러 ({@code CORE_PAYMENT_STMT}).
 *
 * <p>userId(필수), yearMonth, paymentDay(선택)를 수신하여
 * dueDate, totalAmount, items, cardInfo, billingPeriod를 반환한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorePaymentStmtHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final AccountRepository accountRepository;

    @Override
    public boolean supports(String command) {
        return BizCommands.CORE_PAYMENT_STMT.equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        Map<String, Object> payload = request.getPayload();

        try {
            Object userIdObj = payload.get("userId");
            if (userIdObj == null || userIdObj.toString().isBlank()) {
                throw new IllegalArgumentException("필수 파라미터 누락: userId");
            }
            String userId = userIdObj.toString();

            // 선택 파라미터 — null이면 AccountRepository에서 현재 월 기준으로 조회
            String yearMonth = getOptionalString(payload, "yearMonth");
            String paymentDay = getOptionalString(payload, "paymentDay");

            log.debug("[CORE_PAYMENT_STMT] 이용대금명세서 조회 요청 — userId={}, yearMonth={}, paymentDay={}",
                    userId, yearMonth, paymentDay);

            Map<String, Object> result = accountRepository.findPaymentStatement(userId, yearMonth, paymentDay);

            log.debug("[CORE_PAYMENT_STMT] 조회 성공 — userId={}, totalAmount={}", userId, result.get("totalAmount"));

            return JsonCommandResponse.builder()
                    .command(command)
                    .success(true)
                    .message("이용대금명세서 조회 성공")
                    .payload(result)
                    .build();

        } catch (Exception e) {
            log.warn("[CORE_PAYMENT_STMT] 조회 실패 — {}", e.getMessage());
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * payload에서 선택 문자열 값을 추출한다. 없거나 빈 값이면 null을 반환한다.
     *
     * @param payload 요청 payload
     * @param key     추출할 키
     * @return 문자열 값 또는 null
     */
    private String getOptionalString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString();
    }
}

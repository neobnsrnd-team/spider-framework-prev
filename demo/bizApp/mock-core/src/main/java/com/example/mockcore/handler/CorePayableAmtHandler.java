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
 * 즉시결제 가능금액 조회 커맨드 핸들러 ({@code CORE_PAYABLE_AMT}).
 *
 * <p>userId, cardId를 수신하여 payableAmount(미결제 잔액 합계), creditLimit(카드 한도금액)을 반환한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorePayableAmtHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final AccountRepository accountRepository;

    @Override
    public boolean supports(String command) {
        return BizCommands.CORE_PAYABLE_AMT.equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        Map<String, Object> payload = request.getPayload();

        try {
            String userId = getRequiredString(payload, "userId");
            String cardId = getRequiredString(payload, "cardId");

            log.debug("[CORE_PAYABLE_AMT] 결제가능금액 조회 요청 — userId={}, cardId={}", userId, cardId);

            Map<String, Object> result = accountRepository.findPayableAmount(userId, cardId);

            log.debug("[CORE_PAYABLE_AMT] 조회 성공 — userId={}, cardId={}, payableAmount={}",
                    userId, cardId, result.get("payableAmount"));

            return JsonCommandResponse.builder()
                    .command(command)
                    .success(true)
                    .message("결제가능금액 조회 성공")
                    .payload(result)
                    .build();

        } catch (Exception e) {
            log.warn("[CORE_PAYABLE_AMT] 조회 실패 — {}", e.getMessage());
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

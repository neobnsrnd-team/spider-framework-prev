package com.example.biztransfer.handler;

import com.example.bizcommon.BizCommands;
import com.example.biztransfer.service.TransferService;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TRANSFER_IMMEDIATE_PAY 커맨드 핸들러.
 *
 * <p>즉시결제 요청을 처리한다. PIN 검증을 biz-transfer 내부에서 수행하며,
 * 검증 통과 시 pin 필드를 제거한 페이로드로 mock-core의 CORE_IMMEDIATE_PAY를 호출한다.</p>
 *
 * <h3>PIN 검증 규칙</h3>
 * <ul>
 *   <li>유효 PIN: 오늘 날짜의 MMDD (예: 4월 22일 → "0422")</li>
 *   <li>최대 시도 횟수: 3회 — 초과 시 잠금 처리</li>
 *   <li>PIN 성공 시 시도 횟수 초기화</li>
 *   <li>시도 횟수는 인메모리 {@link ConcurrentHashMap}으로 관리 ("userId:cardId" 키)</li>
 * </ul>
 *
 * <ul>
 *   <li>인바운드 페이로드: {@code {userId, cardId, pin, amount, accountNumber}}</li>
 *   <li>응답 페이로드:    {@code {paidAmount, processedCount, completedAt}}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferImmediatePayHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final TransferService transferService;

    /** PIN 실패 횟수 추적 — 키: "userId:cardId", 값: 연속 실패 횟수 */
    private final Map<String, Integer> pinAttemptStore = new ConcurrentHashMap<>();

    /** PIN 최대 허용 시도 횟수 */
    private static final int PIN_MAX_ATTEMPTS = 3;

    @Override
    public boolean supports(String command) {
        return BizCommands.TRANSFER_IMMEDIATE_PAY.equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        Map<String, Object> payload = request.getPayload();
        String userId = (String) payload.get("userId");
        String cardId = (String) payload.get("cardId");
        String pin    = (String) payload.get("pin");

        // "userId:cardId" 복합 키로 카드별 PIN 시도 횟수를 독립적으로 관리한다
        String pinKey = userId + ":" + cardId;

        // 이미 잠금 상태인지 확인
        int attempts = pinAttemptStore.getOrDefault(pinKey, 0);
        if (attempts >= PIN_MAX_ATTEMPTS) {
            log.warn("[TransferImmediatePayHandler] PIN 잠금 — userId={}, cardId={}", userId, cardId);
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(false)
                    .error("PIN 입력 횟수를 초과하였습니다.")
                    .build();
        }

        // 유효 PIN: 오늘 날짜의 MMDD (월·일 모두 두 자리 zero-padding)
        // 서버 기본 시간대가 달라도 한국 시간 기준으로 PIN을 검증하기 위해 시간대 명시
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String validPin = String.format("%02d%02d", today.getMonthValue(), today.getDayOfMonth());

        if (!validPin.equals(pin)) {
            int updatedAttempts = attempts + 1;
            pinAttemptStore.put(pinKey, updatedAttempts);
            int remaining = PIN_MAX_ATTEMPTS - updatedAttempts;

            log.warn("[TransferImmediatePayHandler] PIN 불일치 — userId={}, cardId={}, 시도={}/{}",
                    userId, cardId, updatedAttempts, PIN_MAX_ATTEMPTS);

            return JsonCommandResponse.builder()
                    .command(command)
                    .success(false)
                    .error("PIN 번호가 올바르지 않습니다. 남은 시도 횟수: " + remaining)
                    .build();
        }

        // PIN 검증 성공 — 시도 횟수 초기화
        pinAttemptStore.remove(pinKey);
        log.debug("[TransferImmediatePayHandler] PIN 검증 성공 — userId={}, cardId={}", userId, cardId);

        // mock-core에는 pin 필드를 포함하지 않고 전달 (보안)
        Map<String, Object> corePayload = new HashMap<>(payload);
        corePayload.remove("pin");

        return transferService.forward(
                BizCommands.TRANSFER_IMMEDIATE_PAY,
                BizCommands.CORE_IMMEDIATE_PAY,
                request,
                corePayload);
    }
}

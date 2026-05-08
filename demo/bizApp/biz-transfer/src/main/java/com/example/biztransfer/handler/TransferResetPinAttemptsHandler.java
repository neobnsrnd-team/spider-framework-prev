package com.example.biztransfer.handler;

import com.example.bizcommon.BizCommands;
import com.example.biztransfer.store.PinAttemptStore;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * TRANSFER_RESET_PIN_ATTEMPTS 커맨드 핸들러.
 *
 * <p>관리자 또는 사용자 본인 확인 후 PIN 실패 횟수를 초기화한다.
 * {@link PinAttemptStore}에서 해당 "userId:cardId" 키를 삭제해
 * 잠금 상태를 해제한다. mock-core 호출 없이 biz-transfer 내부에서 처리된다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferResetPinAttemptsHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final PinAttemptStore pinAttemptStore;

    @Override
    public boolean supports(String command) {
        return BizCommands.TRANSFER_RESET_PIN_ATTEMPTS.equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        Map<String, Object> payload = request.getPayload();
        String userId = (String) payload.get("userId");
        String cardId = (String) payload.get("cardId");

        String pinKey = userId + ":" + cardId;
        pinAttemptStore.reset(pinKey);
        log.info("[TransferResetPinAttemptsHandler] PIN 시도 횟수 초기화 — userId={}, cardId={}", userId, cardId);

        return JsonCommandResponse.builder()
                .command(command)
                .success(true)
                .payload(Map.of("ok", true))
                .build();
    }
}

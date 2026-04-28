package com.example.mockcore.handler;

import com.example.bizcommon.BizCommands;
import com.example.mockcore.infra.FixedMessageReader;
import com.example.mockcore.infra.FixedMessageWriter;
import com.example.mockcore.infra.LegacyCoreHandler;
import com.example.mockcore.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 즉시결제 가능금액 조회 커맨드 핸들러 ({@code CORE_PAYABLE_AMT}).
 *
 * <p>REQ: COMMAND(C,20) + REQUEST_ID(C,36) + userId(C,20) + cardId(C,20)
 * RES: SUCCESS(C,1) + ERROR_MSG(K,200) + payableAmount(N,15) + creditLimit(N,15)</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorePayableAmtHandler implements LegacyCoreHandler {

    private final AccountRepository accountRepository;

    @Override
    public String getCommand() {
        return BizCommands.CORE_PAYABLE_AMT;
    }

    @Override
    public byte[] handle(byte[] requestBytes) {
        FixedMessageReader reader = new FixedMessageReader(requestBytes);
        reader.skip(20); // COMMAND
        reader.skip(36); // REQUEST_ID
        String userId = reader.readC(20);
        String cardId = reader.readC(20);

        log.debug("[CORE_PAYABLE_AMT] 가능금액 조회 — userId={}, cardId={}", userId, cardId);

        FixedMessageWriter writer = new FixedMessageWriter();
        try {
            Map<String, Object> result = accountRepository.findPayableAmount(userId, cardId);
            long payableAmount = toLong(result.get("payableAmount"));
            long creditLimit   = toLong(result.get("creditLimit"));
            writer.writeC("Y", 1);
            writer.writeK("", 200);
            writer.writeN(payableAmount, 15);
            writer.writeN(creditLimit, 15);
        } catch (Exception e) {
            log.warn("[CORE_PAYABLE_AMT] 조회 실패 — {}", e.getMessage());
            writer = new FixedMessageWriter();
            writer.writeC("N", 1);
            writer.writeK(e.getMessage(), 200);
            writer.writeN(0L, 15);
            writer.writeN(0L, 15);
        }
        return writer.toBytes();
    }

    private long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val != null) {
            try { return Long.parseLong(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }
}

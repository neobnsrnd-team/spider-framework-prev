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
 * 즉시결제 처리 커맨드 핸들러 ({@code CORE_IMMEDIATE_PAY}).
 *
 * <p>REQ: COMMAND(C,20) + REQUEST_ID(C,36) + userId(C,20) + cardId(C,20)
 *         + amount(N,15) + accountNumber(C,20)
 * RES: SUCCESS(C,1) + ERROR_MSG(K,200) + paidAmount(N,15)
 *      + processedCount(N,4) + completedAt(C,20)</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreImmediatePayHandler implements LegacyCoreHandler {

    private final AccountRepository accountRepository;

    @Override
    public String getCommand() {
        return BizCommands.CORE_IMMEDIATE_PAY;
    }

    @Override
    public byte[] handle(byte[] requestBytes) {
        FixedMessageReader reader = new FixedMessageReader(requestBytes);
        reader.skip(20); // COMMAND
        reader.skip(36); // REQUEST_ID
        String userId        = reader.readC(20);
        String cardId        = reader.readC(20);
        String amountStr     = reader.readN(15);
        String accountNumber = reader.readC(20);

        log.debug("[CORE_IMMEDIATE_PAY] 즉시결제 요청 — userId={}, cardId={}, amount={}", userId, cardId, amountStr);

        FixedMessageWriter writer = new FixedMessageWriter();
        try {
            long amount = amountStr.isBlank() ? 0L : Long.parseLong(amountStr.trim());
            if (amount <= 0) {
                throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
            }

            Map<String, Object> result = accountRepository.processImmediatePay(userId, cardId, amount, accountNumber);
            long paidAmount      = toLong(result.get("paidAmount"));
            int  processedCount  = (int) toLong(result.get("processedCount"));
            String completedAt   = str(result, "completedAt");

            log.info("[CORE_IMMEDIATE_PAY] 즉시결제 완료 — paidAmount={}, processedCount={}", paidAmount, processedCount);
            writer.writeC("Y", 1);
            writer.writeK("", 200);
            writer.writeN(paidAmount, 15);
            writer.writeN(processedCount, 4);
            writer.writeC(completedAt, 20);

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "처리 실패";
            String code = msg.contains("잔액이 부족합니다") ? "INSUFFICIENT_BALANCE"
                    : msg.contains("계좌 정보를 찾을 수 없습니다") ? "ACCOUNT_NOT_FOUND"
                    : "INVALID_PARAMETER";
            log.warn("[CORE_IMMEDIATE_PAY] 결제 실패 — {}: {}", code, msg);
            writer = new FixedMessageWriter();
            writer.writeC("N", 1);
            writer.writeK(code + ": " + msg, 200);
            writer.writeN(0L, 15);
            writer.writeN(0, 4);
            writer.writeC("", 20);
        } catch (Exception e) {
            log.error("[CORE_IMMEDIATE_PAY] 처리 오류 — {}", e.getMessage(), e);
            writer = new FixedMessageWriter();
            writer.writeC("N", 1);
            writer.writeK(e.getMessage(), 200);
            writer.writeN(0L, 15);
            writer.writeN(0, 4);
            writer.writeC("", 20);
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

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}

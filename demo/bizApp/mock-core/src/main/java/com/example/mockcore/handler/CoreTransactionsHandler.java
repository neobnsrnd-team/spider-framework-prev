package com.example.mockcore.handler;

import com.example.bizcommon.BizCommands;
import com.example.mockcore.infra.FixedMessageReader;
import com.example.mockcore.infra.FixedMessageWriter;
import com.example.mockcore.infra.LegacyCoreHandler;
import com.example.mockcore.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 카드 이용내역 조회 커맨드 핸들러 ({@code CORE_TRANSACTIONS}).
 *
 * <p>REQ: COMMAND(C,20) + REQUEST_ID(C,36) + userId(C,20) + cardId(C,20)
 *         + fromDate(C,8) + toDate(C,8) + usageType(K,10)
 * RES: SUCCESS(C,1) + ERROR_MSG(K,200) + totalCount(N,4)
 *      + summaryTotalAmount(N,15) + summaryDate(C,15)
 *      + [반복: id(C,40)+merchant(K,100)+amount(N,15)+date(C,20)+type(K,20)
 *              +approvalNumber(C,20)+status(K,10)+cardName(K,60)]</p>
 *
 * <p>amount 필드: 취소 거래는 DB에서 음수로 저장되지만 N 타입은 부호 없으므로 절댓값 기록.
 * 취소 여부는 status(K,10) 의 '취소' 값으로 구분한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreTransactionsHandler implements LegacyCoreHandler {

    private final AccountRepository accountRepository;

    @Override
    public String getCommand() {
        return BizCommands.CORE_TRANSACTIONS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] handle(byte[] requestBytes) {
        FixedMessageReader reader = new FixedMessageReader(requestBytes);
        reader.skip(20); // COMMAND
        reader.skip(36); // REQUEST_ID
        String userId    = reader.readC(20);
        String cardId    = emptyToNull(reader.readC(20));
        String fromDate  = emptyToNull(reader.readC(8));
        String toDate    = emptyToNull(reader.readC(8));
        String usageType = emptyToNull(reader.readK(10));

        log.debug("[CORE_TRANSACTIONS] 이용내역 조회 — userId={}, cardId={}, fromDate={}, toDate={}, usageType={}",
                userId, cardId, fromDate, toDate, usageType);

        FixedMessageWriter writer = new FixedMessageWriter();
        try {
            Map<String, Object> result = accountRepository.findTransactions(userId, cardId, fromDate, toDate, usageType);

            List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("transactions");
            int totalCount = transactions != null ? transactions.size() : 0;

            Map<String, Object> summary = (Map<String, Object>) result.getOrDefault("paymentSummary", Map.of());
            long summaryTotalAmount = toLong(summary.get("totalAmount"));
            String summaryDate      = str(summary, "date");

            log.debug("[CORE_TRANSACTIONS] 조회 성공 — userId={}, totalCount={}", userId, totalCount);
            writer.writeC("Y", 1);
            writer.writeK("", 200);
            writer.writeN(totalCount, 4);
            writer.writeN(summaryTotalAmount, 15);
            writer.writeC(summaryDate, 15);
            if (transactions != null) {
                for (Map<String, Object> tx : transactions) {
                    writer.writeC(str(tx, "id"), 40);
                    writer.writeK(str(tx, "merchant"), 100);
                    // N 타입은 부호 없음 — 절댓값 기록 (취소 여부는 status 필드로 구분)
                    writer.writeN(Math.abs(toLong(tx.get("amount"))), 15);
                    writer.writeC(str(tx, "date"), 20);
                    writer.writeK(str(tx, "type"), 20);
                    writer.writeC(str(tx, "approvalNumber"), 20);
                    writer.writeK(str(tx, "status"), 10);
                    writer.writeK(str(tx, "cardName"), 60);
                }
            }
        } catch (Exception e) {
            log.warn("[CORE_TRANSACTIONS] 조회 실패 — {}", e.getMessage());
            writer = new FixedMessageWriter();
            writer.writeC("N", 1);
            writer.writeK(e.getMessage(), 200);
            writer.writeN(0, 4);
            writer.writeN(0L, 15);
            writer.writeC("", 15);
        }
        return writer.toBytes();
    }

    private String emptyToNull(String val) {
        return (val == null || val.isBlank()) ? null : val;
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
